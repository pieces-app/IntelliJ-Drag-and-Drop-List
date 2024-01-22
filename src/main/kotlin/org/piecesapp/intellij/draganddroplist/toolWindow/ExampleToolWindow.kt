package org.piecesapp.intellij.draganddroplist.toolWindow

import org.piecesapp.intellij.draganddroplist.sdkutil.classification
import org.piecesapp.intellij.draganddroplist.sdkutil.ext
import org.piecesapp.intellij.draganddroplist.sdkutil.previewClassification
import org.piecesapp.intellij.draganddroplist.services.ApiAccumulator
import org.piecesapp.intellij.draganddroplist.services.SnippetStore
import org.piecesapp.intellij.draganddroplist.toolWindow.tree.MinimalAssetDescriptor
import org.piecesapp.intellij.draganddroplist.toolWindow.tree.SnippetTreeNode
import org.piecesapp.intellij.draganddroplist.toolWindow.tree.TreeTransferHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.StatusText
import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.ClassificationGenericEnum
import org.piecesapp.client.models.ClassificationSpecificEnum
import javax.swing.DropMode
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class ExampleToolWindow(private val project: Project) : SimpleToolWindowPanel(true) {
    private val root = DefaultMutableTreeNode("Pieces")
    private val treeModel = DefaultTreeModel(root)

    // Create a tree with the tree model created above
    private val tree = Tree(treeModel).apply {
        setEmptyText(emptyText)
        // Hide the root node of the tree as it's not necessary
        isRootVisible = false

        // Enable drag and drop on the tree
        dragEnabled = true

        // Set the selection mode of the tree to single selection
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        // Set the drop mode of the tree to allow dropping on or between nodes
        dropMode = DropMode.ON_OR_INSERT

        // Set the transfer handler for the tree to enable proper Asset handling
        transferHandler = TreeTransferHandler(project)

        // Add a tree speed search to the tree to allow for quick searching
        TreeSpeedSearch(this, true) { it.lastPathComponent.toString() }
    }


    init {
        add(ScrollPaneFactory.createScrollPane(tree))
        updateTree()
    }

    /**
     * Updates the tree by removing all children from the root node, rebuilding the main node, and reloading the tree model.
     */
    fun updateTree() {
        root.removeAllChildren()
        rebuildMainNode(root) ?: return

        treeModel.reload()
    }

    /**
     * Builds a tree of assets, grouped by their classification.
     *
     * @return a map of classification to a list of asset descriptors
     */
    private fun buildTypeTree(): Map<ClassificationSpecificEnum, MutableList<MinimalAssetDescriptor>> {
        val sorted = mutableMapOf<ClassificationSpecificEnum, MutableList<MinimalAssetDescriptor>>()
        service<SnippetStore>().values
            .filter (::acceptedTypesFilter)
            .sortedByDescending { it.updated.value }
            .forEach { asset ->
            // Should never happen, but it's a guard clause for compiler not to complain
            var extension = asset.ext ?: return@forEach
            // this is to avoid dual plain text entries if one snippet has txt and the other text as extension
            if (extension == ClassificationSpecificEnum.text)
                extension = ClassificationSpecificEnum.txt

            if (sorted[extension] == null)
                sorted[extension] = mutableListOf()

            sorted[extension]!!.add(MinimalAssetDescriptor(asset))
        }
        return sorted
    }

    private fun acceptedTypesFilter(asset: Asset): Boolean {
        val acceptedTypes = arrayOf(ClassificationGenericEnum.cODE, ClassificationGenericEnum.tEXT, ClassificationGenericEnum.iMAGE)
        // if our generic classification is code, and we have a string fragment. return true, so we do not filter this out.
        if (asset.classification?.generic in acceptedTypes) {
            return true
        }
        return asset.previewClassification?.generic in acceptedTypes
    }

    /**
     * Sets the text to be displayed when the tree is empty.
     * It takes a StatusText object as an argument and uses it to set the text,
     * append links, and format the text.
     */
    private fun setEmptyText(statusText: StatusText) {
        @Suppress("DialogTitleCapitalization")
        statusText.setText("It looks like you don't have any snippets yet.")
            .appendLine("To learn how to add them, ")
            .appendText("visit our website", SimpleTextAttributes.LINK_ATTRIBUTES
            ) { BrowserUtil.browse("https://code.pieces.app") }
            .appendLine("When you have created your first snippet,", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
            .appendLine("refresh the list", SimpleTextAttributes.LINK_ATTRIBUTES) {
                service<ApiAccumulator>().doAsync {
                    val store = service<SnippetStore>()
                    store.clear()
                    store.putAll(
                        assets.assetsSnapshot(true, false, false).iterable
                            .map { it.id to it }
                    )
                }
            }
    }

    /**
     * Rebuilds the main node of the tree, which contains all the assets grouped by their classification.
     *
     * @param mainNode the root node of the tree
     * @return whether rebuilding should continue
     */
    private fun rebuildMainNode(mainNode: DefaultMutableTreeNode): Boolean? {
        // Get all the assets from the snippet store
        val assets = buildTypeTree()

        // If there are no assets, clear the tree and return
        if (assets.isEmpty()) {
            treeModel.reload()
            return null
        }

        // Create a tree node for each classification and add it to the main node
        assets.map { entry ->
            val category = DefaultMutableTreeNode(entry.key)
            entry.value.forEach { assetDescriptor -> category.add(SnippetTreeNode(assetDescriptor)) }
            return@map category
        }
            .forEach(mainNode::add)

        // Return true to indicate that rebuilding should continue
        return true
    }

}