package com.github.tomo2321.androidscreenshotplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel

class HelloWorldToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = HelloWorldToolWindowContent()
        val content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class HelloWorldToolWindowContent {
    val contentPanel: JPanel = JPanel(BorderLayout())

    init {
        val button = JButton("Click Me!")
        button.addActionListener {
            JOptionPane.showMessageDialog(contentPanel, "Hello, World!", "Message", JOptionPane.INFORMATION_MESSAGE)
        }

        contentPanel.add(button, BorderLayout.CENTER)
    }
}
