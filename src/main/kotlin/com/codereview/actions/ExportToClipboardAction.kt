package com.codereview.actions

import com.codereview.export.MarkdownExporter
import com.codereview.service.ReviewSessionService
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class ExportToClipboardAction : AnAction(
    "Copy Review",
    "Copy all review comments as markdown to the clipboard",
    AllIcons.Actions.Copy
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val session = ReviewSessionService.getInstance(project).currentSession

        if (session.comments.isEmpty() && session.summary.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeReviewAnnotator")
                .createNotification("No review content to copy.", NotificationType.WARNING)
                .notify(project)
            return
        }

        val commentCount = session.comments.count { it.scope != com.codereview.model.CommentScope.REVIEW }
        val markdown = MarkdownExporter.export(session)
        CopyPasteManager.getInstance().setContents(StringSelection(markdown))

        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeReviewAnnotator")
            .createNotification(
                "Copied review with $commentCount comment(s) to clipboard.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}
