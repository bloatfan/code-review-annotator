package com.codereview.decoration

import com.codereview.service.ReviewCommentListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.changes.ui.ChangesTree
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import java.awt.Component
import java.awt.Container
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

class ChangesTreeDecoratorInstaller : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.messageBus.connect().subscribe(ReviewCommentListener.TOPIC, object : ReviewCommentListener {
            override fun commentsChanged() {
                ApplicationManager.getApplication().invokeLater {
                    repaintChangesTrees(project)
                }
            }
        })

        ApplicationManager.getApplication().invokeLater {
            installOnCommitWindow(project)
        }

        project.messageBus.connect().subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
            override fun toolWindowShown(toolWindow: ToolWindow) {
                if (toolWindow.id == "Commit") {
                    ApplicationManager.getApplication().invokeLater {
                        installOnCommitWindow(project)
                    }
                }
            }
        })
    }

    private fun installOnCommitWindow(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Commit") ?: return
        val contentComponent = toolWindow.contentManager.component
        wrapChangesTreesIn(contentComponent, project)
    }

    private fun wrapChangesTreesIn(component: Component, project: Project) {
        if (component is ChangesTree) {
            wrapRenderer(component, project)
        }
        if (component is Container) {
            for (child in component.components) {
                wrapChangesTreesIn(child, project)
            }
        }
    }

    private fun wrapRenderer(tree: ChangesTree, project: Project) {
        val current = tree.cellRenderer
        if (current is CommentCountDecorator) return
        tree.cellRenderer = CommentCountDecorator(current, project)

        tree.addPropertyChangeListener("cellRenderer", object : PropertyChangeListener {
            override fun propertyChange(evt: PropertyChangeEvent) {
                if (evt.newValue !is CommentCountDecorator) {
                    tree.removePropertyChangeListener("cellRenderer", this)
                    wrapRenderer(tree, project)
                }
            }
        })
    }

    private fun repaintChangesTrees(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Commit") ?: return
        repaintIn(toolWindow.contentManager.component)
    }

    private fun repaintIn(component: Component) {
        if (component is ChangesTree) {
            component.repaint()
        }
        if (component is Container) {
            for (child in component.components) {
                repaintIn(child)
            }
        }
    }
}
