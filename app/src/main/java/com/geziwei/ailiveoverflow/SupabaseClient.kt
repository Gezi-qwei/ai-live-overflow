package com.geziwei.ailiveoverflow

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class Message(
    val id: String? = null,
    val content: String,
    val emotion: String = "idle",
    val created_at: String? = null
)

object SupabaseClient {
    private val client = OkHttpClient()
    private val gson = Gson()
    private const val TAG = "SupabaseClient"

    // Fetch latest message for the ghost to display
    suspend fun fetchLatestMessage(): Message? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/messages?order=created_at.desc&limit=1")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val arr = gson.fromJson(body, Array<Message>::class.java)
            arr.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "fetchLatestMessage error", e)
            null
        }
    }

    // Insert a new message (for testing / future use)
    suspend fun insertMessage(content: String, emotion: String = "idle"): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("content" to content, "emotion" to emotion))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SupabaseConfig.URL}/rest/v1/messages")
                .addHeader("apikey", SupabaseConfig.ANON_KEY)
                .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "insertMessage error", e)
            false
        }
    }
}
