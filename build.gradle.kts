plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.10.5"
}

group = "com.github.tomo2321"
version = "1.4.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        androidStudio("2025.2.2.1")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Plugin dependencies for compilation
        bundledPlugins("org.jetbrains.android")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "252.*"
        }

        changeNotes = """
            <h2>1.4.1</h2>
            <ul>
                <li>Updated Kotlin version to 2.3.0</li>
                <li>Updated IntelliJ Platform Gradle Plugin version to 2.10.5</li>
            </ul>
            <h2>1.4.0</h2>
            <ul>
                <li>Improved screenshot capture: now captures screens correctly even for contents that would appear black with standard methods, by using gRPC via EmulatorController</li>
                <li>Enhanced emulator screenshot capture: integrated EmulatorController gRPC API for faster and more reliable screenshot capture from emulators</li>
                <li>Improved screenshot reliability: implemented multiple fallback methods (gRPC, RawImage API, and file-based) to ensure successful screenshot capture</li>
                <li>Better device type handling: added automatic detection to use optimal capture method for emulators vs physical devices</li>
                <li>Enhanced error handling: added comprehensive logging and fallback mechanisms for screenshot capture failures</li>
            </ul>
            <h2>1.3.0</h2>
            <ul>
                <li>Added device name display: connected device names are now shown in the status label</li>
                <li>Improved device detection: implemented AndroidDebugBridge integration for more reliable device connection</li>
                <li>Enhanced ADB path resolution: added multiple fallback methods to locate adb binary</li>
                <li>Better error handling: clearer error messages for connection and ADB-related issues</li>
            </ul>
            <h2>1.2.0</h2>
            <ul>
                <li>Added persistent save location: the selected save directory is now remembered across plugin restarts (per project)</li>
                <li>Improved user experience: no need to select save directory every time you restart the IDE</li>
            </ul>
            <h2>1.1.0</h2>
            <ul>
                <li>Added support for physical Android devices in addition to emulators</li>
                <li>Added manual reload button to refresh device connection status</li>
                <li>Improved device detection: properly resets connection state when no devices found</li>
                <li>Enhanced status messages with standardized constants for better maintainability</li>
                <li>Changed UI labels from "Emulator" to "Device" to reflect broader device support</li>
            </ul>
            <h2>1.0.0</h2>
            <ul>
                <li>Added Android emulator screenshot capture functionality</li>
                <li>Automatic detection and connection to running emulators</li>
                <li>Screenshot save location configuration</li>
                <li>Automatic timestamped file naming (yyyyMMdd_HHmmss.png)</li>
            </ul>
            <h2>0.1.0</h2>
            <ul>
                <li>Initial release</li>
                <li>Added Tool Window on the right side</li>
                <li>Click button to show "Hello, World!" message</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
