package com.exsatsukirin.transpilot.ui

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.exsatsukirin.transpilot.ui.theme.TransPilotTheme

class CapturePermissionActivity : ComponentActivity() {

    companion object {
        private var onTranslationDone: (() -> Unit)? = null
        fun notifyTranslationDone() {
            onTranslationDone?.invoke()
            onTranslationDone = null
        }
    }

    /** State: 0=requesting, 1=processing */
    private var stage by mutableStateOf(0)

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onTranslationDone = { finishAndRemoveTask() }
        ScreenCaptureService.setCaptureResult(result.resultCode, result.data)
        stage = 1  // switch to processing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TransPilotTheme(themeMode = "system") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (stage == 0) "正在请求屏幕捕获权限..."
                        else "正在识别屏幕文字..."
                    )
                }
            }
        }

        // Launch capture permission dialog
        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            captureLauncher.launch(mgr.createScreenCaptureIntent())
        } catch (e: Exception) {
            finish()
        }
    }
}
