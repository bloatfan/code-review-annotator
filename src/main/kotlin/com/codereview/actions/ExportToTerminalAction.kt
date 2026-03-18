package com.codereview.actions

import com.codereview.editor.InlineCommentManager
import com.codereview.export.MarkdownExporter
import com.codereview.service.ReviewSessionService
import com.codereview.toolwindow.ReviewToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class ExportToTerminalAction : AnAction(
    "Submit Review",
    "Submit all review comments to the active terminal session",
    AllIcons.Actions.MenuPaste
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val session = ReviewSessionService.getInstance(project).currentSession

        if (session.comments.isEmpty() && session.summary.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeReviewAnnotator")
                .createNotification("No review content to submit.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
        val selectedContent = toolWindow?.contentManager?.selectedContent
        val allContents = toolWindow?.contentManager?.contents

        // Reworked terminal (GoLand 2024.1+): use TerminalWidget.sendCommandToExecute
        val newStyleWidget = selectedContent?.let { TerminalToolWindowManager.findWidgetByContent(it) }
            ?: allContents?.firstNotNullOfOrNull { TerminalToolWindowManager.findWidgetByContent(it) }

        // Classic JediTerm terminal (GoLand < 2024.1): find JBTerminalWidget in component tree
        val oldStyleWidget = if (newStyleWidget == null) {
            selectedContent?.let { UIUtil.findComponentOfType(it.component, JBTerminalWidget::class.java) }
                ?: allContents?.firstNotNullOfOrNull { UIUtil.findComponentOfType(it.component, JBTerminalWidget::class.java) }
        } else null

        if (newStyleWidget == null && oldStyleWidget?.ttyConnector == null) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeReviewAnnotator")
                .createNotification(
                    "No active terminal session found. Open a terminal tab first.",
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }

        val commentCount = session.comments.count { it.scope != com.codereview.model.CommentScope.REVIEW }
        val markdown = MarkdownExporter.export(session)

        if (newStyleWidget != null) {
            newStyleWidget.sendCommandToExecute(markdown)
        } else {
            // Bracketed paste: treated as a single block, not executed line-by-line
            oldStyleWidget!!.ttyConnector!!.write("\u001B[200~$markdown\u001B[201~")
        }

        toolWindow?.activate(null)

        InlineCommentManager.clearAllInlineComments(project)
        ReviewSessionService.getInstance(project).clearSession()
        ReviewToolWindowFactory.refreshPanel(project)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeReviewAnnotator")
            .createNotification(
                "Submitted review with $commentCount comment(s) to terminal.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
