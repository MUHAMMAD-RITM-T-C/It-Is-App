package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class AiApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ai_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_PROVIDER = "active_ai_provider"
        private const val KEY_CUSTOM_GEMINI_KEY = "custom_gemini_api_key"
        private const val KEY_SELECTED_GEMINI_MODEL = "selected_gemini_model"
        const val DEFAULT_MODEL = "deepseek-chat"
    }

    fun getActiveProvider(): AiProvider {
        val id = prefs.getString(KEY_ACTIVE_PROVIDER, AiProvider.DEEPSEEK.id) ?: AiProvider.DEEPSEEK.id
        return AiProvider.fromId(id)
    }

    fun setActiveProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_ACTIVE_PROVIDER, provider.id).apply()
    }

    fun getApiKey(provider: AiProvider = getActiveProvider()): String {
        val key = prefs.getString(provider.keyPrefKey, "")?.trim().orEmpty()
        if (key.isNotBlank()) {
            return key
        }

        // Backward compatibility for Gemini
        if (provider == AiProvider.GEMINI) {
            val legacy = prefs.getString(KEY_CUSTOM_GEMINI_KEY, "")?.trim().orEmpty()
            if (legacy.isNotBlank()) return legacy

            return try {
                val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
                if (buildConfigKey.isNotBlank() && !buildConfigKey.contains("MY_GEMINI_API_KEY")) {
                    buildConfigKey
                } else {
                    ""
                }
            } catch (_: Throwable) {
                ""
            }
        }

        return ""
    }

    fun saveApiKey(provider: AiProvider, key: String) {
        prefs.edit().putString(provider.keyPrefKey, key.trim()).apply()
        if (provider == AiProvider.GEMINI) {
            prefs.edit().putString(KEY_CUSTOM_GEMINI_KEY, key.trim()).apply()
        }
    }

    fun getSelectedModel(provider: AiProvider = getActiveProvider()): String {
        val model = prefs.getString(provider.modelPrefKey, "")?.trim().orEmpty()
        if (model.isNotBlank()) return model
        if (provider == AiProvider.GEMINI) {
            val legacy = prefs.getString(KEY_SELECTED_GEMINI_MODEL, "")?.trim().orEmpty()
            if (legacy.isNotBlank()) return legacy
        }
        return provider.defaultModel
    }

    fun saveSelectedModel(provider: AiProvider, model: String) {
        prefs.edit().putString(provider.modelPrefKey, model.trim()).apply()
        if (provider == AiProvider.GEMINI) {
            prefs.edit().putString(KEY_SELECTED_GEMINI_MODEL, model.trim()).apply()
        }
    }

    // Backward-compatible delegates
    fun saveCustomApiKey(key: String) {
        saveApiKey(getActiveProvider(), key)
    }

    fun saveSelectedModel(model: String) {
        saveSelectedModel(getActiveProvider(), model)
    }

    fun hasValidApiKey(provider: AiProvider = getActiveProvider()): Boolean {
        return getApiKey(provider).isNotBlank()
    }
}
