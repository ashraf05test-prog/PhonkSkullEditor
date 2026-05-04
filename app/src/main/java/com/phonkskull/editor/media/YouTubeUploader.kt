package com.phonkskull.editor.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class YouTubeUploader {

    private val client = OkHttpClient.Builder()
        .writeTimeout(10, TimeUnit.MINUTES)
        .readTimeout(10, TimeUnit.MINUTES)
        .build()

    suspend fun upload(
        accessToken: String,
        videoFile: File,
        title: String,
        description: String = "#phonk #shorts #skull",
        privacyStatus: String = "private",
        onProgress: (Int) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {

        val metadata = JSONObject().apply {
            put("snippet", JSONObject().apply {
                put("title", title)
                put("description", description)
                put("tags", listOf("phonk", "shorts", "skull", "edit"))
                put("categoryId", "22")
            })
            put("status", JSONObject().apply {
                put("privacyStatus", privacyStatus)
            })
        }.toString()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null,
                metadata.toRequestBody("application/json".toMediaType()))
            .addFormDataPart("video", videoFile.name,
                videoFile.asRequestBody("video/mp4".toMediaType()))
            .build()

        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        onProgress(10)
        val resp = client.newCall(req).execute()
        onProgress(90)
        val json = JSONObject(resp.body?.string() ?: "{}")
        onProgress(100)
        json.optString("id", "")
    }
}
