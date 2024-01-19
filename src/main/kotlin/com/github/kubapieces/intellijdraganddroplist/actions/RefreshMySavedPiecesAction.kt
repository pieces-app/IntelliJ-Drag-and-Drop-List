package com.github.kubapieces.intellijdraganddroplist.actions

import com.github.kubapieces.intellijdraganddroplist.services.ApiAccumulator
import com.github.kubapieces.intellijdraganddroplist.services.SnippetStore
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class RefreshMySavedPiecesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        performRefresh()
    }
    companion object {
        fun performRefresh() {
            service<ApiAccumulator>().doAsync {
                assets.assetsSnapshot(true, false, false)
                    .iterable
                    .map { it.id to it }
                    .let(service<SnippetStore>()::putAll)
            }
        }
    }
}
