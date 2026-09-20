package com.codereview.actions

import com.codereview.editor.InlineCommentManager
import com.codereview.export.MarkdownExporter
import com.codereview.model.CommentScope
import com.codereview.service.ReviewSessionService
import com.codereview.toolwindow.ReviewToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class CutReviewAction : AnAction(
    "Cut Review",
    "Copy all review comments as markdown to the clipboard and remove them from the review",
    AllIcons.Actions.MenuCut
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val session = ReviewSessionService.getInstance(project).currentSession

        if (session.comments.isEmpty() && session.summary.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeReviewAnnotator")
                .createNotification("No review content to cut.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val commentCount = session.comments.count { it.scope != CommentScope.REVIEW }
        val markdown = MarkdownExporter.export(session)
        CopyPasteManager.getInstance().setContents(StringSelection(markdown))

        InlineCommentManager.clearAllInlineComments(project)
        ReviewSessionService.getInstance(project).clearSession()
        ReviewToolWindowFactory.refreshPanel(project)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeReviewAnnotator")
            .createNotification(
                "Cut review with $commentCount comment(s) to clipboard.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
