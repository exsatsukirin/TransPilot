package com.exsatsukirin.transpilot.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.data.ApiConfigRepository
import com.exsatsukirin.transpilot.data.AppDatabase
import com.exsatsukirin.transpilot.data.TranslationRecord
import com.exsatsukirin.transpilot.network.LlmClient
import com.exsatsukirin.transpilot.ui.theme.TransPilotTheme
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class TranslateOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceText = readProcessTextIntent(intent) ?: run {
            finish()
            return
        }

        setContent {
            TransPilotTheme(themeMode = "system") {
                TranslateOverlayContent(sourceText = sourceText, onDismiss = { finish() })
            }
        }
    }

    companion object {
        private fun readProcessTextIntent(intent: Intent): String? {
            if (intent.action == Intent.ACTION_PROCESS_TEXT) {
                return intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.toString()
                    ?.trim()
            }
            return null
        }
    }
}

@Composable
private fun TranslateOverlayContent(
    sourceText: String,
    onDismiss: () -> Unit
) {
    var result by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var targetLang by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val configRepo = ApiConfigRepository(context.applicationContext)
            val llmClient = LlmClient()
            val dao = AppDatabase.getInstance(context.applicationContext).translationDao()

            val config = configRepo.config.first()
            targetLang = configRepo.targetLang.first()

            val autoPrompt = config.systemPrompt
                .replace("{source}", "the source language")
                .replace("{target}", targetLang)
                .replace(
                    "Translate the following text from the source language to",
                    "Detect the source language of the following text and translate it to"
                )
            val effectiveConfig = config.copy(systemPrompt = autoPrompt)

            llmClient.translate(sourceText, "auto", targetLang, effectiveConfig)
                .onSuccess { text ->
                    result = text
                    // Save to history
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        dao.insert(TranslationRecord(
                            sourceText = sourceText,
                            translatedText = text,
                            sourceLang = "自动检测",
                            targetLang = targetLang
                        ))
                    }
                }
                .onFailure { e -> result = "翻译失败: ${parseError(e.message ?: "未知错误")}"; isError = true }
        } catch (e: Exception) {
            result = "翻译失败: ${e.message ?: e.javaClass.simpleName}"
            isError = true
        }
    }

    if (result == null) {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.wrapContentSize(),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("TransPilot 翻译中...")
                }
            }
        }
    } else if (isError) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("TransPilot 翻译") },
            text = {
                Text(
                    text = result!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        )
    } else {
        val clipboard = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }
        val translated = result!!

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("TransPilot 翻译") },
            text = {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = translated,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(translated))
                            copied = true
                        }) {
                            Icon(
                                if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copied) "已复制" else "复制译文")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        )
    }
}

private fun parseError(msg: String): String {
    val jsonStart = msg.indexOf("{")
    if (jsonStart >= 0) {
        try {
            val errObj = JSONObject(msg.substring(jsonStart)).optJSONObject("error")
            if (errObj != null) {
                val errMsg = errObj.optString("message", "")
                val errCode = errObj.optString("code", "")
                return if (errCode.isNotBlank()) "$errMsg ($errCode)" else errMsg
            }
        } catch (_: Exception) {}
    }
    return msg
}
