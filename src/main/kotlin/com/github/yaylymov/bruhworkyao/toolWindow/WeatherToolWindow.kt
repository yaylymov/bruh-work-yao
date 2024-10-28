package com.github.yaylymov.bruhworkyao.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JTextArea
import java.io.BufferedReader
import java.io.InputStreamReader
import java.awt.Font
import javax.swing.JPanel

class WeatherToolWindow {

    private val panel = JBPanel<JBPanel<*>>(BorderLayout())

    init {
        val textArea = JTextArea(20, 50)
        textArea.isEditable = false
        textArea.font = Font("Monospaced", Font.PLAIN, 12)
        val scrollPane = JBScrollPane(textArea)

        val refreshButton = JButton("Get Weather")
        refreshButton.addActionListener {
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val process = Runtime.getRuntime().exec("curl wttr.in/?T")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val output = reader.readText()
                    reader.close()

                    val cleanOutput = output.replace(Regex("\u001B\\[[;\\d]*m"), "")

                    ApplicationManager.getApplication().invokeLater {
                        textArea.text = cleanOutput
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog("Failed to retrieve weather information: ${e.message}", "Error")
                    }
                }
            }
        }

        val resetButton = JButton("Reset")
        resetButton.addActionListener {
            textArea.text = ""
        }

        val buttonPanel = JPanel()
        buttonPanel.add(refreshButton)
        buttonPanel.add(resetButton)

        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
    }

    fun getContent() = panel
}