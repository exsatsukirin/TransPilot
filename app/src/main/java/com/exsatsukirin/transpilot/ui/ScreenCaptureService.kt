package com.exsatsukirin.transpilot.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.exsatsukirin.transpilot.data.ApiConfigRepository
import com.exsatsukirin.transpilot.data.ScreenRecognizer
import com.exsatsukirin.transpilot.data.TranslationRecord
import com.exsatsukirin.transpilot.data.AppDatabase
import com.exsatsukirin.transpilot.network.LlmClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class ScreenCaptureService : Service() {

    companion object {
        private var pendingCaptureCallback: ((resultCode: Int, data: Intent?) -> Unit)? = null

        /** Called by CapturePermissionActivity when user grants/denies capture permission. */
        fun setCaptureResult(resultCode: Int, data: Intent?) {
            pendingCaptureCallback?.invoke(resultCode, data)
            pendingCaptureCallback = null
        }
    }

    private val NOTIFICATION_ID = 1001
    private lateinit var windowManager: WindowManager
    private var overlayView: android.view.View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val metrics: DisplayMetrics get() {
        val m = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(m)
        return m
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            showFloatingBubble()
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        removeFloatingBubble()
        super.onDestroy()
    }

    // ── Notification ──
    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            "screen_capture", "屏幕识别",
            NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)

        return NotificationCompat.Builder(this, "screen_capture")
            .setContentTitle("TransPilot 屏幕识别")
            .setContentText("悬浮球已开启")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()
    }

    // ── Floating bubble ──
    private fun showFloatingBubble() {
        if (overlayView != null) return

        val density = resources.displayMetrics.density
        val size = (56 * density).toInt()
        val bubbleView = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(size, size)
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(android.graphics.Color.parseColor("#1976D2"))
            background = bg
            val tv = TextView(this@ScreenCaptureService).apply {
                text = "译"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            addView(tv, ViewGroup.LayoutParams(size, size))
            // Tap vs drag detection
            var startX = 0f
            var startY = 0f
            var isDragging = false
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - startX
                        val dy = event.rawY - startY
                        if (kotlin.math.sqrt(dx * dx + dy * dy) > 20f) {
                            isDragging = true
                            overlayParams?.let { p ->
                                p.x = (event.rawX - size / 2).toInt()
                                p.y = (event.rawY - size).toInt()
                                windowManager.updateViewLayout(this, p)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            requestScreenCapture()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = metrics.heightPixels / 3
        }

        overlayView = bubbleView
        overlayParams = params
        try {
            windowManager.addView(bubbleView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun removeFloatingBubble() {
        try {
            overlayView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        overlayParams = null
    }

    // ── Translation result overlay ──
    private var resultOverlayView: android.view.View? = null

    private fun showTranslationOverlay(text: String) {
        // Remove previous result overlay if any
        dismissResultOverlay()

        val density = resources.displayMetrics.density
        val padding = (16 * density).toInt()
        val maxWidth = (metrics.widthPixels * 0.85).toInt()

        val tv = TextView(this).apply {
            setText(text)
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            setPadding(padding, padding, padding, padding)
            setLineSpacing(4f, 1f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(android.graphics.Color.parseColor("#CC1976D2")) // semi-transparent blue
            setOnClickListener { dismissResultOverlay() }
            // Auto-dismiss after 15 seconds
            postDelayed({ dismissResultOverlay() }, 15000L)
        }

        // Wrap in FrameLayout for positioning
        val container = FrameLayout(this).apply {
            val textSize = android.view.View.MeasureSpec.makeMeasureSpec(maxWidth, android.view.View.MeasureSpec.AT_MOST)
            tv.measure(textSize, android.view.View.MeasureSpec.UNSPECIFIED)
            val tvHeight = tv.measuredHeight
            val params = FrameLayout.LayoutParams(maxWidth, tvHeight + padding * 2)
            params.gravity = Gravity.CENTER
            tv.layoutParams = params
            addView(tv)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        resultOverlayView = container
        try {
            windowManager.addView(container, params)
        } catch (_: Exception) {}
    }

    private fun dismissResultOverlay() {
        try {
            resultOverlayView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        resultOverlayView = null
    }

    // ── Screen capture ──
    private fun requestScreenCapture() {
        pendingCaptureCallback = { resultCode, data ->
            onCapturePermissionGranted(resultCode, data)
        }
        val intent = Intent(this, CapturePermissionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        startActivity(intent)
    }

    private fun onCapturePermissionGranted(resultCode: Int, data: Intent?) {
        if (data == null) {
            Toast.makeText(this, "屏幕捕获授权失败", Toast.LENGTH_SHORT).show()
            return
        }
        // Show "processing" overlay immediately to prevent visual gap
        // (the CapturePermissionActivity stays alive showing its own processing state)

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        if (projection == null) {
            Toast.makeText(this, "无法创建屏幕投影", Toast.LENGTH_SHORT).show()
            return
        }
        // Register callback before using (required on Android 14+)
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Toast.makeText(this@ScreenCaptureService, "屏幕捕获已停止", Toast.LENGTH_SHORT).show()
            }
        }, mainHandler)
        captureScreenshot(projection)
    }

    private fun captureScreenshot(projection: MediaProjection) {
        val m = metrics
        val width = m.widthPixels
        val height = m.heightPixels
        val density = m.densityDpi

        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = projection.createVirtualDisplay(
            "TransPilotCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )

        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                image.close()
                reader.close()
                virtualDisplay.release()
                projection.stop()

                performOcrAndTranslate(cropped)
            }
        }, mainHandler)
    }

    private fun performOcrAndTranslate(bitmap: Bitmap) {
        serviceScope.launch {
            try {
                Toast.makeText(this@ScreenCaptureService, "识别中...", Toast.LENGTH_SHORT).show()

                val ocrResult = ScreenRecognizer.recognize(bitmap)
                if (ocrResult.isFailure) {
                    Toast.makeText(this@ScreenCaptureService,
                        ocrResult.exceptionOrNull()?.message ?: "识别失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val recognizedText = ocrResult.getOrThrow()

                val configRepo = ApiConfigRepository(this@ScreenCaptureService)
                val llmClient = LlmClient()
                val config = configRepo.config.first()
                val targetLang = configRepo.targetLang.first()

                val autoPrompt = config.systemPrompt
                    .replace("{source}", "the source language")
                    .replace("{target}", targetLang)
                    .replace(
                        "Translate the following text from the source language to",
                        "Detect the source language of the following text and translate it to"
                    )
                val effectiveConfig = config.copy(systemPrompt = autoPrompt)

                val translateResult = llmClient.translate(
                    recognizedText, "auto", targetLang, effectiveConfig
                )

                val displayText = translateResult.getOrNull() ?: run {
                    val err = translateResult.exceptionOrNull()
                    "翻译失败: ${err?.message ?: "未知错误"}"
                }

                // Save to history
                try {
                    val dao = AppDatabase.getInstance(this@ScreenCaptureService).translationDao()
                    dao.insert(TranslationRecord(
                        sourceText = recognizedText.take(500),
                        translatedText = displayText.take(500),
                        sourceLang = "自动检测(屏幕)",
                        targetLang = targetLang
                    ))
                } catch (_: Exception) {}

                // Show result as overlay (WindowManager, avoid Activity launch restrictions on Android 16)
                try {
                    showTranslationOverlay(displayText)
                    // Notify the permission activity so it can finish (prevents visual jump)
                    try {
                        CapturePermissionActivity.notifyTranslationDone()
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            } catch (e: Exception) {
                Toast.makeText(this@ScreenCaptureService,
                    "处理失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
