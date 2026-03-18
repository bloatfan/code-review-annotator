package com.codereview.export

import com.codereview.model.CommentScope
import com.codereview.model.ReviewSession

object MarkdownExporter {

    fun export(session: ReviewSession): String {
        val exportableComments = session.comments.filter { it.scope != CommentScope.REVIEW }
        if (exportableComments.isEmpty() && session.summary.isEmpty()) return ""

        val sb = StringBuilder()
        if (session.summary.isNotEmpty()) {
            sb.appendLine(session.summary)
        } else {
            sb.appendLine("I reviewed your code and have the following comments. Please address them.")
        }
        sb.appendLine()

        if (exportableComments.isNotEmpty()) {
            sb.appendLine("Comment types: ISSUE (problems to fix), SUGGESTION (improvements), NOTE (observations)")
            sb.appendLine()

            val sorted = exportableComments.sortedWith(
                compareBy<com.codereview.model.ReviewComment> { it.scope.ordinal }
                    .thenBy { it.filePath ?: "" }
                    .thenBy { it.lineStart ?: Int.MAX_VALUE }
            )

            sorted.forEachIndexed { index, comment ->
                val location = when (comment.scope) {
                    CommentScope.LINE -> {
                        val lineRange = if (comment.lineEnd != null && comment.lineEnd != comment.lineStart) {
                            "${comment.lineStart}-${comment.lineEnd}"
                        } else {
                            "${comment.lineStart}"
                        }
                        "`${comment.filePath}:$lineRange`"
                    }
                    CommentScope.FILE -> "`${comment.filePath}`"
                    CommentScope.REVIEW -> "`Review Comment`"
                }
                sb.appendLine("${index + 1}. **[${comment.type.name}]** $location - ${comment.text}")
            }
        }

        return sb.toString().trimEnd() + "\n"
    }
}
