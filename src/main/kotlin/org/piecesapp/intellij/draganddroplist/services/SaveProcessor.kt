package org.piecesapp.intellij.draganddroplist.services


import org.piecesapp.intellij.draganddroplist.sdkutil.safeValue
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Range
import org.piecesapp.client.models.*

object SaveProcessor {
    /**
     * This function saves a new asset from data processed from an in-IDE event.
     * It first connects to the server using ApiAccumulator service, then creates a new asset using the provided seed.
     * The new asset is created with a SeededFormat, which includes a SeededFragment and a SeededClassification.
     * The SeededFragment is created with a TransferableString from the seed's text and a FragmentMetadata from the seed's extension.
     * The SeededClassification is determined based on the seed's extension. If the extension is 'text' or 'txt', the classification is set to 'TEXT', otherwise it is set to 'CODE'.
     * The new asset also includes a SeededAssetMetadata, which is set with the seed's name and a manual mechanism.
     * Finally, the new asset is stored in the SnippetStore service.
     *
     * @param seed The EventDerivedAssetSeed used to create the new asset.
     * @see EventDerivedAssetSeed
     */
    fun save(seed: EventDerivedAssetSeed) =
        service<ApiAccumulator>().doAsync {
            connect()
            val newAsset = assets.assetsCreateNewAsset(true, Seed(Seed.Type.aSSET, null,
                SeededAsset(
                    context!!.application,
                    SeededFormat(
                        fragment = SeededFragment(
                            string = TransferableString(
                                raw = seed.text
                            ),
                            metadata = FragmentMetadata(ext = seed.extension)
                        ),
                        classification = seed.extension?.let { SeededClassification(null,
                            if (it in setOf(ClassificationSpecificEnum.text, ClassificationSpecificEnum.txt)) ClassificationGenericEnum.tEXT else ClassificationGenericEnum.cODE,
                            it
                        ) }
                    ),
                    metadata = SeededAssetMetadata(
                        name = seed.name,
                        mechanism = MechanismEnum.mANUAL,
                    )
                    )))
            service<SnippetStore>().put(newAsset)
        }

    private fun wrap(start: Int, end: Int): String =
        if (start == end)
            "line $start"
        else
            "lines $start-$end"

    /**
     * This function computes the line range from provided editor reference.
     * If no selection is made, the range will be the entire file's line count
     *
     * @param editor The Editor instance for which the line range is to be calculated.
     *
     * @return A string representing the line range of the editor. If the editor is null, an empty string is returned.
     */
    private fun lineRange(editor: Editor?): String {
        if (editor == null) return ""
        val rawRange = rawLineRange(editor)
        return wrap(rawRange.from.plus(1), rawRange.to.plus(1))
    }

    private fun rawLineRange(editor: Editor?, fallback: Range<Int> = Range(0,0)): Range<Int> {
        if (editor == null) return fallback
        val selection = editor.selectionModel
        if (!selection.hasSelection())
            return Range(0, editor.document.text.trimEnd().lines().size - 1)
        return Range(selection.selectionStartPosition!!.line, selection.selectionEndPosition!!.line)
    }


    /**
     * This function builds an EventDerivedAssetSeed object from the provided parameters.
     *
     * @param project The Project object, can be null. If null, the project name will be set as "Unknown".
     * @param editor The Editor object, can be null. Used to determine the selection's line range.
     * @param file The VirtualFile object. Its name and presentable URL are used in the EventDerivedAssetSeed object.
     * @param text The text to be included in the EventDerivedAssetSeed object.
     * @param ext The ClassificationSpecificEnum object, can be null. If null, the extension will be determined from the file's extension.
     *
     * @return An EventDerivedAssetSeed object built from the provided parameters.
     */
    fun buildSeed(project: Project?, editor: Editor?, file: VirtualFile, text: String, ext: ClassificationSpecificEnum? = null): EventDerivedAssetSeed {
        val name = file.name
        val projectName: String = project?.name ?: "Unknown"
        val lineNumberRange = lineRange(editor)
        val description = "Snippet from lines $lineNumberRange, from file $name, in $projectName"
        val extension = ext ?: ClassificationSpecificEnum::class.safeValue(file.extension)

        val rawRange = rawLineRange(editor, Range(0, text.trimEnd().lines().size - 1))

        return EventDerivedAssetSeed(project!!, text, name, description, extension, filePath = file.presentableUrl, lineRange = rawRange)
    }

    data class EventDerivedAssetSeed(
        var project: Project?,
        val text: String,
        var name: String?,
        var description: String,
        var extension: ClassificationSpecificEnum?,
        var tags: List<SeededAssetTag>? = null,
        var sites: List<SeededAssetWebsite>? = null,
        var people: List<SeededPerson>? = null,
        val filePath: String? = null,
        val lineRange: Range<Int>?
    )
}