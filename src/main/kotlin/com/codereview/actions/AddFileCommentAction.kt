package com.codereview.actions

import com.codereview.model.CommentScope
import com.codereview.model.ReviewComment
import com.codereview.service.ReviewSessionService
import com.codereview.toolwindow.ReviewToolWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys

class AddFileCommentAction : AnAction("Add File Comment...", "Add a comment on this file", null) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val changes = e.getData(VcsDataKeys.CHANGES) ?: return
        val change = changes.firstOrNull() ?: return
        val filePath = (change.afterRevision?.file ?: change.beforeRevision?.file)
            ?.path ?: return

        val projectBasePath = project.basePath ?: ""
        val relativePath = if (filePath.startsWith(projectBasePath)) {
            filePath.removePrefix(projectBasePath).removePrefix("/")
        } else {
            filePath
        }

        val dialog = CommentDialog(project, "Add File Comment", filePath = relativePath)
        if (dialog.showAndGet()) {
            val comment = ReviewComment(
                scope = CommentScope.FILE,
                type = dialog.selectedType,
                text = dialog.commentText,
                filePath = relativePath
            )
            ReviewSessionService.getInstance(project).addComment(comment)
            ReviewToolWindowFactory.refreshPanel(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val changes = e.getData(VcsDataKeys.CHANGES)
        e.presentation.isEnabledAndVisible = e.project != null && changes != null && changes.isNotEmpty()
    }
}
