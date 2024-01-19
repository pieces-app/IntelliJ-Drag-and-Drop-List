package com.github.kubapieces.intellijdraganddroplist.toolWindow

import com.github.kubapieces.intellijdraganddroplist.sdkutil.classification
import com.github.kubapieces.intellijdraganddroplist.sdkutil.ext
import com.github.kubapieces.intellijdraganddroplist.sdkutil.previewClassification
import com.github.kubapieces.intellijdraganddroplist.services.ApiAccumulator
import com.github.kubapieces.intellijdraganddroplist.services.SnippetStore
import com.github.kubapieces.intellijdraganddroplist.toolWindow.tree.MinimalAssetDescriptor
import com.github.kubapieces.intellijdraganddroplist.toolWindow.tree.SnippetTreeNode
import com.github.kubapieces.intellijdraganddroplist.toolWindow.tree.TreeTransferHandler
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.StatusText
import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.ClassificationGenericEnum
import org.piecesapp.client.models.ClassificationSpecificEnum
import javax.swing.DropMode
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class ExampleToolWindow(private val project: Project) : SimpleToolWindowPanel(true) {
    private val root = DefaultMutableTreeNode("Pieces")
    private val treeModel = DefaultTreeModel(root)

    private val tree = Tree(treeModel).apply {
        setEmptyText(emptyText)
        showsRootHandles = true
        isRootVisible = false
        dragEnabled = true
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        dropMode = DropMode.ON_OR_INSERT
        transferHandler = TreeTransferHandler(project)
        name = "ListView"
        TreeSpeedSearch(this, true) { it.lastPathComponent.toString() }
    }
    private val library = DefaultMutableTreeNode("All Snippets")

    private val myCellRenderer = object : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val text = value.toString()
            append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            if (leaf || row == -1 || value == library)
                return
        }
    }

    init {
        add(ScrollPaneFactory.createScrollPane(tree))
        updateTree()
        tree.cellRenderer = myCellRenderer
    }

    fun updateTree() {
        // save expanded paths for restore after refresh
        val expandedTypes = tree.getExpandedDescendants(TreePath(root.path))?.toList().orEmpty().map { it.lastPathComponent.toString() }
        // the 0 child count means the subtree wasn't there before, so if it appears after refresh, we want it expanded
        val expandLibrary = tree.isExpanded(TreePath(library.path)) || library.childCount == 0

        // region clear the tree
        root.removeAllChildren()
        library.removeAllChildren()
        // endregion

        rebuildMainNode(library) ?: return

        root.add(library)

        treeModel.reload()

        // region re-expand
        if (!expandLibrary)
            return

        tree.expandPath(TreePath(library.path))
        library.children().asSequence().forEach {
            if (it.toString() in expandedTypes)
                tree.expandPath(TreePath(treeModel.getPathToRoot(it)))
        }
        // endregion
    }

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
        // if our generic classification is code and we have a string fragment. return true, so we do not filter this out.
        if (asset.classification?.generic in acceptedTypes) {
            return true
        }
        return asset.previewClassification?.generic in acceptedTypes
        // TODO in the future might want to also check our overlay for overlayed text/code..
    }

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
     * @return whether rebuilding should continue
     */
    private fun rebuildMainNode(mainNode: DefaultMutableTreeNode): Boolean? {
        val assets = buildTypeTree()
        if (assets.isEmpty()) {
            treeModel.reload()
            return null
        }

        assets.map { entry ->
            val category = DefaultMutableTreeNode(entry.key)
            entry.value.forEach { assetDescriptor -> category.add(SnippetTreeNode(assetDescriptor)) }
            return@map category
        }
            .forEach(mainNode::add)
        return true
    }
}