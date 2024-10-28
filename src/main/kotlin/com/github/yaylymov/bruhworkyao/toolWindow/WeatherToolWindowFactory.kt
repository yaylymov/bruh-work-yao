package com.github.yaylymov.bruhworkyao.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class WeatherToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val weatherToolWindow = WeatherToolWindow()
        val content = ContentFactory.getInstance().createContent(weatherToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

}
