package com.github.kubapieces.intellijdraganddroplist.toolWindow.tree

import com.intellij.openapi.components.service
import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.SearchedAsset
import org.piecesapp.client.models.SeededConnectorAsset
import javax.swing.Icon

data class MinimalAssetDescriptor(val id: String, val name: String?, val icon: Icon? = null) {
    constructor(asset: Asset) : this(asset.id, asset.name)
    constructor(asset: SearchedAsset, trueAsset: Asset? = asset.asset ?: service<GlobalStorage>().lightSnippet(asset.identifier))
            : this(asset.identifier, trueAsset?.name)

    constructor(assetPair: Pair<String, SeededConnectorAsset>) : this(
        assetPair.first,
        assetPair.second.metadata?.name ?: "Explored Snippet",
    )
}
