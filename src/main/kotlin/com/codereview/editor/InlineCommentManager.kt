package com.codereview.editor

import com.codereview.model.CommentType
import com.codereview.model.ReviewComment
import com.codereview.service.ReviewSessionService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

object InlineCommentManager {

    private val log = Logger.getInstance(InlineCommentManager::class.java)
    private val inlays = mutableMapOf<String, Inlay<InlineCommentRenderer>>()
    private var currentEditorPopup: JBPopup? = null

    fun addInlineComment(@Suppress("UNUSED_PARAMETER") project: Project, editor: Editor, comment: ReviewComment) {
        val lineStart = (comment.lineStart ?: return) - 1
        val document = editor.document
        if (lineStart < 0 || lineStart >= document.lineCount) {
            log.warn("[CodeReview] addInlineComment SKIP: commentId=${comment.id}, lineStart=$lineStart out of range (lineCount=${document.lineCount})")
            return
        }

        val lineEnd = ((comment.lineEnd ?: comment.lineStart) - 1).coerceIn(0, document.lineCount - 1)
        val offset = document.getLineEndOffset(lineEnd)

        val renderer = InlineCommentRenderer(comment)
        val properties = InlayProperties()
            .relatesToPrecedingText(true)
            .showAbove(false)
            .priority(0)

        val inlay = editor.inlayModel.addBlockElement(offset, properties, renderer)
        if (inlay == null) {
            log.warn("[CodeReview] addInlineComment FAILED: addBlockElement returned null for commentId=${comment.id}, offset=$offset, editor=${System.identityHashCode(editor)}")
            return
        }
        log.warn("[CodeReview] addInlineComment OK: commentId=${comment.id}, line=${lineStart+1}, editor=${System.identityHashCode(editor)}, inlay=${System.identityHashCode(inlay)}")
        inlays[comment.id] = inlay
    }

    fun removeInlineComment(commentId: String) {
        inlays.remove(commentId)?.let { inlay ->
            try { inlay.dispose() } catch (_: Exception) {}
        }
    }

    fun updateInlineComment(project: Project, editor: Editor, comment: ReviewComment) {
        removeInlineComment(comment.id)
        addInlineComment(project, editor, comment)
    }

    fun clearAllInlineComments(project: Project) {
        val session = ReviewSessionService.getInstance(project).currentSession
        session.comments.forEach { removeInlineComment(it.id) }
        inlays.values.forEach { inlay ->
            try { inlay.dispose() } catch (_: Exception) {}
        }
        inlays.clear()
    }

    fun reapplyInlineComments(project: Project, editor: Editor, filePath: String) {
        val session = ReviewSessionService.getInstance(project).currentSession
        val matching = session.comments.filter { it.filePath == filePath && it.lineStart != null }
        log.warn("[CodeReview] reapplyInlineComments: filePath=$filePath, editor=${System.identityHashCode(editor)}, matchingComments=${matching.size}, totalInlays=${inlays.size}")
        matching.forEach { comment ->
                val existing = inlays[comment.id]
                log.warn("[CodeReview] reapplyInlineComments check: commentId=${comment.id}, existing=${existing != null}, isValid=${existing?.isValid}, existingEditor=${existing?.let { System.identityHashCode(it.editor) }}, currentEditor=${System.identityHashCode(editor)}, sameEditor=${existing?.editor == editor}")
                if (existing == null || !existing.isValid || existing.editor != editor) {
                    log.warn("[CodeReview] reapplyInlineComments RECREATING: commentId=${comment.id}")
                    existing?.let { try { it.dispose() } catch (_: Exception) {} }
                    inlays.remove(comment.id)
                    addInlineComment(project, editor, comment)
                } else {
                    log.warn("[CodeReview] reapplyInlineComments SKIPPED (already valid): commentId=${comment.id}")
                }
            }
    }

    /**
     * Shows an inline editor popup below the given line for creating or editing a comment.
     * @param line1Based the 1-based line number to anchor below
     * @param existingComment if non-null, pre-fills the editor with this comment's data
     * @param onSave callback receiving (type, text) when the user saves
     */
    fun showEditorPopup(
        editor: Editor,
        line1Based: Int,
        existingComment: ReviewComment?,
        onSave: (CommentType, String) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        // Dismiss any existing editor popup
        currentEditorPopup?.cancel()
        currentEditorPopup = null

        val document = editor.document
        val line0 = (line1Based - 1).coerceIn(0, document.lineCount - 1)
        val offset = document.getLineEndOffset(line0)
        val visualPoint = editor.offsetToXY(offset)
        val lineHeight = editor.lineHeight

        val panel = InlineCommentEditorPanel(
            initialType = existingComment?.type ?: CommentType.ISSUE,
            initialText = existingComment?.text ?: "",
            onSave = { type, text ->
                currentEditorPopup?.cancel()
                currentEditorPopup = null
                onSave(type, text)
            },
            onCancel = {
                currentEditorPopup?.cancel()
                currentEditorPopup = null
                onCancel?.invoke()
            }
        )

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel.textArea)
            .setRequestFocus(true)
            .setFocusable(true)
            .setMovable(false)
            .setResizable(false)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .setCancelKeyEnabled(false) // We handle ESC in the panel
            .createPopup()

        currentEditorPopup = popup

        // Position just below the target line
        val showPoint = Point(visualPoint.x, visualPoint.y + lineHeight)
        popup.show(RelativePoint(editor.contentComponent, showPoint))
    }
}
