package com.phonkskull.editor.drive

import android.content.Context
import com.phonkskull.editor.data.PhonkTrack
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

class DriveManager(private val context: Context) {

    private val client = OkHttpClient()
    private val FOLDER_NAME = "PhonkSkull_Tracks"

    suspend fun listTracks(accessToken: String): List<PhonkTrack> = withContext(Dispatchers.IO) {
        // Find or create folder
        val folderId = getOrCreateFolder(accessToken)

        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q='$folderId'+in+parents+and+mimeType+contains+'audio'&fields=files(id,name)")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: return@withContext emptyList()
        val json = JSONObject(body)
        val files = json.getJSONArray("files")

        (0 until files.length()).map { i ->
            val f = files.getJSONObject(i)
            PhonkTrack(
                id = f.getString("id"),
                name = f.getString("name"),
                driveFileId = f.getString("id"),
            )
        }
    }

    suspend fun uploadTrack(accessToken: String, file: File): PhonkTrack = withContext(Dispatchers.IO) {
        val folderId = getOrCreateFolder(accessToken)

        val metadata = """{"name":"${file.name}","parents":["$folderId"]}"""
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null,
                metadata.toRequestBody("application/json".toMediaType()))
            .addFormDataPart("file", file.name,
                file.asRequestBody("audio/*".toMediaType()))
            .build()

        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        val resp = client.newCall(req).execute()
        val json = JSONObject(resp.body?.string() ?: "{}")
        PhonkTrack(
            id = json.getString("id"),
            name = json.getString("name"),
            driveFileId = json.getString("id"),
        )
    }

    suspend fun downloadTrack(accessToken: String, track: PhonkTrack, cacheDir: File): File =
        withContext(Dispatchers.IO) {
            val outFile = File(cacheDir, "phonk_${track.id}_${track.name}")
            if (outFile.exists()) return@withContext outFile

            val req = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/${track.driveFileId}?alt=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val resp = client.newCall(req).execute()
            outFile.outputStream().use { out ->
                resp.body?.byteStream()?.copyTo(out)
            }
            outFile
        }

    private suspend fun getOrCreateFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        // Search for existing folder
        val searchReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=name='$FOLDER_NAME'+and+mimeType='application/vnd.google-apps.folder'&fields=files(id)")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val searchResp = client.newCall(searchReq).execute()
        val searchJson = JSONObject(searchResp.body?.string() ?: "{}")
        val existing = searchJson.getJSONArray("files")
        if (existing.length() > 0) {
            return@withContext existing.getJSONObject(0).getString("id")
        }

        // Create folder
        val body = """{"name":"$FOLDER_NAME","mimeType":"application/vnd.google-apps.folder"}"""
        val createReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val createResp = client.newCall(createReq).execute()
        JSONObject(createResp.body?.string() ?: "{}").getString("id")
    }
}
