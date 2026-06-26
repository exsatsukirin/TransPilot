package com.exsatsukirin.transpilot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.promptDataStore: DataStore<Preferences> by preferencesDataStore(name = "prompt_presets")

class PromptPresetRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val KEY_ACTIVE_PRESET_ID = stringPreferencesKey("active_preset_id")
        private val KEY_CUSTOM_PRESETS = stringPreferencesKey("custom_presets")
    }

    /** Currently selected preset ID. Defaults to "default". */
    val activePresetId: Flow<String> = context.promptDataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PRESET_ID] ?: PROMPT_DEFAULT.id
    }

    /** All custom (user-created) presets. */
    val customPresets: Flow<List<PromptPreset>> = context.promptDataStore.data.map { prefs ->
        val raw = prefs[KEY_CUSTOM_PRESETS] ?: ""
        if (raw.isBlank()) emptyList()
        else try {
            json.decodeFromString<List<PromptPreset>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun setActivePresetId(id: String) {
        context.promptDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PRESET_ID] = id
        }
    }

    suspend fun saveCustomPresets(presets: List<PromptPreset>) {
        context.promptDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_PRESETS] = json.encodeToString(presets)
        }
    }
}
