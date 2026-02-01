# Android Screenshot Plugin

Android Studio plugin that captures screenshots from running Android emulators and devices.

## Features

- **Automatic Device Detection**: Automatically detects and connects to running Android devices and emulators via ADB
- **Manual Device Refresh**: Use the reload button to manually search for connected devices
- **Advanced Screenshot Capture**: 
  - Uses gRPC via EmulatorController for fast and reliable emulator screenshots
  - Multiple fallback methods (gRPC → RawImage API → File-based) ensure successful capture
  - Automatically selects optimal capture method based on device type (emulator vs physical device)
- **Device Name Display**: Shows the name of the connected device in the status label
- **Automatic File Saving**: Automatically saves to specified directory with timestamp format (yyyyMMdd_HHmmss.png)
- **Save Location Configuration**: Easily set save directory from the tool window
- **Persistent Save Location**: Remembers your selected save directory across plugin restarts (per project)
- **Robust ADB Path Resolution**: Multiple fallback methods to locate adb binary automatically
- **Comprehensive Error Handling**: Clear error messages and automatic fallback mechanisms

## Usage

1. Launch Android Studio and install the plugin
2. Start an Android emulator or connect a physical device via USB (ensure USB debugging is enabled)
3. Open "Android Screenshot" from the right-side tool window
4. The plugin automatically searches for connected devices on startup
   - Device name and connection status are displayed in the status label
   - If device is not detected, click the "Reload" button to refresh the device list
5. Click "Select Save Location" button to specify where screenshots will be saved
   - The location is saved per project and persists across IDE restarts
6. Click "Take Screenshot" button to capture the device screen
   - For emulators: uses fast gRPC-based capture via EmulatorController
   - For physical devices: uses standard ADB-based capture methods
   - Automatic fallback ensures capture success even if primary method fails
7. Screenshots are automatically saved to the specified directory with timestamp (yyyyMMdd_HHmmss.png)

## Requirements

- Android Studio 2025.2.2.1 or later
- Android SDK with ADB (Android Debug Bridge) installed
- Running Android emulator or physical device with USB debugging enabled
- For physical devices: USB connection with proper device drivers installed

## Technical Details

### Screenshot Capture Methods

The plugin implements a three-tier fallback strategy to ensure reliable screenshot capture:

1. **EmulatorController gRPC API** (Emulators only)
   - Fastest and most reliable method for emulators
   - Uses Android Studio's internal gRPC API
   - Captures PNG images directly without intermediate conversion
   - Falls back to method 2 if EmulatorController is unavailable

2. **RawImage API via DDMLib**
   - Works for both emulators and physical devices
   - Uses Android's `ddmlib` library to capture raw image data
   - Converts raw pixel data to PNG format
   - Falls back to method 3 if this method fails

3. **File-based Capture via ADB Shell**
   - Most compatible method, works on all devices
   - Uses `screencap` command to save screenshot on device
   - Pulls the file from device to local machine
   - Slowest but most reliable fallback option

### ADB Path Resolution

The plugin automatically locates the ADB binary using multiple methods:

1. Android Studio's SDK utilities (`AndroidSdkUtils.getAdb()`)
2. Environment variables (`ANDROID_HOME`, `ANDROID_SDK_ROOT`)
3. Common installation paths (macOS, Linux, Windows)
4. System PATH lookup

### Project Structure

- `ScreenshotToolWindowFactory.kt`: Main plugin logic and UI
- `ScreenshotSettings.kt`: Persistent storage for save directory path
- `plugin.xml`: Plugin configuration and metadata

## Development

### Build

```bash
./gradlew build
```

### Build Plugin Distribution

```bash
./gradlew buildPlugin
```

This task builds the plugin distribution file (ZIP) that can be installed in Android Studio or published to JetBrains Marketplace. The distribution file will be created in `build/distributions/`.

### Run Plugin

```bash
./gradlew runIde
```

## Plugin template structure

A generated project contains the following content structure:

```
.
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── gradle
│   ├── wrapper/            Gradle Wrapper
├── src                     Plugin sources
│   ├── main
│   │   ├── kotlin/         Kotlin production sources
│   │   └── resources/      Resources - plugin.xml, icons, messages
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Gradle build configuration
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation
and the manifest for our plugin – [plugin.xml][file:plugin.xml].

> [!NOTE]
> To use Java in your plugin, create the `/src/main/java` directory.

## Plugin configuration file

The plugin configuration file is a [plugin.xml][file:plugin.xml] file located in the `src/main/resources/META-INF`
directory.
It provides general information about the plugin, its dependencies, extensions, and listeners.

You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

If you're still not quite sure what this is all about, read our
introduction: [What is the IntelliJ Platform?][docs:intro]

$H$H Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name | Description                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin         | Runs [`:runIde`][gh:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests          | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                 |
| Run Verifications  | Runs [`:verifyPlugin`][gh:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and
> required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses
the `publishPlugin` Gradle task provided by
the [intellij-platform-gradle-plugin][gh:intellij-platform-gradle-plugin-docs].

You can also upload the plugin to the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/upload)
manually via UI.

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][gh:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]
- [JetBrains Marketplace Paid Plugins][jb:paid-plugins]
- [IntelliJ SDK Code Samples][gh:code-samples]

[docs]: https://plugins.jetbrains.com/docs/intellij

[docs:intro]: https://plugins.jetbrains.com/docs/intellij/intellij-platform.html?from=IJPluginTemplate

[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate

[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin

[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

[gh:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde

[gh:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md

[jb:forum]: https://platform.jetbrains.com/

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui