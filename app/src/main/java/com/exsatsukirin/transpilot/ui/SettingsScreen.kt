package com.exsatsukirin.transpilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.data.ApiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TranslatorViewModel) {
    val config by viewModel.apiConfig.collectAsState()

    var endpoint by remember(config) { mutableStateOf(config.endpoint) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var systemPrompt by remember(config) { mutableStateOf(config.systemPrompt) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("API 配置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it; saved = false },
            label = { Text("API 端点") },
            placeholder = { Text("https://api.openai.com/v1/chat/completions") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            label = { Text("API Key") },
            placeholder = { Text("sk-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = model,
            onValueChange = { model = it; saved = false },
            label = { Text("模型") },
            placeholder = { Text("gpt-4o-mini") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("系统提示词", style = MaterialTheme.typography.titleMedium)
        Text(
            "使用 {source} 和 {target} 作为占位符",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it; saved = false },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.updateConfig(
                    ApiConfig(endpoint, apiKey, model, systemPrompt)
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存配置")
        }

        if (saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("配置已保存 ✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}
