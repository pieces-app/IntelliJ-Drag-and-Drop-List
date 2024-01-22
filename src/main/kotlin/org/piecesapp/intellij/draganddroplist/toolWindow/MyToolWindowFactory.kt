package org.piecesapp.intellij.draganddroplist.toolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class MyToolWindowFactory : ToolWindowFactory {
    /**
     * Creates the content of the tool window.
     *
     * @param project the project the tool window is associated with
     * @param toolWindow the tool window itself
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Get the action for refreshing the snippets
        val action = service<ActionManager>().getAction("DnD.RefreshSnippets")

        // Set the header actions for the tool window
        toolWindow.setTitleActions(listOf(action))

        // Create an instance of the tool window
        val myToolWindow = ExampleToolWindow(project)

        // Create the content for the tool window
        val content = ContentFactory.getInstance().createContent(myToolWindow, null, false)

        // Set the content to not be closable
        content.isCloseable = false

        // Add the content to the tool window
        toolWindow.contentManager.addContent(content)
    }
}
