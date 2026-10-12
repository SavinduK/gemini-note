package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

enum class KeySource {
    MANUAL,
    BUILD_CONFIG,
    NONE
}

object ApiKeyManager {
    private const val PREFS_NAME = "stickydesk_api_prefs"
    private const val KEY_MANUAL_API_KEY = "manual_gemini_api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getManualApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_MANUAL_API_KEY, "")?.trim() ?: ""
    }

    fun setManualApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_MANUAL_API_KEY, key.trim()).apply()
    }

    fun clearManualApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_MANUAL_API_KEY).apply()
    }

    fun getActiveApiKey(context: Context): String {
        val manual = getManualApiKey(context)
        if (manual.isNotBlank()) {
            return manual
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun getKeySource(context: Context): KeySource {
        val manual = getManualApiKey(context)
        if (manual.isNotBlank()) {
            return KeySource.MANUAL
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return KeySource.BUILD_CONFIG
        }
        return KeySource.NONE
    }

    fun getMaskedKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        return key.take(6) + "••••••••" + key.takeLast(4)
    }
}
