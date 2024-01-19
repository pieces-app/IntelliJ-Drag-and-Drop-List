package com.github.kubapieces.intellijdraganddroplist.sdkutil

import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.ClassificationGenericEnum
import org.piecesapp.client.models.ClassificationSpecificEnum
import org.piecesapp.client.models.Format

// the following method saves a lot of typing and can be used directly on an asset object
fun Asset.rawContent(): String = this.original.reference?.fragment?.string?.raw
    ?: this.preview.base.reference?.fragment?.string?.raw.orEmpty()

fun Asset.ocrContent(): String {
    val textIntArray = getOcrFormat(this)?.file?.bytes?.raw.orEmpty().toIntArray()
    return String(textIntArray, 0, textIntArray.size)
}

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
 * @since 4.8.0
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
