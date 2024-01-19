package com.github.kubapieces.intellijdraganddroplist.toolWindow.tree

import javax.swing.tree.DefaultMutableTreeNode

class SnippetTreeNode(descriptor: MinimalAssetDescriptor) : DefaultMutableTreeNode(descriptor) {

    override fun toString(): String = (userObject as MinimalAssetDescriptor).name.orEmpty()

    val id get() = (userObject as MinimalAssetDescriptor).id

    val icon get() = (userObject as MinimalAssetDescriptor).icon
}