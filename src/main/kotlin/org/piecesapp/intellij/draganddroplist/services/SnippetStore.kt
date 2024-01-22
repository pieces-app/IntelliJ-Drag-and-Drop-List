package org.piecesapp.intellij.draganddroplist.services

import org.piecesapp.intellij.draganddroplist.toolWindow.ExampleToolWindow
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import org.piecesapp.client.models.Asset

/**
 * A basic wrapper for an underlying map that decorates most of its accessors and setters with a Snippet List update trigger
 */
@Service
class SnippetStore {
    private val store: MutableMap<String, Asset> = mutableMapOf()

    val values get() = store.values

    fun put(asset: Asset) {
        store[asset.id] = asset
        triggerListUpdate()
    }

    fun putAll(assetPairs: Iterable<Pair<String, Asset>>) {
        store.putAll(assetPairs)
        triggerListUpdate()
    }

    fun remove(id: String) {
        store.remove(id)
        triggerListUpdate()
    }

    fun clear(refresh: Boolean = false) {
        store.clear()
        if (refresh) triggerListUpdate()
    }

    operator fun get(id: String) = store[id]

    /**
     * This function triggerListUpdate is used to update the tree structure of the tool window named "MyToolWindow" in all open projects.
     * It first retrieves all open projects from the ProjectManager service.
     * For each project, it gets the ToolWindowManager service and uses it to find the tool window named "MyToolWindow".
     * It then gets the content manager of this tool window, retrieves its contents, and gets the first content.
     * If the component of this content is an instance of ExampleToolWindow, it is cast to this type.
     * Finally, for each ExampleToolWindow found, it invokes the updateTree method asynchronously.
     */
    private fun triggerListUpdate() {
        service<ProjectManager>().openProjects.map { project ->
            project.service<ToolWindowManager>()
                .getToolWindow("MyToolWindow")
                ?.contentManager
                ?.contents
                ?.first()
                ?.component as? ExampleToolWindow
        }
            .onEach {
                invokeLater { it?.updateTree() }
            }
    }
}