package com.exsatsukirin.transpilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exsatsukirin.transpilot.data.*
import com.exsatsukirin.transpilot.network.LlmClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val AUTO_DETECT = "自动检测"
    }

    private val dao = AppDatabase.getInstance(application).translationDao()
    private val configRepo = ApiConfigRepository(application)
    private val llmClient = LlmClient()

    init {
        seedTestData()
    }

    private fun seedTestData() {
        viewModelScope.launch {
            val count = dao.count()
            if (count < 1000) {
                val samples = listOf(
                    "Hello, how are you?" to "你好，你怎么样？",
                    "The weather is beautiful today." to "今天天气真好。",
                    "I love programming in Kotlin." to "我喜欢用Kotlin编程。",
                    "Android development is fun." to "Android开发很有趣。",
                    "Machine learning is transforming the world." to "机器学习正在改变世界。",
                    "Have you tried the new feature?" to "你试过新功能了吗？",
                    "This is a long text that should test the expand and collapse behavior of the history cards in the TransPilot application." to "这是一个长文本，用于测试TransPilot应用中历史卡片的展开和收起行为。",
                    "The quick brown fox jumps over the lazy dog." to "敏捷的棕色狐狸跳过了懒惰的狗。",
                    "Technology advances at an unprecedented pace." to "技术以空前的速度发展。",
                    "Learning a new language opens up new opportunities." to "学习一门新语言打开了新的机会。"
                )
                val startTime = System.currentTimeMillis() - 1000L * 3600 * 24 * 30 // 30 days ago
                val batch = (0 until 1000).map { i ->
                    val (en, zh) = samples[i % samples.size]
                    TranslationRecord(
                        sourceText = en + " (#${i + 1})",
                        translatedText = zh + " (#${i + 1})",
                        sourceLang = if (i % 5 == 0) "自动检测" else "English",
                        targetLang = listOf("Chinese", "Japanese", "French", "German", "Spanish")[i % 5],
                        timestamp = startTime + i * 3600_000L, // 1 hour apart
                        isFavorite = i % 7 == 0
                    )
                }
                dao.insertAll(batch)
            }
        }
    }

    // ── Config ──
    /** Single atomic state: holds both the ApiConfig and whether DataStore has loaded.
     *  Using one StateFlow guarantees config and loaded update together, avoiding
     *  race conditions that cause the floating-label animation on API Key. */
    data class ConfigState(val config: ApiConfig, val loaded: Boolean)

    val configState: StateFlow<ConfigState> = configRepo.config
        .map { ConfigState(it, true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConfigState(ApiConfig(), false))

    /** Convenience derived flow (same atomic source). */
    val apiConfig: StateFlow<ApiConfig> = configState.map { it.config }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ApiConfig())

    // ── Translate state ──
    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    val sourceLang: StateFlow<String> = configRepo.sourceLang
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

    val targetLang: StateFlow<String> = configRepo.targetLang
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Chinese")

    val themeMode: StateFlow<String> = configRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ── History ──
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly

    val history: StateFlow<List<TranslationRecord>> = combine(
        dao.getAll(), _searchQuery, _showFavoritesOnly
    ) { all, query, favOnly ->
        var list = all
        if (favOnly) list = list.filter { it.isFavorite }
        if (query.isNotBlank()) {
            list = list.filter {
                it.sourceText.contains(query, ignoreCase = true) ||
                        it.translatedText.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSourceText(text: String) { _sourceText.value = text }
    fun setTranslatedText(text: String) { _translatedText.value = text }
    fun setSourceLang(lang: String) {
        viewModelScope.launch { configRepo.setSourceLang(lang) }
    }
    fun setTargetLang(lang: String) {
        viewModelScope.launch { configRepo.setTargetLang(lang) }
    }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setShowFavoritesOnly(v: Boolean) { _showFavoritesOnly.value = v }
    fun clearError() { _errorMessage.value = null }

    fun translate() {
        val text = _sourceText.value.trim()
        if (text.isEmpty()) {
            _errorMessage.value = "请输入要翻译的文本"
            return
        }
        viewModelScope.launch {
            _isTranslating.value = true
            _errorMessage.value = null
            val sourceLang = this@TranslatorViewModel.sourceLang.value
            val targetLang = this@TranslatorViewModel.targetLang.value
            // If auto-detect, rewrite system prompt to let LLM detect source
            val effectiveConfig = if (sourceLang == AUTO_DETECT) {
                val basePrompt = apiConfig.value.systemPrompt
                val autoPrompt = basePrompt
                    .replace("{source}", "the source language")
                    .replace("{target}", targetLang)
                    .replace(
                        "Translate the following text from the source language to",
                        "Detect the source language of the following text and translate it to"
                    )
                apiConfig.value.copy(systemPrompt = autoPrompt)
            } else {
                apiConfig.value
            }
            val result = llmClient.translate(
                text, sourceLang, targetLang, effectiveConfig
            )
            result.onSuccess { translated ->
                _translatedText.value = translated
                dao.insert(
                    TranslationRecord(
                        sourceText = text,
                        translatedText = translated,
                        sourceLang = this@TranslatorViewModel.sourceLang.value,
                        targetLang = this@TranslatorViewModel.targetLang.value
                    )
                )
            }.onFailure { e ->
                val msg = e.message ?: "未知错误"
                // Try to extract human-readable error from JSON API response
                val userMsg = if (msg.startsWith("API error ")) {
                    val jsonStart = msg.indexOf("{")
                    if (jsonStart >= 0) {
                        try {
                            val errJson = JSONObject(msg.substring(jsonStart))
                            val errObj = errJson.optJSONObject("error")
                            if (errObj != null) {
                                val errMsg = errObj.optString("message", "")
                                val errCode = errObj.optString("code", "")
                                if (errCode.isNotBlank()) "$errMsg ($errCode)" else errMsg
                            } else msg
                        } catch (_: Exception) { msg }
                    } else msg
                } else msg
                _errorMessage.value = "翻译失败: $userMsg"
            }
            _isTranslating.value = false
        }
    }

    fun toggleFavorite(record: TranslationRecord) {
        viewModelScope.launch {
            dao.update(record.copy(isFavorite = !record.isFavorite))
        }
    }

    fun deleteRecord(record: TranslationRecord) {
        viewModelScope.launch {
            dao.delete(record)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun updateConfig(config: ApiConfig) {
        viewModelScope.launch {
            configRepo.update(config)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            configRepo.setThemeMode(mode)
        }
    }
}
