package com.exsatsukirin.transpilot.data

import kotlinx.serialization.Serializable

@Serializable
data class PromptPreset(
    val id: String,
    val name: String,
    val prompt: String,
    val isDefault: Boolean = false
)

/** Built-in default prompt — not editable, not deletable. */
val PROMPT_DEFAULT = PromptPreset(
    id = "default",
    name = "默认",
    prompt = "You are a professional translator. Translate the following text from {source} to {target}. Only return the translated text, no explanations.",
    isDefault = true
)
