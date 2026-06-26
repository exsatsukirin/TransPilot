package com.exsatsukirin.transpilot.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exsatsukirin.transpilot.data.*
import com.exsatsukirin.transpilot.network.ApiException
import com.exsatsukirin.transpilot.network.LlmClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn

class TranslatorViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val AUTO_DETECT = "自动检测"
    }

    private val dao = AppDatabase.getInstance(application).translationDao()
    private val configRepo = ApiConfigRepository(application)
    private val llmClient = LlmClient()

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

    /** Paged history for efficient first-load performance. */
    val pagedHistory: Flow<PagingData<TranslationRecord>> = Pager(
        config = PagingConfig(pageSize = 30, enablePlaceholders = false)
    ) {
        dao.getAllPaging()
    }.flow.cachedIn(viewModelScope)

    /** Paged history with current filter applied. Reacts to search/favorite changes. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredPagedHistory: Flow<PagingData<TranslationRecord>> =
        combine(_searchQuery, _showFavoritesOnly) { query, favOnly ->
            Pair(query, favOnly)
        }.flatMapLatest { (query, favOnly) ->
            Pager(
                config = PagingConfig(pageSize = 30, enablePlaceholders = false)
            ) {
                dao.getAllPagingFiltered(query, if (favOnly) 1 else 0)
            }.flow
        }.cachedIn(viewModelScope)

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
                apiConfig.value.buildAutoDetectConfig(targetLang)
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
                val userMsg = if (e is ApiException) {
                    LlmClient.parseErrorMessage(e.responseBody, e.httpCode)
                } else {
                    e.message ?: "未知错误"
                }
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
