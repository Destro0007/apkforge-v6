
package com.apkforge.v6

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AiSettingsStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "apkforge_ai_secure_settings",
        Context.MODE_PRIVATE
    )

    private val keyAlias = "apkforge_ai_api_key_encryption"

    fun saveOpenRouterApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) {
            "API key cannot be empty."
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val encrypted = cipher.doFinal(
            apiKey.trim().toByteArray(Charsets.UTF_8)
        )

        preferences.edit()
            .putString("openrouter_iv", Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP
            ))
            .putString("openrouter_encrypted_key", Base64.encodeToString(
                encrypted,
                Base64.NO_WRAP
            ))
            .apply()
    }

    fun getOpenRouterApiKey(): String? {
        val ivText = preferences.getString("openrouter_iv", null)
            ?: return null

        val encryptedText = preferences.getString(
            "openrouter_encrypted_key",
            null
        ) ?: return null

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val iv = Base64.decode(ivText, Base64.NO_WRAP)
            val encrypted = Base64.decode(
                encryptedText,
                Base64.NO_WRAP
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            null
        }
    }

    fun hasOpenRouterApiKey(): Boolean {
        return getOpenRouterApiKey()?.isNotBlank() == true
    }

    fun clearOpenRouterApiKey() {
        preferences.edit()
            .remove("openrouter_iv")
            .remove("openrouter_encrypted_key")
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(keyAlias, null)

        if (existingKey is SecretKey) {
            return existingKey
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }
}
