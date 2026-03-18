package com.codereview.editor

import com.codereview.model.CommentType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import javax.swing.*

class InlineCommentEditorPanel(
    initialType: CommentType = CommentType.ISSUE,
    initialText: String = "",
    private val onSave: (CommentType, String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout(0, 4)) {

    private val typeCombo = ComboBox(CommentType.entries.toTypedArray()).apply {
        selectedItem = initialType
    }
    val textArea: JTextArea = JTextArea(initialText, 4, 60).apply {
        lineWrap = true
        wrapStyleWord = true
        font = UIManager.getFont("EditorPane.font") ?: font
    }

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, accentColorFor(initialType)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        )
        background = JBColor(Color(245, 245, 250), Color(45, 45, 50))

        // Top bar: type combo + buttons
        val topBar = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            val typePanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                isOpaque = false
                add(JLabel("Type:"))
                add(typeCombo)
            }
            add(typePanel, BorderLayout.WEST)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                isOpaque = false
                val saveBtn = JButton("Save").apply {
                    addActionListener { save() }
                }
                val cancelBtn = JButton("Cancel").apply {
                    addActionListener { cancel() }
                }
                add(saveBtn)
                add(cancelBtn)
            }
            add(buttonPanel, BorderLayout.EAST)
        }
        add(topBar, BorderLayout.NORTH)

        val scrollPane = JBScrollPane(textArea).apply {
            preferredSize = Dimension(0, 80)
        }
        add(scrollPane, BorderLayout.CENTER)

        val hintLabel = JLabel("Ctrl+Enter to save, Escape to cancel").apply {
            foreground = JBColor(Color(140, 140, 140), Color(120, 120, 120))
            font = font.deriveFont(font.size2D - 1)
            border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
        }
        add(hintLabel, BorderLayout.SOUTH)

        // Update accent bar color on type change
        typeCombo.addActionListener {
            val type = typeCombo.selectedItem as CommentType
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accentColorFor(type)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            )
        }

        // Escape to cancel
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("ESCAPE"), "cancel-edit"
        )
        textArea.actionMap.put("cancel-edit", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) { cancel() }
        })

        // Ctrl+Enter to save
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke("ctrl ENTER"), "save-edit"
        )
        textArea.actionMap.put("save-edit", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) { save() }
        })
    }

    private fun save() {
        val text = textArea.text.trim()
        if (text.isNotEmpty()) {
            onSave(typeCombo.selectedItem as CommentType, text)
        }
    }

    private fun cancel() {
        onCancel()
    }

    companion object {
        fun accentColorFor(type: CommentType): Color = when (type) {
            CommentType.ISSUE -> JBColor(Color(220, 50, 50), Color(220, 80, 80))
            CommentType.SUGGESTION -> JBColor(Color(50, 100, 220), Color(80, 130, 255))
            CommentType.NOTE -> JBColor(Color(200, 160, 0), Color(220, 180, 50))
        }
    }
}
