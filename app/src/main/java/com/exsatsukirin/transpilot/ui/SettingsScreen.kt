package com.exsatsukirin.transpilot.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.BuildConfig
import com.exsatsukirin.transpilot.R
import com.exsatsukirin.transpilot.data.ApiConfig
import com.exsatsukirin.transpilot.data.PromptPreset
import com.exsatsukirin.transpilot.data.PROMPT_DEFAULT
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TranslatorViewModel) {
    val configState by viewModel.configState.collectAsState()
    val config = configState.config
    val configLoaded = configState.loaded
    val themeMode by viewModel.themeMode.collectAsState()
    val activePromptId by viewModel.activePromptId.collectAsState()
    val allPresets by viewModel.allPromptPresets.collectAsState()

    if (!configLoaded) return

    var endpoint by remember(config) { mutableStateOf(config.endpoint) }
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var model by remember(config) { mutableStateOf(config.model) }
    var saved by remember { mutableStateOf(false) }

    // ── Prompt editing dialog state ──
    var editDialogPreset by remember { mutableStateOf<PromptPreset?>(null) }
    var isNewPreset by remember { mutableStateOf(false) }

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

        Button(
            onClick = {
                viewModel.updateConfig(ApiConfig(endpoint, apiKey, model, config.systemPrompt))
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
        // Section 3: 提示词配置
        // ════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("提示词配置", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = {
                isNewPreset = true
                editDialogPreset = PromptPreset(
                    id = UUID.randomUUID().toString(),
                    name = "",
                    prompt = PROMPT_DEFAULT.prompt
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增提示词")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Active preset indicator
        val activePreset = allPresets.find { it.id == activePromptId } ?: PROMPT_DEFAULT
        Text(
            "当前: ${activePreset.name}${if (activePreset.isDefault) " (默认)" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Preset radio list
        allPresets.forEach { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setActivePromptId(preset.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = preset.id == activePromptId,
                        onClick = { viewModel.setActivePromptId(preset.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${preset.name}${if (preset.isDefault) " (默认)" else ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            preset.prompt.take(80) + if (preset.prompt.length > 80) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (!preset.isDefault) {
                        IconButton(onClick = {
                            editDialogPreset = preset
                            isNewPreset = false
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { viewModel.deletePromptPreset(preset.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // ════════════════════════════════════════
        // Section 4: 关于
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

    // ── Prompt editing dialog ──
    editDialogPreset?.let { preset ->
        var editName by remember { mutableStateOf(preset.name) }
        var editPrompt by remember { mutableStateOf(preset.prompt) }
        AlertDialog(
            onDismissRequest = { editDialogPreset = null },
            title = { Text(if (isNewPreset) "新增提示词" else "编辑提示词") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editPrompt,
                        onValueChange = { editPrompt = it },
                        label = { Text("提示词内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        maxLines = 8
                    )
                    Text(
                        "占位符: {source} = 源语言, {target} = 目标语言",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = preset.copy(name = editName, prompt = editPrompt)
                    if (isNewPreset) viewModel.addPromptPreset(updated)
                    else viewModel.updatePromptPreset(updated)
                    editDialogPreset = null
                }, enabled = editName.isNotBlank() && editPrompt.isNotBlank()) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editDialogPreset = null }) { Text("取消") }
            }
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
