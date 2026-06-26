package com.exsatsukirin.transpilot.data

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** On-device OCR via Google ML Kit. Supports Chinese + Latin. */
object ScreenRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognize(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val task = recognizer.process(image)
            val result = Tasks.await(task)
            val text = result.textBlocks.joinToString("\n") { it.text }
            if (text.isBlank()) Result.failure(Exception("未识别到文字"))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
