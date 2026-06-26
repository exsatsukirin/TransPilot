package com.exsatsukirin.transpilot.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.BuildConfig
import com.exsatsukirin.transpilot.R
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

    val themeOptions = listOf(
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_system)
    )
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
        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleLarge)
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
        Text(stringResource(R.string.api_settings), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it; saved = false },
            label = { Text(stringResource(R.string.api_endpoint)) },
            placeholder = { Text("https://api.openai.com/v1/chat/completions") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            label = { Text(stringResource(R.string.api_key)) },
            placeholder = { Text("sk-...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = model,
            onValueChange = { model = it; saved = false },
            label = { Text(stringResource(R.string.api_model)) },
            placeholder = { Text("gpt-4o-mini") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(stringResource(R.string.system_prompt), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.system_prompt_hint),
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
            Text(stringResource(R.string.save_config))
        }
        if (saved) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.config_saved), color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════
        // Section 3: 关于
        // ════════════════════════════════════════
        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        SettingsInfoRow(label = stringResource(R.string.about_app_name), value = "TransPilot")
        SettingsInfoRow(label = stringResource(R.string.about_version), value = BuildConfig.VERSION_NAME)
        SettingsInfoRow(label = stringResource(R.string.about_package), value = BuildConfig.APPLICATION_ID)
        SettingsInfoRow(label = stringResource(R.string.about_license), value = stringResource(R.string.license_mit))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.about_description),
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
