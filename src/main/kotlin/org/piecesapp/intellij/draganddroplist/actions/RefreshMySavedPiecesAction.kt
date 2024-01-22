package org.piecesapp.intellij.draganddroplist.actions

import org.piecesapp.intellij.draganddroplist.services.ApiAccumulator
import org.piecesapp.intellij.draganddroplist.services.SnippetStore
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

/**
 * An Action clickable by the user, allowing to manually refresh Snippet tree
 */
class RefreshMySavedPiecesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        performRefresh()
    }
    companion object {
        /**
         * This method performs a refresh operation. It asynchronously fetches a snapshot of assets,
         * maps each asset to its ID, clears the current store, and then populates the store with the fresh assets.
         * It is extracted as a static function, so it can be used not only in an instantiated action
         */
        fun performRefresh() {
            service<ApiAccumulator>().doAsync {
                val freshAssets = assets.assetsSnapshot(true, false, false)
                    .iterable
                    .map { it.id to it }
                val store = service<SnippetStore>()
                store.clear()
                store.putAll(freshAssets)
            }
        }
    }
}
