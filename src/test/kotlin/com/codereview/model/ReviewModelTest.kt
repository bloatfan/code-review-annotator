package com.codereview.model

import org.junit.Assert.*
import org.junit.Test

class ReviewModelTest {

    @Test
    fun `ReviewComment has correct defaults`() {
        val comment = ReviewComment(text = "Test comment")
        assertEquals(CommentScope.REVIEW, comment.scope)
        assertEquals(CommentType.NOTE, comment.type)
        assertEquals("Test comment", comment.text)
        assertNull(comment.filePath)
        assertNull(comment.lineStart)
        assertNull(comment.lineEnd)
        assertTrue(comment.id.isNotEmpty())
        assertTrue(comment.createdAt > 0)
    }

    @Test
    fun `ReviewComment with all fields`() {
        val comment = ReviewComment(
            id = "test-id",
            scope = CommentScope.LINE,
            type = CommentType.ISSUE,
            text = "Bug here",
            filePath = "src/main.go",
            lineStart = 10,
            lineEnd = 15,
            createdAt = 12345L
        )
        assertEquals("test-id", comment.id)
        assertEquals(CommentScope.LINE, comment.scope)
        assertEquals(CommentType.ISSUE, comment.type)
        assertEquals("Bug here", comment.text)
        assertEquals("src/main.go", comment.filePath)
        assertEquals(10, comment.lineStart)
        assertEquals(15, comment.lineEnd)
        assertEquals(12345L, comment.createdAt)
    }

    @Test
    fun `ReviewSession has correct defaults`() {
        val session = ReviewSession()
        assertTrue(session.id.isNotEmpty())
        assertTrue(session.comments.isEmpty())
        assertTrue(session.createdAt > 0)
        assertNull(session.baseBranch)
    }

    @Test
    fun `ReviewSession manages comments`() {
        val session = ReviewSession()
        val comment1 = ReviewComment(text = "First")
        val comment2 = ReviewComment(text = "Second")

        session.comments.add(comment1)
        session.comments.add(comment2)

        assertEquals(2, session.comments.size)
        assertEquals("First", session.comments[0].text)
        assertEquals("Second", session.comments[1].text)

        session.comments.removeAll { it.id == comment1.id }
        assertEquals(1, session.comments.size)
        assertEquals("Second", session.comments[0].text)
    }
}
