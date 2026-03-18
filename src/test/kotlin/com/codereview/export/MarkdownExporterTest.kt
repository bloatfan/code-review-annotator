package com.codereview.export

import com.codereview.model.*
import org.junit.Assert.*
import org.junit.Test

class MarkdownExporterTest {

    @Test
    fun `export empty session returns empty string`() {
        val session = ReviewSession()
        assertEquals("", MarkdownExporter.export(session))
    }

    @Test
    fun `export single review comment`() {
        val session = ReviewSession(
            comments = mutableListOf(
                ReviewComment(scope = CommentScope.REVIEW, type = CommentType.NOTE, text = "Looks clean")
            )
        )
        val result = MarkdownExporter.export(session)
        assertTrue(result.contains("1. **[NOTE]** `Review Comment` - Looks clean"))
        assertTrue(result.contains("I reviewed your code"))
    }

    @Test
    fun `export file comment`() {
        val session = ReviewSession(
            comments = mutableListOf(
                ReviewComment(scope = CommentScope.FILE, type = CommentType.ISSUE, text = "Missing tests", filePath = "src/auth.go")
            )
        )
        val result = MarkdownExporter.export(session)
        assertTrue(result.contains("1. **[ISSUE]** `src/auth.go` - Missing tests"))
    }

    @Test
    fun `export line comment single line`() {
        val session = ReviewSession(
            comments = mutableListOf(
                ReviewComment(
                    scope = CommentScope.LINE, type = CommentType.SUGGESTION,
                    text = "Add unit tests", filePath = "src/auth.go",
                    lineStart = 42, lineEnd = null
                )
            )
        )
        val result = MarkdownExporter.export(session)
        assertTrue(result.contains("1. **[SUGGESTION]** `src/auth.go:42` - Add unit tests"))
    }

    @Test
    fun `export line comment range`() {
        val session = ReviewSession(
            comments = mutableListOf(
                ReviewComment(
                    scope = CommentScope.LINE, type = CommentType.ISSUE,
                    text = "Race condition", filePath = "src/handler.go",
                    lineStart = 10, lineEnd = 15
                )
            )
        )
        val result = MarkdownExporter.export(session)
        assertTrue(result.contains("1. **[ISSUE]** `src/handler.go:10-15` - Race condition"))
    }

    @Test
    fun `export sorts by scope then file then line`() {
        val session = ReviewSession(
            comments = mutableListOf(
                ReviewComment(scope = CommentScope.LINE, type = CommentType.ISSUE, text = "Bug", filePath = "z.go", lineStart = 5),
                ReviewComment(scope = CommentScope.REVIEW, type = CommentType.NOTE, text = "Overall clean"),
                ReviewComment(scope = CommentScope.FILE, type = CommentType.SUGGESTION, text = "Refactor", filePath = "a.go"),
                ReviewComment(scope = CommentScope.LINE, type = CommentType.NOTE, text = "Nice", filePath = "a.go", lineStart = 1)
            )
        )
        val result = MarkdownExporter.export(session)
        val lines = result.lines().filter { it.matches(Regex("^\\d+\\..*")) }
        assertEquals(4, lines.size)
        assertTrue(lines[0].contains("Review Comment"))  // REVIEW first
        assertTrue(lines[1].contains("a.go` - Refactor"))  // FILE second
        assertTrue(lines[2].contains("a.go:1"))  // LINE, a.go before z.go
        assertTrue(lines[3].contains("z.go:5"))
    }

    @Test
    fun `export contains header and type legend`() {
        val session = ReviewSession(
            comments = mutableListOf(ReviewComment(text = "Test"))
        )
        val result = MarkdownExporter.export(session)
        assertTrue(result.contains("I reviewed your code and have the following comments."))
        assertTrue(result.contains("Comment types: ISSUE (problems to fix)"))
        assertTrue(result.contains("SUGGESTION (improvements)"))
        assertTrue(result.contains("NOTE (observations)"))
        assertTrue(result.contains("NOTE (observations)"))
    }
}
