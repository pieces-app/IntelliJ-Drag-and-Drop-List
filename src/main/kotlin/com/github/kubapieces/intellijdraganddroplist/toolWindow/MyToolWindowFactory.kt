package com.github.kubapieces.intellijdraganddroplist.toolWindow

import com.github.kubapieces.intellijdraganddroplist.actions.RefreshMySavedPiecesAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val action = RefreshMySavedPiecesAction()
        action.templatePresentation.icon = AllIcons.Actions.Refresh
        action.templatePresentation.text = "Refresh Saved Pieces"
        toolWindow.setTitleActions(mutableListOf(action))
        val myToolWindow = ExampleToolWindow(project)
        val content = ContentFactory.getInstance().createContent(myToolWindow, null, false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }
}
