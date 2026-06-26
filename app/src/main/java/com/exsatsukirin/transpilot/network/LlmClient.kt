package com.exsatsukirin.transpilot.network

import com.exsatsukirin.transpilot.data.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(
    val httpCode: Int,
    val responseBody: String
) : Exception("API error $httpCode")

class LlmClient(
    private val maxRetries: Int = 2,
    private val retryDelayMs: Long = 1000L
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        config: ApiConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        val systemPrompt = config.systemPrompt
            .replace("{source}", sourceLang)
            .replace("{target}", targetLang)

        val body = JSONObject().apply {
            put("model", config.model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
            put("temperature", 0.3)
        }

        val requestBody = body.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(config.endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        executeWithRetry(request)
    }

    private suspend fun executeWithRetry(request: Request): Result<String> {
        var lastException: Exception? = null
        var delayMs = retryDelayMs

        for (attempt in 0..maxRetries) {
            if (attempt > 0) delay(delayMs)
            delayMs *= 2

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val ex = ApiException(response.code, responseBody)
                    // Only retry on server errors (5xx) and rate limits (429)
                    if (response.code >= 500 || response.code == 429) {
                        lastException = ex
                        continue
                    }
                    return Result.failure(ex)
                }

                val json = JSONObject(responseBody)
                val choices = json.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content").trim()

                return Result.success(content)
            } catch (e: IOException) {
                // Network errors are retryable
                lastException = e
            } catch (e: ApiException) {
                throw e // Already handled above, shouldn't reach here
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    companion object {
        fun parseErrorMessage(body: String, httpCode: Int): String {
            val jsonStart = body.indexOf("{")
            if (jsonStart >= 0) {
                try {
                    val errObj = JSONObject(body.substring(jsonStart)).optJSONObject("error")
                    if (errObj != null) {
                        val errMsg = errObj.optString("message", "")
                        val errCode = errObj.optString("code", "")
                        val detail = if (errCode.isNotBlank()) "$errMsg ($errCode)" else errMsg
                        return if (detail.isNotBlank()) detail else "服务器错误 (HTTP $httpCode)"
                    }
                } catch (_: Exception) {}
            }
            // Fallback: never expose raw body which may contain API key in error responses
            val statusReason = if (httpCode == 401) "未授权，请检查 API Key"
                else if (httpCode == 403) "禁止访问"
                else if (httpCode == 404) "端点不存在"
                else if (httpCode == 429) "请求过于频繁"
                else if (httpCode >= 500) "服务器内部错误"
                else "请求错误"
            return "$statusReason (HTTP $httpCode)"
        }
    }
}
