package com.exsatsukirin.transpilot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "api_config")

class ApiConfigRepository(private val context: Context) {
    companion object {
        val KEY_ENDPOINT = stringPreferencesKey("endpoint")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_SOURCE_LANG = stringPreferencesKey("source_lang")
        val KEY_TARGET_LANG = stringPreferencesKey("target_lang")
    }

    val config: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        ApiConfig(
            endpoint = prefs[KEY_ENDPOINT] ?: "https://api.openai.com/v1/chat/completions",
            apiKey = prefs[KEY_API_KEY] ?: "",
            model = prefs[KEY_MODEL] ?: "gpt-4o-mini",
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "You are a professional translator. Translate the following text from {source} to {target}. Only return the translated text, no explanations."
        )
    }

    val sourceLang: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOURCE_LANG] ?: "English"
    }

    val targetLang: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_TARGET_LANG] ?: "Chinese"
    }

    suspend fun update(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ENDPOINT] = config.endpoint.trim()
            prefs[KEY_API_KEY] = config.apiKey.trim()
            prefs[KEY_MODEL] = config.model.trim()
            prefs[KEY_SYSTEM_PROMPT] = config.systemPrompt.trim()
        }
    }

    suspend fun setSourceLang(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOURCE_LANG] = lang
        }
    }

    suspend fun setTargetLang(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TARGET_LANG] = lang
        }
    }
}
