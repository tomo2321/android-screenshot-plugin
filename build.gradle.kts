plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.github.tomo2321"
version = "1.1.0"

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

        // Add plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "252.*"
        }

        changeNotes = """
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
