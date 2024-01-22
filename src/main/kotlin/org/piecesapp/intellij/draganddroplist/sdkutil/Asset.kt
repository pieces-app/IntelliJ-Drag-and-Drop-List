package org.piecesapp.intellij.draganddroplist.sdkutil

import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.ClassificationGenericEnum
import org.piecesapp.client.models.ClassificationSpecificEnum
import org.piecesapp.client.models.Format

/**
 * This function is used to get the raw content of an Asset.
 * It saves a lot of typing and can be used directly on an asset object
 *
 * It first tries to get the raw string from the original reference's fragment.
 * If that is null, it tries to get the raw string from the preview base reference's fragment.
 * If both are null, it returns an empty string.
 * @return the raw content of the Asset as a String. If no raw content is found, returns an empty string.
 */
fun Asset.rawContent(): String = this.original.reference?.fragment?.string?.raw
    ?: this.preview.base.reference?.fragment?.string?.raw.orEmpty()

/**
 * This method is used to extract the OCR content from an Asset object.
 * It first retrieves the OCR format of the asset, then converts the raw bytes of the file to an integer array.
 * If the file or its bytes are null, it uses an empty array instead.
 * Finally, it converts the integer array to a string and returns it.
 * @return A string representing the OCR content of the asset.
 */
fun Asset.ocrContent(): String {
    val textIntArray = getOcrFormat(this)?.file?.bytes?.raw.orEmpty().toIntArray()
    return String(textIntArray, 0, textIntArray.size)
}

@Suppress("RedundantNullableReturnType") // the `.first` method doesn't actually guarantee returning a value as it is actually a find replacement
private fun getOcrFormat(asset: Asset): Format? =
    asset.original.reference?.analysis?.image?.ocr?.raw?.id
        ?.let { id -> asset.formats.iterable.find { format -> format.id == id } } // find the asset directly pointed to by analysis result
        ?: asset.formats.iterable // use old search as fallback
            .first { it.classification.generic in setOf(ClassificationGenericEnum.cODE, ClassificationGenericEnum.tEXT) }

/**
 * A simple null-safe double getter that prioritizes native content due to probability and complexity,
 * and falls back to OCR content using rawContent and ocrContent respectively
 * @see rawContent
 * @see ocrContent
 */
fun Asset.stringRepresentation() = rawContent().ifBlank(::ocrContent)

/**
 * A nullable version of searchableContent
 * @see stringRepresentation
 */
fun Asset.stringRepresentationOrNull() = stringRepresentation().ifBlank { null }
val Asset.previewClassification get() = this.preview.base.reference?.classification
val Asset.classification get() = this.original.reference?.classification

val Asset.ext: ClassificationSpecificEnum? get() = this.classification?.specific
