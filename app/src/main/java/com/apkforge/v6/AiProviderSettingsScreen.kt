
package com.apkforge.v6

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AiProviderSettingsScreen(
    settingsStore: AiSettingsStore
) {
    var apiKey by remember { mutableStateOf("") }
    var configured by remember {
        mutableStateOf(settingsStore.hasOpenRouterApiKey())
    }
    var message by remember {
        mutableStateOf(
            if (configured) "OpenRouter key is saved securely."
            else "Add your OpenRouter API key to enable AI generation."
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "AI Provider Manager",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                "Primary gateway: OpenRouter",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                if (configured) "API key: Configured"
                else "API key: Not configured",
                color = if (configured) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OpenRouter API key") },
                placeholder = { Text("Paste your key here") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Button(
                onClick = {
                    try {
                        settingsStore.saveOpenRouterApiKey(apiKey)
                        apiKey = ""
                        configured = true
                        message = "OpenRouter API key saved securely."
                    } catch (error: Exception) {
                        message = error.message
                            ?: "Could not save API key."
                    }
                },
                enabled = apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save API key")
            }

            OutlinedButton(
                onClick = {
                    settingsStore.clearOpenRouterApiKey()
                    apiKey = ""
                    configured = false
                    message = "OpenRouter API key removed."
                },
                enabled = configured,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove saved key")
            }

            Text(
                message,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Only configure models you are allowed to use. " +
                    "Automatic fallback will use free models unless " +
                    "you later enable paid models.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
