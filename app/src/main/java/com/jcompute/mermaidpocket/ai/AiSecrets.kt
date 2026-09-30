package com.jcompute.mermaidpocket.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * API keys in EncryptedSharedPreferences (AES-256 via Android Keystore).
 * Never write keys to the plaintext `mermaid` prefs file.
 */
class AiSecrets(context: Context) {
    private val prefs: SharedPreferences = open(context.applicationContext)

    fun openRouterKey(): String = prefs.getString(AiDefaults.KEY_OPENROUTER, "").orEmpty()

    fun setOpenRouterKey(value: String) {
        prefs.edit().putString(AiDefaults.KEY_OPENROUTER, value.trim()).apply()
    }

    fun braveSearchKey(): String = prefs.getString(AiDefaults.KEY_BRAVE, "").orEmpty()

    fun setBraveSearchKey(value: String) {
        prefs.edit().putString(AiDefaults.KEY_BRAVE, value.trim()).apply()
    }

    companion object {
        fun open(context: Context): SharedPreferences {
            return runCatching { create(context) }.getOrElse {
                context.deleteSharedPreferences(AiDefaults.SECRETS_PREFS)
                create(context)
            }
        }

        private fun create(context: Context): SharedPreferences {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            return EncryptedSharedPreferences.create(
                AiDefaults.SECRETS_PREFS,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
