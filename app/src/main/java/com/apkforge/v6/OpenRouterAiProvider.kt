
package com.apkforge.v6

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterAiProvider(
    private val apiKey: String
) : AiCodeProvider {

    override val displayName = "OpenRouter Free Model Router"

    override suspend fun generate(
        request: AiGenerationRequest
    ): AiGenerationResult = withTimeout(120_000L) {

        require(apiKey.isNotBlank()) {
            "OpenRouter API key is missing. Add it in AI settings."
        }

        require(
            Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")
                .matches(request.packageName.trim())
        ) {
            "Enter a valid Android package name."
        }

        require(request.description.isNotBlank()) {
            "Describe the Android app you want to create."
        }

        withContext(Dispatchers.IO) {
            val connection = URL(
                "https://openrouter.ai/api/v1/chat/completions"
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 100_000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $apiKey"
                )
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )
                connection.setRequestProperty(
                    "HTTP-Referer",
                    "https://github.com/Destro0007/apkforge-v6"
                )
                connection.setRequestProperty(
                    "X-Title",
                    "APKForge V6"
                )

                val systemPrompt = """
                    You are an expert Android Kotlin developer.

                    Generate a small, functional Android application
                    source-code starter for the user's project.

                    Return ONLY valid JSON with this structure:
                    {
                      "summary": "Short description",
                      "files": [
                        {
                          "fileName": "MainActivity.kt",
                          "content": "Complete Kotlin source code"
                        },
                        {
                          "fileName": "README.md",
                          "content": "Project instructions"
                        }
                      ]
                    }

                    Requirements:
                    - Use the exact package name supplied by the user.
                    - Generate between 1 and 6 files.
                    - Only generate .kt and .md files.
                    - Include complete source code, not placeholders.
                    - Do not generate Gradle files, manifests or binaries.
                    - Do not use markdown code fences around the JSON.
                    - Do not include explanations outside the JSON.
                """.trimIndent()

                val userPrompt = """
                    Project name: ${request.projectName}
                    Package name: ${request.packageName}

                    App requirements:
                    ${request.description}
                """.trimIndent()

                val body = JSONObject()
                    .put("model", "openrouter/free")
                    .put(
                        "messages",
                        JSONArray()
                            .put(
                                JSONObject()
                                    .put("role", "system")
                                    .put("content", systemPrompt)
                            )
                            .put(
                                JSONObject()
                                    .put("role", "user")
                                    .put("content", userPrompt)
                            )
                    )
                    .put("temperature", 0.2)
                    .put("max_tokens", 12000)

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode

                val responseText = (
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }
                )?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (responseCode !in 200..299) {
                    val message = try {
                        JSONObject(responseText)
                            .optJSONObject("error")
                            ?.optString("message")
                    } catch (_: Exception) {
                        null
                    }

                    throw IllegalStateException(
                        "OpenRouter error ($responseCode): " +
                            (message ?: responseText.take(500))
                    )
                }

                val response = JSONObject(responseText)
                val content = response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val cleanJson = content
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val resultJson = JSONObject(cleanJson)
                val filesJson = resultJson.getJSONArray("files")

                require(filesJson.length() in 1..6) {
                    "The AI returned an invalid number of files."
                }

                val generatedFiles = buildList {
                    for (index in 0 until filesJson.length()) {
                        val item = filesJson.getJSONObject(index)
                        val fileName = item.getString("fileName")
                        val fileContent = item.getString("content")

                        require(
                            fileName.matches(
                                Regex("[A-Za-z0-9_.-]+")
                            )
                        ) {
                            "Invalid generated file name: $fileName"
                        }

                        require(
                            fileName.substringAfterLast(".") in
                                setOf("kt", "md")
                        ) {
                            "Unsupported file type: $fileName"
                        }

                        require(fileContent.isNotBlank()) {
                            "Generated file is empty: $fileName"
                        }

                        require(fileContent.length <= 100_000) {
                            "Generated file is too large: $fileName"
                        }

                        add(
                            GeneratedProjectFile(
                                fileName = fileName,
                                content = fileContent
                            )
                        )
                    }
                }

                require(
                    generatedFiles.any {
                        it.fileName == "MainActivity.kt"
                    }
                ) {
                    "The AI did not generate MainActivity.kt."
                }

                AiGenerationResult(
                    providerName = "OpenRouter (${response.optString("model", "free model")})",
                    summary = resultJson.optString(
                        "summary",
                        "Project files generated."
                    ),
                    files = generatedFiles
                )
            } finally {
                connection.disconnect()
            }
        }
    }
}
