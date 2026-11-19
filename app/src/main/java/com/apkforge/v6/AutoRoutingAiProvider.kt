
package com.apkforge.v6

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

class AutoRoutingAiProvider(
    private val settingsStore: AiSettingsStore,
    private val openRouterProvider: (String) -> AiCodeProvider = {
        OpenRouterAiProvider(it)
    },
    private val geminiProvider: AiCodeProvider = GeminiAiProvider(),
    private val offlineProvider: AiCodeProvider = OfflineTemplateProvider()
) : AiCodeProvider {

    override val displayName =
        "Automatic AI Router: OpenRouter → Gemini → Offline"

    override suspend fun generate(
        request: AiGenerationRequest
    ): AiGenerationResult {

        val failures = mutableListOf<String>()

        // First attempt: OpenRouter free-model router.
        val apiKey = try {
            settingsStore.getOpenRouterApiKey()
        } catch (error: Exception) {
            failures.add(
                "Could not read OpenRouter settings: ${error.message}"
            )
            null
        }

        if (!apiKey.isNullOrBlank()) {
            try {
                return openRouterProvider(apiKey).generate(request)
            } catch (error: TimeoutCancellationException) {
                failures.add("OpenRouter timed out.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures.add(
                    "OpenRouter failed: ${error.message ?: "Unknown error"}"
                )
            }
        } else {
            failures.add("OpenRouter API key is not configured.")
        }

        // Second attempt: existing Gemini provider.
        try {
            return geminiProvider.generate(request)
        } catch (error: TimeoutCancellationException) {
            failures.add("Gemini timed out.")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failures.add(
                "Gemini failed: ${error.message ?: "Unknown error"}"
            )
        }

        // Final attempt: existing offline generator.
        val offlineResult = offlineProvider.generate(request)

        return offlineResult.copy(
            summary = offlineResult.summary +
                "\n\nAutomatic fallback was used because: " +
                failures.joinToString(" | ")
        )
    }
}
