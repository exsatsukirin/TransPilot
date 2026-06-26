# TransPilot — 设置页重构计划（深色模式 + API设置 + 关于）

> **For Hermes:** 按 Task 顺序执行。每个 Task 完成后构建验证再继续。

**Goal:** 将设置页从单一的 API 配置页面重构为三个分区的设置页面：深色模式控制、API 设置、关于信息。

**Architecture:**
- DataStore 新增 `theme_mode` key ("system"/"light"/"dark")
- `Theme.kt` 接收 `themeMode` 参数替代直接调用 `isSystemInDarkTheme()`
- `SettingsScreen.kt` 用 `HorizontalDivider` 分为三个视觉分区
- ViewModel 暴露 `themeMode` 状态和 setter

**Tech Stack:** Jetpack Compose, Material 3, DataStore Preferences

---

## 涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `data/ApiConfigRepository.kt` | **Modify** | 新增 `themeMode` key/flow/setter |
| `ui/theme/Theme.kt` | **Modify** | `darkTheme` 参数改为 `themeMode: String` |
| `ui/TranslatorViewModel.kt` | **Modify** | 新增 `themeMode` StateFlow 和 `setThemeMode()` |
| `ui/SettingsScreen.kt` | **Rewrite** | 三分区布局 |
| `MainActivity.kt` | **Modify** | 读取 ViewModel 的 themeMode 并传给 TransPilotTheme |

---

## Task 1: DataStore 添加 themeMode

**Objective:** 在 `ApiConfigRepository` 中新增深色模式偏好持久化。

**Files:**
- Modify: `data/ApiConfigRepository.kt`

**Step 1:** 在 `companion object` 中添加 key
```kotlin
val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
```

**Step 2:** 添加 Flow
```kotlin
val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[KEY_THEME_MODE] ?: "system"
}
```

**Step 3:** 添加 setter
```kotlin
suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { prefs ->
        prefs[KEY_THEME_MODE] = mode
    }
}
```

---

## Task 2: Theme.kt 接受 themeMode 字符串

**Objective:** `TransPilotTheme` 不再直接调用 `isSystemInDarkTheme()`，而是接受外部传入的 `themeMode`。

**Files:**
- Modify: `ui/theme/Theme.kt`

**Step 1:** 修改函数签名
```kotlin
@Composable
fun TransPilotTheme(
    themeMode: String = "system",   // "system" | "light" | "dark"
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark   // "system" 跟随系统
    }
    // ... 其余不变
}
```

移除 `darkTheme: Boolean = isSystemInDarkTheme()` 参数，改为 `themeMode: String = "system"`。

---

## Task 3: ViewModel 暴露 themeMode

**Objective:** ViewModel 连接 DataStore 与 UI。

**Files:**
- Modify: `ui/TranslatorViewModel.kt`

**Step 1:** 添加 StateFlow
```kotlin
val themeMode: StateFlow<String> = configRepo.themeMode
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
```

**Step 2:** 添加 setter
```kotlin
fun setThemeMode(mode: String) {
    viewModelScope.launch { configRepo.setThemeMode(mode) }
}
```

---

## Task 4: MainActivity 传递 themeMode

**Objective:** 读取 ViewModel 的 themeMode 并传给 Theme。

**Files:**
- Modify: `MainActivity.kt`

**Step 1:** 在 `TransPilotTheme {` 上方读取 themeMode
```kotlin
setContent {
    val themeMode by viewModel.themeMode.collectAsState()
    TransPilotTheme(themeMode = themeMode) {
        // ... 现有内容
    }
}
```

---

## Task 5: 重写 SettingsScreen

**Objective:** 三分区布局：深色模式 / API 设置 / 关于。

**Files:**
- Rewrite: `ui/SettingsScreen.kt`

**Step 1: 完整代码**

```kotlin
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
    val config by viewModel.apiConfig.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

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

        // Single-segment row using FilterChip
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

        // Info rows
        SettingsInfoRow(label = "应用名称", value = "TransPilot")
        SettingsInfoRow(label = "版本", value = BuildConfig.VERSION_NAME)
        SettingsInfoRow(label = "包名", value = BuildConfig.APPLICATION_ID)
        SettingsInfoRow(
            label = "开源许可",
            value = "MIT License"
        )
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
```

---

## 执行顺序

```
Task 1: ApiConfigRepository.kt  ← DataStore 新增 3 行
Task 2: Theme.kt                ← 修改函数签名 (4 行改动)
Task 3: TranslatorViewModel.kt  ← 新增 4 行
Task 4: MainActivity.kt         ← 新增 1 行 collectAsState + 传参
Task 5: SettingsScreen.kt       ← 完全重写
```

## 验证清单

- [ ] 构建通过：`gradle clean assembleDebug`
- [ ] 安装后设置页显示三个分区（深色模式 / API 设置 / 关于）
- [ ] 切换「浅色」→ UI 立即变为浅色
- [ ] 切换「深色」→ UI 立即变为深色
- [ ] 切换「跟随系统」→ 随系统设置而变化
- [ ] 切换后杀进程重启，保留上次的选择
- [ ] API 设置保存仍然正常
- [ ] 关于页显示版本号 1.0

## 风险和注意事项

1. **`BuildConfig` 访问** — 需要 `import com.exsatsukirin.transpilot.BuildConfig`。在 AGP 8.7+ 默认启用非最终版本（debug）的 BuildConfig，应可直接使用。
2. **`FilterChip` 的 weight(1f)** — 三个芯片等宽排列，在窄屏上可能文字截断。如果 "跟随系统" 显示不全，可将 `themeOptions` 缩写为 `themeValues` 映射。
3. **`HorizontalDivider`** — 在 Material 3 中替代了旧的 `Divider`，如果编译报错，改用 `Divider()`（已废弃但可用）。
