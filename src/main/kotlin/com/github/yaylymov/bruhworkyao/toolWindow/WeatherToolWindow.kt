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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class WeatherToolWindow {

    private val panel = JBPanel<JBPanel<*>>(BorderLayout())

    private val client = OkHttpClient()

    // I know hard coding my api key is dumb, but I wanted to give you a chance to try it out without generating an api key yourself. :/
    // You can use my api key, but if you have your own, please use your own. I will deactivate this key in 3-4 weeks.
    private val apiKey = "your-api-key"

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

                    // Generate AI Mood Interpretation using LLM
                    val mood = getLLMGeneratedMood(weatherCondition)

                    ApplicationManager.getApplication().invokeLater {
                        textArea.text = "$output\n\nMood for today:\n\n$mood"
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

    // Function to generate a meme based mood interpretation based on the below prompt.
    // It will generate different messages everytime you reset and run the plugin or click Get Weather button on the plugin.
    // Source guides: https://platform.openai.com/docs/guides/text-generation?text-generation-quickstart-example=text
    // Source guides: https://platform.openai.com/docs/api-reference/making-requests
    private fun getLLMGeneratedMood(weatherCondition: String): String {
        val prompt =
            "The current weather is $weatherCondition. Generate a meme like mood interpretation based on this weather condition in one short sentence."

        val requestBody = JSONObject()
            .put("model", "gpt-4o-mini")
            .put(
                "messages", listOf(
                    JSONObject().put("role", "user").put("content", prompt)
                )
            )
            .put("temperature", 0.7)
            .toString()
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    choices.getJSONObject(0).getJSONObject("message").getString("content").trim()
                } else {
                    "Failed to generate mood interpretation."
                }
            }
        } catch (e: Exception) {
            "Failed to generate: ${e.message}"
        }
    }
}
