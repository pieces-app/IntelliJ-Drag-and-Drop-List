package org.piecesapp.intellij.draganddroplist.toolWindow.tree

import org.piecesapp.client.models.Asset
import javax.swing.Icon

/**
 * A data class that represents a minimal descriptor for an asset.
 * It is used to minimise memory usage for data required to list and reference the snippets
 *
 * @property id The unique identifier of the asset.
 * @property name The name of the asset. It can be null.
 * @property icon The icon representing the asset's type. It is optional and can be null.
 *
 * @constructor Creates a new instance of MinimalAssetDescriptor using an Asset object.
 * @param asset The Asset object to create the MinimalAssetDescriptor from.
 */
@Suppress("KDocUnresolvedReference") // the secondary constructor's param is not resolved by IntelliSense
data class MinimalAssetDescriptor(val id: String, val name: String?, val icon: Icon? = null) {
    constructor(asset: Asset) : this(asset.id, asset.name)
}
