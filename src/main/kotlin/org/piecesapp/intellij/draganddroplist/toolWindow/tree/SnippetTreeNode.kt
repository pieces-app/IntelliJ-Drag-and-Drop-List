package org.piecesapp.intellij.draganddroplist.toolWindow.tree

import javax.swing.tree.DefaultMutableTreeNode

// This class represents a node in a tree of code snippets.
class SnippetTreeNode(descriptor: MinimalAssetDescriptor) : DefaultMutableTreeNode(descriptor) {

    // The name of the snippet.
    override fun toString(): String = (userObject as MinimalAssetDescriptor).name.orEmpty()

    val id get() = (userObject as MinimalAssetDescriptor).id

    val icon get() = (userObject as MinimalAssetDescriptor).icon
}