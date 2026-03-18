package com.codereview.actions

import com.codereview.editor.InlineCommentManager
import com.codereview.service.ReviewSessionService
import com.codereview.toolwindow.ReviewToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ClearSessionAction : AnAction("Clear Session", "Clear all review comments", AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to clear all review comments?",
            "Clear Review Session",
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            InlineCommentManager.clearAllInlineComments(project)
            ReviewSessionService.getInstance(project).clearSession()
            ReviewToolWindowFactory.refreshPanel(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
