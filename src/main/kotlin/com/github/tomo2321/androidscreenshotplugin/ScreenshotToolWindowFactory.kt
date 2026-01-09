package com.github.tomo2321.androidscreenshotplugin

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
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
    private var connectedDevice: IDevice? = null
    private var bridge: AndroidDebugBridge? = null
    private val settings = ScreenshotSettings.getInstance(project)
    private val logger = Logger.getInstance(ScreenshotToolWindowContent::class.java)

    companion object {
        private const val STATUS_SEARCHING = "Device: Searching..."
        private const val STATUS_NOT_FOUND = "Device: Not found"
        private const val STATUS_ADB_NOT_FOUND = "Device: adb not found"
        private const val STATUS_CONNECTED = "Device: %s (Connected)"
        private const val STATUS_ERROR = "Device: Error - %s"
    }

    private val statusLabel = JLabel(STATUS_SEARCHING)

    init {
        val mainPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0

        // Status label and reload button section
        val statusPanel = JPanel(BorderLayout(5, 0))
        statusLabel.horizontalAlignment = SwingConstants.CENTER
        val reloadButton = JButton("Reload")
        reloadButton.addActionListener {
            checkDevices()
        }
        statusPanel.add(statusLabel, BorderLayout.CENTER)
        statusPanel.add(reloadButton, BorderLayout.EAST)
        mainPanel.add(statusPanel, gbc)

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

        // Load saved directory from settings
        loadSavedDirectory()

        // Initialize: check for connected devices
        checkDevices()
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
            // Save to settings
            settings.saveDirectoryPath = saveDirectory?.absolutePath
        }
    }

    private fun loadSavedDirectory() {
        val savedPath = settings.saveDirectoryPath
        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists() && file.isDirectory) {
                saveDirectory = file
                directoryLabel.text = "Save Location: ${saveDirectory?.absolutePath}"
            } else {
                // If saved directory no longer exists, clear it from settings
                settings.saveDirectoryPath = null
            }
        }
    }

    private fun checkDevices() {
        // Reset status immediately
        SwingUtilities.invokeLater {
            statusLabel.text = STATUS_SEARCHING
            connectedDevice = null
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Initialize AndroidDebugBridge if not already done
                if (bridge == null || !bridge!!.isConnected) {
                    val adbPath = getAdbPath()
                    if (adbPath == null) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = STATUS_ADB_NOT_FOUND
                            connectedDevice = null
                        }
                        return@executeOnPooledThread
                    }

                    // Initialize ADB using Android Studio's internal API
                    @Suppress("DEPRECATION")
                    AndroidDebugBridge.initIfNeeded(false)
                    @Suppress("DEPRECATION")
                    bridge = AndroidDebugBridge.createBridge(adbPath, false)

                    // Wait for bridge to connect
                    var timeout = 5000L // 5 seconds
                    val sleepTime = 100L
                    while (!bridge!!.isConnected && timeout > 0) {
                        Thread.sleep(sleepTime)
                        timeout -= sleepTime
                    }

                    if (!bridge!!.isConnected) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = STATUS_ADB_NOT_FOUND
                            connectedDevice = null
                        }
                        return@executeOnPooledThread
                    }

                    // Wait for device list
                    timeout = 5000L
                    while (!bridge!!.hasInitialDeviceList() && timeout > 0) {
                        Thread.sleep(sleepTime)
                        timeout -= sleepTime
                    }
                }

                val devices = bridge?.devices ?: emptyArray()

                SwingUtilities.invokeLater {
                    if (devices.isNotEmpty()) {
                        connectedDevice = devices[0]
                        val deviceName = connectedDevice?.name ?: connectedDevice?.serialNumber ?: "Unknown"
                        statusLabel.text = STATUS_CONNECTED.format(deviceName)
                    } else {
                        connectedDevice = null
                        statusLabel.text = STATUS_NOT_FOUND
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error checking for devices", e)
                SwingUtilities.invokeLater {
                    connectedDevice = null
                    statusLabel.text = STATUS_ERROR.format(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun getAdbPath(): String? {
        // Try to use Android Studio's SDK utilities first
        try {
            @Suppress("DEPRECATION")
            val adb = AndroidSdkUtils.getAdb(project)
            if (adb != null && adb.exists()) {
                logger.info("Using ADB from Android Studio SDK: ${adb.absolutePath}")
                return adb.absolutePath
            }
        } catch (e: Exception) {
            logger.info("Failed to get ADB from Android Studio SDK", e)
        }

        // Fall back to environment variables and common locations
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adbFile = File(androidHome, "platform-tools/adb")
            if (adbFile.exists()) {
                return adbFile.absolutePath
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
            val path = process.inputStream.bufferedReader().readLine()
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

        if (connectedDevice == null) {
            checkDevices()
            Thread.sleep(1000) // Give time for device detection
            if (connectedDevice == null) {
                JOptionPane.showMessageDialog(
                    contentPanel,
                    "No running device found.\nPlease start a device and try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val device = connectedDevice
                if (device == null) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            contentPanel,
                            "No device connected.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    return@executeOnPooledThread
                }

                // Use Android Studio's internal API to capture screenshot via shell command
                // Android Studio uses executeShellCommand with screencap
                logger.info("Capturing screenshot from device: ${device.serialNumber}")

                val pngData = try {
                    captureScreenshotViaShell(device)
                } catch (e: Exception) {
                    logger.warn("Failed to capture screenshot: ${e.message}", e)
                    null
                }

                if (pngData == null || pngData.isEmpty()) {
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            contentPanel,
                            "Failed to capture screenshot from device.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                    return@executeOnPooledThread
                }

                // Generate filename with timestamp
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
                val timestamp = dateFormat.format(Date())
                val fileName = "$timestamp.png"
                val localFile = File(saveDirectory, fileName)

                // Save the PNG data directly to file
                localFile.writeBytes(pngData)

                logger.info("Screenshot saved successfully to: ${localFile.absolutePath}")

                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "Screenshot saved successfully:\n${localFile.absolutePath}",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            } catch (e: Exception) {
                logger.warn("Error taking screenshot", e)
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "An error occurred: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }
    }

    private fun captureScreenshotViaShell(device: IDevice): ByteArray? {
        // Use shell command to capture screenshot, same as Android Studio
        val outputReceiver = ByteArrayOutputReceiver()

        try {
            // Execute screencap command and get raw PNG data
            device.executeShellCommand("screencap -p", outputReceiver, 10, java.util.concurrent.TimeUnit.SECONDS)
            val pngData = outputReceiver.getData()

            if (pngData.isEmpty()) {
                logger.warn("Screenshot capture returned empty data")
                return null
            }

            logger.info("Captured ${pngData.size} bytes of screenshot data")
            return pngData
        } catch (e: Exception) {
            logger.warn("Failed to execute screencap command", e)
            throw e
        }
    }

    private class ByteArrayOutputReceiver : com.android.ddmlib.IShellOutputReceiver {
        private val output = java.io.ByteArrayOutputStream()
        private var cancelled = false

        override fun addOutput(data: ByteArray, offset: Int, length: Int) {
            if (!cancelled) {
                output.write(data, offset, length)
            }
        }

        override fun flush() {
            // Nothing to flush
        }

        override fun isCancelled(): Boolean = cancelled

        fun cancel() {
            cancelled = true
        }

        fun getData(): ByteArray = output.toByteArray()
    }
}
