package com.github.kubapieces.intellijdraganddroplist.services


import com.github.kubapieces.intellijdraganddroplist.sdkutil.safeValue
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Range
import org.piecesapp.client.models.*

object SaveProcessor {
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

    /// this is going to get the range of lines
    /// if cursor, then from start to end of selected text
    /// if just file then 0 to the end of the file ~ Mark
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