package com.example.llmtranslator.data

data class ApiConfig(
    val endpoint: String = "https://api.openai.com/v1/chat/completions",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "You are a professional translator. Translate the following text from {source} to {target}. Only return the translated text, no explanations."
)
