package com.codereview.actions

import com.codereview.editor.InlineCommentManager
import com.codereview.model.CommentScope
import com.codereview.model.ReviewComment
import com.codereview.service.ReviewSessionService
import com.codereview.toolwindow.ReviewToolWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.VcsDataKeys

class AddLineCommentFromEditorAction : AnAction(
    "Add Review Comment Here...",
    "Add a review comment on the selected lines",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val filePath = resolveFilePath(e, editor) ?: return

        val projectBasePath = project.basePath ?: ""
        val relativePath = filePath.let {
            if (it.startsWith(projectBasePath)) it.removePrefix(projectBasePath).removePrefix("/") else it
        }

        val (lineStart, lineEnd) = getLineRange(editor)

        val anchorLine = if (lineEnd != lineStart) lineEnd else lineStart
        InlineCommentManager.showEditorPopup(
            editor = editor,
            line1Based = anchorLine,
            existingComment = null,
            onSave = { type, text ->
                val comment = ReviewComment(
                    scope = CommentScope.LINE,
                    type = type,
                    text = text,
                    filePath = relativePath,
                    lineStart = lineStart,
                    lineEnd = if (lineEnd != lineStart) lineEnd else null
                )
                ReviewSessionService.getInstance(project).addComment(comment)
                InlineCommentManager.addInlineComment(project, editor, comment)
                ReviewToolWindowFactory.refreshPanel(project)
            }
        )
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = e.project != null && editor != null && resolveFilePath(e, editor) != null
    }

    private fun resolveFilePath(e: AnActionEvent, editor: Editor?): String? {
        if (editor == null) return null

        // Try VCS file path first (works in diff viewers)
        e.getData(VcsDataKeys.FILE_PATH)?.let { return it.path }

        // Fall back to regular editor file resolution
        return FileDocumentManager.getInstance().getFile(editor.document)?.path
    }

    companion object {
        fun getLineRange(editor: Editor): Pair<Int, Int> {
            val selectionModel = editor.selectionModel
            val document = editor.document
            return if (selectionModel.hasSelection()) {
                val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
                val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1
                startLine to endLine
            } else {
                val line = document.getLineNumber(editor.caretModel.offset) + 1
                line to line
            }
        }
    }
}
