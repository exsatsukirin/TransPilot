package com.exsatsukirin.transpilot.network

import com.exsatsukirin.transpilot.data.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LlmClient {
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
        try {
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

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API error ${response.code}: $responseBody")
                )
            }

            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content").trim()

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
