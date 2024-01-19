package com.github.kubapieces.intellijdraganddroplist.toolWindow.tree

import org.piecesapp.client.models.Asset
import javax.swing.Icon

data class MinimalAssetDescriptor(val id: String, val name: String?, val icon: Icon? = null) {
    constructor(asset: Asset) : this(asset.id, asset.name)
}
