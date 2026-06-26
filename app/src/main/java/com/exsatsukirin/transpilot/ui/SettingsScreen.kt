package com.exsatsukirin.transpilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.BuildConfig
import com.exsatsukirin.transpilot.data.ApiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TranslatorViewModel) {
    val configState by viewModel.configState.collectAsState()
    val config = configState.config
    val configLoaded = configState.loaded
    val themeMode by viewModel.themeMode.collectAsState()

    if (!configLoaded) return

    var endpoint by remember(config) { mutableStateOf(config.endpoint) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var systemPrompt by remember(config) { mutableStateOf(config.systemPrompt) }
    var saved by remember { mutableStateOf(false) }

    val themeOptions = listOf("浅色", "深色", "跟随系统")
    val themeValues = listOf("light", "dark", "system")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ════════════════════════════════════════
        // Section 1: 深色模式
        // ════════════════════════════════════════
        Text("深色模式", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themeOptions.forEachIndexed { index, label ->
                val selected = themeMode == themeValues[index]
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setThemeMode(themeValues[index]) },
                    label = { Text(label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════
        // Section 2: API 设置
        // ════════════════════════════════════════
        Text("API 设置", style = MaterialTheme.typography.titleLarge)
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
                viewModel.updateConfig(ApiConfig(endpoint, apiKey, model, systemPrompt))
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

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════
        // Section 3: 关于
        // ════════════════════════════════════════
        Text("关于", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        SettingsInfoRow(label = "应用名称", value = "TransPilot")
        SettingsInfoRow(label = "版本", value = BuildConfig.VERSION_NAME)
        SettingsInfoRow(label = "包名", value = BuildConfig.APPLICATION_ID)
        SettingsInfoRow(label = "开源许可", value = "MIT License")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "TransPilot 是一个基于 LLM 的翻译助手。\n" +
            "使用 Material Design 3 设计，支持动态取色。\n" +
            "源码：github.com/exsatsukirin/TransPilot",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
