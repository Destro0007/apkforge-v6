
package com.apkforge.v6

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * Real Gemini provider using Firebase AI Logic.
 * No manually supplied API key is required.
 */
class GeminiAiProvider : AiCodeProvider {

    override val displayName: String =
        "Gemini 3.8 Flash (Firebase AI Logic)"

    override suspend fun generate(
        request: AiGenerationRequest
    ): AiGenerationResult = withTimeout(90_000L) {

        val safePackage = request.packageName.trim()

        require(PACKAGE_PATTERN.matches(safePackage)) {
            "Enter a valid Android package name."
        }

        require(request.description.isNotBlank()) {
            "Describe the Android app you want to create."
        }

        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")

        val prompt = """
            You are an expert Android Kotlin application developer.

            Create a working Android application starter for APKForge V6.

            PROJECT NAME: ${request.projectName}
            PACKAGE NAME: $safePackage
            USER REQUIREMENTS:
            ${request.description}

            REQUIREMENTS:
            1. Generate application-specific Kotlin code.
            2. MainActivity.kt must use package $safePackage.
            3. Use Android SDK and standard AndroidX Activity APIs.
            4. Create a useful interface matching the user's requirements.
            5. Do not include API keys, passwords or secrets.
            6. Do not use arbitrary file paths or external downloads.
            7. Include a README.md explaining the generated application.
            8. Do not generate Gradle files, manifests or binary files.
            9. Keep the code self-contained and readable.

            RETURN ONLY VALID JSON.
            Do not use Markdown code fences.

            Required JSON structure:
            {
              "summary": "Short description of the generated application",
              "files": [
                {
                  "fileName": "MainActivity.kt",
                  "content": "Complete Kotlin source code"
                },
                {
                  "fileName": "README.md",
                  "content": "Application documentation"
                }
              ]
            }
        """.trimIndent()

        val response = model.generateContent(prompt)
        val rawText = response.text?.trim().orEmpty()

        require(rawText.isNotBlank()) {
            "Gemini returned an empty response."
        }

        val jsonStart = rawText.indexOf('{')
        val jsonEnd = rawText.lastIndexOf('}')

        require(jsonStart >= 0 && jsonEnd > jsonStart) {
            "Gemini did not return valid JSON."
        }

        val resultJson = JSONObject(
            rawText.substring(jsonStart, jsonEnd + 1)
        )

        val summary = resultJson.optString(
            "summary",
            "Generated using Gemini."
        )

        val fileArray = resultJson.optJSONArray("files")
            ?: error("Gemini response did not contain a files list.")

        require(fileArray.length() in 1..6) {
            "Gemini returned an invalid number of files."
        }

        val generatedFiles = mutableListOf<GeneratedProjectFile>()
        val usedNames = mutableSetOf<String>()

        for (index in 0 until fileArray.length()) {
            val item = fileArray.getJSONObject(index)

            val fileName = item.optString("fileName").trim()
            val content = item.optString("content")

            require(FILE_PATTERN.matches(fileName)) {
                "Gemini returned an unsafe filename."
            }

            require(
                fileName.endsWith(".kt") ||
                    fileName.endsWith(".md")
            ) {
                "Only Kotlin and Markdown files are allowed."
            }

            require(usedNames.add(fileName)) {
                "Gemini returned a duplicate filename."
            }

            require(content.isNotBlank() && content.length <= 100_000) {
                "Generated file is empty or too large."
            }

            generatedFiles.add(
                GeneratedProjectFile(
                    fileName = fileName,
                    content = content
                )
            )
        }

        val mainActivity = generatedFiles.firstOrNull {
            it.fileName == "MainActivity.kt"
        } ?: error("Gemini did not generate MainActivity.kt.")

        val packageDeclaration = Regex(
            "(?m)^\\s*package\\s+${Regex.escape(safePackage)}\\s*$"
        )

        require(packageDeclaration.containsMatchIn(mainActivity.content)) {
            "Generated MainActivity has an incorrect package declaration."
        }

        AiGenerationResult(
            providerName = displayName,
            summary = summary,
            files = generatedFiles
        )
    }

    private companion object {
        val PACKAGE_PATTERN = Regex(
            "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+"
        )

        val FILE_PATTERN = Regex(
            "[A-Za-z0-9_-][A-Za-z0-9_.-]*"
        )
    }
}
