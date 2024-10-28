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
import java.util.Locale
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
                    var output = reader.readText()
                    reader.close()

                    // ANSI format
                    output = output.replace(Regex("\u001B\\[[;\\d]*m"), "")
                    output = output.replace("Follow @igor_chubin for wttr.in updates", "")

                    // Extract the weather after the location to generate the mood text
                    val weatherCondition = extractWeatherCondition(output)

                    val mood = analyzeWeatherMood(weatherCondition)

                    ApplicationManager.getApplication().invokeLater {
                        textArea.text = "$output\n\nMood for today:\n$mood"
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

    private fun extractWeatherCondition(weatherData: String): String {
        val regex = Regex("Weather report:.*?\\n\\s*(\\w+)")
        val matchResult = regex.find(weatherData)
        return matchResult?.groupValues?.get(1)?.lowercase(Locale.getDefault()) ?: "unknown"
    }

    // Simple function to analyze the weather and generate a mood text. Disclaimer: These texts are generated with ChatGPT
    // TODO: make it random
    private fun analyzeWeatherMood(weatherCondition: String): String {
        return when (weatherCondition) {
            "sunny" -> {
                "The sun is shining bright! It's a great day for outdoor activities. Go enjoy some sunshine!"
            }

            "rain" -> {
                "Rainy and cozy! Perfect weather to grab a warm drink and enjoy a good book."
            }

            "cloudy" -> {
                "It's a bit cloudy today. Maybe a good day to stay in and work on a hobby."
            }

            "snow" -> {
                "Snowy and beautiful! Time to get warm and cozy or maybe build a snowman if you’re feeling adventurous!"
            }

            "clear" -> {
                "Clear skies! Looks like a perfect time for a stroll and some fresh air."
            }

            "fog" -> {
                "Foggy weather! Mysterious and calm, maybe take it slow and enjoy the stillness."
            }

            else -> {
                "The weather looks interesting today! Make the best out of it, no matter what it brings!"
            }
        }
    }
}
