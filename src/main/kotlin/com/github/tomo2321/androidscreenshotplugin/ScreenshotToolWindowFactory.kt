package com.github.tomo2321.androidscreenshotplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class ScreenshotToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = ScreenshotToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.contentPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class ScreenshotToolWindowContent(private val project: Project) {
    val contentPanel: JPanel = JPanel(BorderLayout())
    private var saveDirectory: File? = null
    private val directoryLabel = JLabel("Save Location: Not set")
    private val statusLabel = JLabel("Emulator: Searching...")
    private var connectedEmulator: String? = null

    init {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0

        // Status label
        statusLabel.horizontalAlignment = SwingConstants.CENTER
        mainPanel.add(statusLabel, gbc)

        // Directory selection section
        gbc.gridy++
        val dirPanel = JPanel(BorderLayout(5, 0))
        directoryLabel.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        val selectDirButton = JButton("Select Save Location")
        selectDirButton.addActionListener {
            selectDirectory()
        }
        dirPanel.add(directoryLabel, BorderLayout.CENTER)
        dirPanel.add(selectDirButton, BorderLayout.EAST)
        mainPanel.add(dirPanel, gbc)

        // Screenshot button
        gbc.gridy++
        val screenshotButton = JButton("Take Screenshot")
        screenshotButton.addActionListener {
            takeScreenshot()
        }
        mainPanel.add(screenshotButton, gbc)

        contentPanel.add(mainPanel, BorderLayout.NORTH)

        // Initialize: check for connected emulators
        checkEmulators()
    }

    private fun selectDirectory() {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.dialogTitle = "Select Screenshot Save Location"

        if (saveDirectory != null) {
            fileChooser.currentDirectory = saveDirectory
        }

        val result = fileChooser.showOpenDialog(contentPanel)
        if (result == JFileChooser.APPROVE_OPTION) {
            saveDirectory = fileChooser.selectedFile
            directoryLabel.text = "Save Location: ${saveDirectory?.absolutePath}"
        }
    }

    private fun checkEmulators() {
        Thread {
            try {
                val adbPath = findAdb()
                if (adbPath == null) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "Emulator: adb not found"
                    }
                    return@Thread
                }

                val process = Runtime.getRuntime().exec(arrayOf(adbPath, "devices"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val devices = mutableListOf<String>()

                reader.forEachLine { line ->
                    if (line.contains("\t") && line.contains("device")) {
                        val deviceId = line.split("\t")[0].trim()
                        devices.add(deviceId)
                    }
                }
                process.waitFor()

                SwingUtilities.invokeLater {
                    if (devices.isNotEmpty()) {
                        connectedEmulator = devices[0]
                        statusLabel.text = "Emulator: ${connectedEmulator} (Connected)"
                    } else {
                        statusLabel.text = "Emulator: No running devices found"
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    statusLabel.text = "Emulator: Error - ${e.message}"
                }
            }
        }.start()
    }

    private fun findAdb(): String? {
        // Check ANDROID_HOME environment variable
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbPath = File(androidHome, "platform-tools/adb")
            if (adbPath.exists()) {
                return adbPath.absolutePath
            }
        }

        // Check common locations
        val commonPaths = listOf(
            System.getProperty("user.home") + "/Library/Android/sdk/platform-tools/adb",
            "/usr/local/bin/adb",
            "/opt/homebrew/bin/adb"
        )

        for (path in commonPaths) {
            val file = File(path)
            if (file.exists()) {
                return file.absolutePath
            }
        }

        // Try to find adb in PATH
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "adb"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val path = reader.readLine()
            process.waitFor()
            if (path != null && path.isNotEmpty()) {
                return path.trim()
            }
        } catch (e: Exception) {
            // Ignore
        }

        return null
    }

    private fun takeScreenshot() {
        if (saveDirectory == null) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Please select a save directory first.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        if (connectedEmulator == null) {
            checkEmulators()
            if (connectedEmulator == null) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "No running emulator found.\nPlease start an emulator and try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }
        }

        Thread {
            try {
                val adbPath = findAdb()
                if (adbPath == null) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            contentPanel,
                            "adb not found. Please ensure Android SDK is installed.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    return@Thread
                }

                // Generate filename with timestamp
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
                val timestamp = dateFormat.format(Date())
                val fileName = "$timestamp.png"
                val localFile = File(saveDirectory, fileName)

                // Take screenshot on device
                val devicePath = "/sdcard/screenshot.png"

                // Execute screencap command
                val screencapProcess = Runtime.getRuntime().exec(
                    arrayOf(adbPath, "-s", connectedEmulator, "shell", "screencap", "-p", devicePath)
                )
                screencapProcess.waitFor()

                if (screencapProcess.exitValue() != 0) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            contentPanel,
                            "Failed to capture screenshot.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    return@Thread
                }

                // Pull the screenshot to local machine
                val pullProcess = Runtime.getRuntime().exec(
                    arrayOf(adbPath, "-s", connectedEmulator, "pull", devicePath, localFile.absolutePath)
                )
                pullProcess.waitFor()

                if (pullProcess.exitValue() != 0) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            contentPanel,
                            "Failed to retrieve screenshot.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    return@Thread
                }

                // Clean up device screenshot
                Runtime.getRuntime().exec(
                    arrayOf(adbPath, "-s", connectedEmulator, "shell", "rm", devicePath)
                )

                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "Screenshot saved successfully:\n${localFile.absolutePath}",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "An error occurred: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.start()
    }
}
