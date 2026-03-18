package com.codereview.decoration

import com.codereview.service.ReviewSessionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNode
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import java.awt.Color
import java.awt.Component
import javax.swing.JTree
import javax.swing.tree.TreeCellRenderer

class CommentCountDecorator(
    private val delegate: TreeCellRenderer,
    private val project: Project
) : TreeCellRenderer {

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val component = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

        if (value !is ChangesBrowserNode<*>) return component

        val filePath = resolveFilePath(value) ?: return component
        val service = ReviewSessionService.getInstance(project)
        val count = service.currentSession.comments.count { it.filePath == filePath }
        if (count == 0) return component

        val coloredComponent = findSimpleColoredComponent(component) ?: return component
        insertCountAfterFilename(coloredComponent, count)

        return component
    }

    private fun insertCountAfterFilename(component: SimpleColoredComponent, count: Int) {
        // Collect all existing fragments
        val fragments = mutableListOf<Triple<String, SimpleTextAttributes, Any?>>()
        val it = component.iterator()
        while (it.hasNext()) {
            fragments.add(Triple(it.next(), it.textAttributes, it.tag))
        }
        component.clear()
        // Re-add: first fragment (filename) + badge + rest
        val badgeAttrs = SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor(Color(200, 100, 0), Color(255, 160, 60)))
        fragments.forEachIndexed { index, (text, attrs, tag) ->
            component.append(text, attrs, tag)
            if (index == 0) {
                component.append(" [$count]", badgeAttrs)
            }
        }
        // If there were no fragments at all, just append at the end
        if (fragments.isEmpty()) {
            component.append(" [$count]", badgeAttrs)
        }
    }

    private fun resolveFilePath(node: ChangesBrowserNode<*>): String? {
        val userObject = node.userObject
        if (userObject is com.intellij.openapi.vcs.changes.Change) {
            val file = (userObject.afterRevision ?: userObject.beforeRevision)?.file ?: return null
            val basePath = project.basePath ?: return null
            val path = file.path
            return if (path.startsWith(basePath)) path.removePrefix(basePath).removePrefix("/") else path
        }
        return null
    }

    private fun findSimpleColoredComponent(component: Component): SimpleColoredComponent? {
        if (component is SimpleColoredComponent) return component
        if (component is java.awt.Container) {
            for (child in component.components) {
                val result = findSimpleColoredComponent(child)
                if (result != null) return result
            }
        }
        return null
    }
}
