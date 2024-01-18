package com.github.kubapieces.intellijdraganddroplist.toolWindow.tree

import app.pieces.plugins.jetbrains.actions.SaveProcessor
import app.pieces.plugins.jetbrains.actions.listview.ReclassifyAction
import app.pieces.plugins.jetbrains.ex.stringRepresentation
import app.pieces.plugins.jetbrains.services.assets.GlobalStorage
import app.pieces.plugins.jetbrains.services.reporters.Analytics
import app.pieces.plugins.jetbrains.ui.components.SnippetTreeNode
import app.pieces.plugins.jetbrains.util.LangToExt
import app.pieces.sdk.models.Asset
import app.pieces.sdk.models.ClassificationSpecificEnum
import app.pieces.sdk.models.UIElements
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.tree.DefaultMutableTreeNode

internal class TreeTransferHandler(private val project: Project) : TransferHandler() {
    private val editorManager = FileEditorManager.getInstance(project)

    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDrop) {
            return false
        }
        support.setShowDropLocation(true)
        val isString = support.isDataFlavorSupported(DataFlavor.stringFlavor)
        val isAsset = support.isDataFlavorSupported(SnippetNodesTransferable.ASSET_FLAVOR)
        if (!isString && !isAsset) {
            return false
        }

        if (!isAsset)
            return canGetNewText(support)

        // Do not allow a drop onto the same category.
        val dl = support.dropLocation as JTree.DropLocation
        val destination = dl.path
        if (destination.pathCount < 2 || destination.getPathComponent(1).toString() != "All Snippets")
            return false // because we don't want to risk reclassification attempts when user drags assets onto suggested or similar sections
        val tree = support.component as JTree
        val selectedPath = tree.selectionPath
        return !(selectedPath == destination
            || destination == selectedPath?.parentPath
            || selectedPath?.parentPath == destination.parentPath)
    }

    override fun createTransferable(c: JComponent): Transferable? {
        val tree = c as JTree
        val node = tree.selectionPath?.lastPathComponent ?: return null
        if (node !is SnippetTreeNode) return null
        val assetId = node.id
        val asset = service<GlobalStorage>().lightSnippet(assetId)
        return SnippetNodesTransferable(asset)
    }

    override fun getSourceActions(c: JComponent): Int = COPY_OR_MOVE

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support))
            return false

        val isAsset = support.isDataFlavorSupported(SnippetNodesTransferable.ASSET_FLAVOR)
        if (isAsset) {
            val ext = getTargetType(support.dropLocation as JTree.DropLocation) ?: return false
            Analytics.interact("asset_reclassified_via_dragndrop", UIElements.pieces_list)
            ReclassifyAction.doReclassify(support.transferable.getTransferData(SnippetNodesTransferable.ASSET_FLAVOR) as Asset, ext, project)
            return true
        }

        importString(support)
        return true
    }

    private fun canGetNewText(source: TransferSupport?): Boolean {
        if (source == null)
            return false
        if (!source.isDataFlavorSupported(DataFlavor.stringFlavor))
            return false
        val text = source.transferable.getTransferData(DataFlavor.stringFlavor) as String
        if (text.isBlank() || (!text.trim().contains(Regex("\\s")) && text.length < 16))
            return false // block one-word drags as they might be mistakes
        // Only allow when we can be sure the content is from current editor,
        // so we can get enough metadata to seed an asset
        val editor = editorManager.selectedTextEditor
        val file = editorManager.selectedEditor?.file
        if (editor == null || file == null)
            return false
        if (!editor.document.text.contains(text))
            return false
        // Update drop action, so we don't delete the dragged content from the editor
        source.dropAction = COPY
        return text.length < 1000000
    }

    private fun importString(source: TransferSupport) {
        val editor = editorManager.selectedTextEditor
        val file = editorManager.selectedEditor?.file!!
        val text = source.transferable.getTransferData(DataFlavor.stringFlavor) as String
        val ext = getTargetType(source.dropLocation as JTree.DropLocation)
        val seed = SaveProcessor.buildSeed(project, editor, file, text, ext)
        Analytics.interact("asset_created_by_dragging_code_onto_${if (ext == null) "list" else "specific_type"}", UIElements.pieces_list)
        SaveProcessor.save(seed)
    }

    private fun getTargetType(dl: JTree.DropLocation): ClassificationSpecificEnum? {
        var destination = dl.path.lastPathComponent as DefaultMutableTreeNode
        if (destination is SnippetTreeNode)
            destination = dl.path.parentPath.lastPathComponent as DefaultMutableTreeNode
        return LangToExt.getValue(destination.userObject as String)
    }

}

class SnippetNodesTransferable(private val asset: Asset? = null, givenText: String? = null) : Transferable {
    private val text = givenText ?: asset?.stringRepresentation()?.ifBlank { null }

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        if (flavor == ASSET_FLAVOR) return asset!!
        Analytics.interact("asset_dragndropped_into_text_destination", UIElements.pieces_list)
        return text!!
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        val flavors = mutableListOf<DataFlavor>()
        if (asset != null)
            flavors.add(ASSET_FLAVOR)
        if (text != null)
            flavors.add(DataFlavor.stringFlavor)
        return flavors.toTypedArray()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = transferDataFlavors.contains(flavor)

    companion object {
        val ASSET_FLAVOR = DataFlavor(Asset::class.java, "Top Level Asset")
    }
}