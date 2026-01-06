package com.github.tomo2321.androidscreenshotplugin

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "ScreenshotSettings",
    storages = [Storage("screenshotSettings.xml")]
)
@Service(Service.Level.PROJECT)
class ScreenshotSettings : PersistentStateComponent<ScreenshotSettings> {
    var saveDirectoryPath: String? = null

    override fun getState(): ScreenshotSettings {
        return this
    }

    override fun loadState(state: ScreenshotSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(project: Project): ScreenshotSettings {
            return project.getService(ScreenshotSettings::class.java)
        }
    }
}
