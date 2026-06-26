package com.exsatsukirin.transpilot.data

data class ApiConfig(
    val endpoint: String = "https://api.openai.com/v1/chat/completions",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "You are a professional translator. Translate the following text from {source} to {target}. Only return the translated text, no explanations."
)

fun ApiConfig.buildAutoDetectConfig(targetLang: String): ApiConfig {
    val autoPrompt = systemPrompt
        .replace("{source}", "the source language")
        .replace("{target}", targetLang)
        .replace(
            "Translate the following text from the source language to",
            "Detect the source language of the following text and translate it to"
        )
    return copy(systemPrompt = autoPrompt)
}
