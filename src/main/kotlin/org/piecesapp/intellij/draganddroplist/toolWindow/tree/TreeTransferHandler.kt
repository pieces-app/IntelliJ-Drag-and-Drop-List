package org.piecesapp.intellij.draganddroplist.toolWindow.tree

import org.piecesapp.intellij.draganddroplist.sdkutil.stringRepresentationOrNull
import org.piecesapp.intellij.draganddroplist.services.ApiAccumulator
import org.piecesapp.intellij.draganddroplist.services.SaveProcessor
import org.piecesapp.intellij.draganddroplist.services.SnippetStore
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.piecesapp.client.models.Asset
import org.piecesapp.client.models.AssetReclassification
import org.piecesapp.client.models.ClassificationSpecificEnum
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.TransferHandler
import javax.swing.tree.DefaultMutableTreeNode

internal class TreeTransferHandler(private val project: Project) : TransferHandler() {
    private val editorManager = FileEditorManager.getInstance(project)

    /**
     * Determines if an import event can be performed given the drop information.
     * @param support The transferSupport handler from IDE.
     * @return whether the import event can be performed.
     */
    override fun canImport(support: TransferSupport): Boolean {
        // Only support drops
        if (!support.isDrop) {
            return false
        }

        // Display the drop location indicator
        support.setShowDropLocation(true)

        // Only support string or Asset flavors
        val isString = support.isDataFlavorSupported(DataFlavor.stringFlavor)
        val isAsset = support.isDataFlavorSupported(SnippetNodesTransferable.ASSET_FLAVOR)
        if (!isString && !isAsset) {
            return false
        }

        // Check if the data can be imported:
        // - for strings, check if a new text can be retrieved
        // - for assets, check if the drop location is valid
        if (!isAsset)
            return canGetNewText(support)

        // Do not allow a drop onto the same category.
        val dl = support.dropLocation as JTree.DropLocation
        val destination = dl.path
        val tree = support.component as JTree
        val selectedPath = tree.selectionPath
        return !(selectedPath == destination
            || destination == selectedPath?.parentPath
            || selectedPath?.parentPath == destination.parentPath)
    }

    // This function creates a transferable object that can be used to transfer data from a JTree component.
    override fun createTransferable(c: JComponent): Transferable? {
        // Check if the component is a JTree.
        val tree = c as? JTree ?: return null

        // Get the selected node from the tree.
        val node = tree.selectionPath?.lastPathComponent ?: return null

        // Check if the selected node is a SnippetTreeNode.
        if (node !is SnippetTreeNode) return null

        // Get the asset ID from the selected node.
        val assetId = node.id

        // Get the asset from the SnippetStore service.
        val asset = service<SnippetStore>()[assetId]

        // Create a SnippetNodesTransferable object with the asset.
        return SnippetNodesTransferable(asset)
    }


    override fun getSourceActions(c: JComponent): Int = COPY_OR_MOVE

    /**
     * Imports data from the specified transfer support.
     *
     * @param support the transfer support to import data from
     * @return true if the data was imported successfully, false otherwise
     */
    override fun importData(support: TransferSupport): Boolean {
        // Check if the data can be imported.
        if (!canImport(support))
            return false

        // Check if the data is an asset.
        val isAsset = support.isDataFlavorSupported(SnippetNodesTransferable.ASSET_FLAVOR)
        // If the data is an asset, import it as an asset.
        if (isAsset) {
            val ext = getTargetType(support.dropLocation as JTree.DropLocation) ?: return false

            // Reclassify the asset and put its updated version in the snippet store.
            service<ApiAccumulator>().asset.assetReclassify(
                false,
                AssetReclassification(
                    ext,
                    support.transferable.getTransferData(SnippetNodesTransferable.ASSET_FLAVOR) as Asset
                )
            ).let(service<SnippetStore>()::put)

            // Return true to indicate that the data was imported successfully.
            return true
        }

        // If the data is not an asset, import it as a string.
        importString(support)

        // Return true to indicate that the data was imported successfully.
        return true
    }

    /**
     * Checks if the given transfer support contains valid text that can be used to create a new asset.
     *
     * @param source the transfer support to check
     * @return true if the transfer support contains valid text, false otherwise
     */
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

    /**
     * Imports a string from a transfer support object.
     *
     * @param source the transfer support object
     */
    private fun importString(source: TransferSupport) {
        // Get the selected text editor and file
        val editor = editorManager.selectedTextEditor
        val file = editorManager.selectedEditor?.file!!

        // Get the string from the transferable
        val text = source.transferable.getTransferData(DataFlavor.stringFlavor) as String

        // Get the target type
        val ext = getTargetType(source.dropLocation as JTree.DropLocation)

        // Build the seed
        val seed = SaveProcessor.buildSeed(project, editor, file, text, ext)

        // Save the seed
        SaveProcessor.save(seed)
    }


    /**
     * Gets the target type of the drop location.
     *
     * @param dl the drop location
     * @return the target type, or null if the drop location is not valid
     */
    private fun getTargetType(dl: JTree.DropLocation): ClassificationSpecificEnum? {
        // Get the last path component of the drop location
        var destination = dl.path?.lastPathComponent as? DefaultMutableTreeNode ?: return null

        // If the destination is a SnippetTreeNode, get the parent path component
        if (destination is SnippetTreeNode)
            destination = dl.path.parentPath.lastPathComponent as DefaultMutableTreeNode
        return destination.userObject as? ClassificationSpecificEnum
    }

}

/**
 * A class that implements the Transferable interface and allows to transfer an Asset or a String.
 *
 * @param asset the Asset to transfer, or null if no Asset is to be transferred
 * @param givenText the String to transfer, or null if no String is to be transferred
 */
class SnippetNodesTransferable(private val asset: Asset? = null, givenText: String? = null) : Transferable {
    private val text = givenText ?: asset?.stringRepresentationOrNull()

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        if (flavor == ASSET_FLAVOR) return asset!!
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
        // Define a unique data flavor
        val ASSET_FLAVOR = DataFlavor(Asset::class.java, "Top Level Asset")
    }
}