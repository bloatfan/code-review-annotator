package com.codereview.service

import com.codereview.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReviewSessionServiceTest {

    private lateinit var service: ReviewSessionService

    @Before
    fun setup() {
        service = ReviewSessionService()
    }

    @Test
    fun `addComment adds to session`() {
        val comment = ReviewComment(text = "Test")
        service.addComment(comment)
        assertEquals(1, service.currentSession.comments.size)
        assertEquals("Test", service.currentSession.comments[0].text)
    }

    @Test
    fun `removeComment removes by id`() {
        val comment1 = ReviewComment(id = "c1", text = "First")
        val comment2 = ReviewComment(id = "c2", text = "Second")
        service.addComment(comment1)
        service.addComment(comment2)

        service.removeComment("c1")
        assertEquals(1, service.currentSession.comments.size)
        assertEquals("c2", service.currentSession.comments[0].id)
    }

    @Test
    fun `removeComment with unknown id does nothing`() {
        val comment = ReviewComment(id = "c1", text = "First")
        service.addComment(comment)
        service.removeComment("nonexistent")
        assertEquals(1, service.currentSession.comments.size)
    }

    @Test
    fun `updateComment updates existing comment`() {
        val comment = ReviewComment(id = "c1", type = CommentType.NOTE, text = "Original")
        service.addComment(comment)

        service.updateComment("c1") { it.copy(type = CommentType.ISSUE, text = "Updated") }

        val updated = service.currentSession.comments[0]
        assertEquals(CommentType.ISSUE, updated.type)
        assertEquals("Updated", updated.text)
    }

    @Test
    fun `clearSession resets everything`() {
        service.addComment(ReviewComment(text = "Test"))
        val oldId = service.currentSession.id

        service.clearSession()

        assertEquals(0, service.currentSession.comments.size)
        assertNotEquals(oldId, service.currentSession.id)
    }

    @Test
    fun `serialization round trip preserves data`() {
        val comment = ReviewComment(
            id = "c1",
            scope = CommentScope.LINE,
            type = CommentType.SUGGESTION,
            text = "Add tests",
            filePath = "src/auth.go",
            lineStart = 42,
            lineEnd = 50,
            createdAt = 99999L
        )
        service.addComment(comment)

        val state = service.state

        // Create a new service and load the state
        val newService = ReviewSessionService()
        newService.loadState(state)

        val restored = newService.currentSession.comments[0]
        assertEquals("c1", restored.id)
        assertEquals(CommentScope.LINE, restored.scope)
        assertEquals(CommentType.SUGGESTION, restored.type)
        assertEquals("Add tests", restored.text)
        assertEquals("src/auth.go", restored.filePath)
        assertEquals(42, restored.lineStart)
        assertEquals(50, restored.lineEnd)
        assertEquals(99999L, restored.createdAt)
    }

    @Test
    fun `serialization round trip with multiple comments`() {
        service.addComment(ReviewComment(id = "c1", scope = CommentScope.REVIEW, text = "Overall good"))
        service.addComment(ReviewComment(id = "c2", scope = CommentScope.FILE, text = "Complex file", filePath = "handler.go"))
        service.addComment(ReviewComment(id = "c3", scope = CommentScope.LINE, text = "Bug", filePath = "main.go", lineStart = 10))

        val state = service.state
        val newService = ReviewSessionService()
        newService.loadState(state)

        assertEquals(3, newService.currentSession.comments.size)
        assertEquals("c1", newService.currentSession.comments[0].id)
        assertEquals("c2", newService.currentSession.comments[1].id)
        assertEquals("c3", newService.currentSession.comments[2].id)
    }

    @Test
    fun `serialization with null optional fields`() {
        val comment = ReviewComment(id = "c1", text = "Review note")
        service.addComment(comment)

        val state = service.state
        val newService = ReviewSessionService()
        newService.loadState(state)

        val restored = newService.currentSession.comments[0]
        assertNull(restored.filePath)
        assertNull(restored.lineStart)
        assertNull(restored.lineEnd)
    }
}
