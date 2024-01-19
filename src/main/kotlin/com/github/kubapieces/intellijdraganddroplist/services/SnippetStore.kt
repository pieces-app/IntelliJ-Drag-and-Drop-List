package com.github.kubapieces.intellijdraganddroplist.services

import com.github.kubapieces.intellijdraganddroplist.toolWindow.ExampleToolWindow
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import org.piecesapp.client.models.Asset

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