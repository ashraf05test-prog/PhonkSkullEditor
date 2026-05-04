package com.phonkskull.editor.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonkskull.editor.data.ColorGradingOptions
import com.phonkskull.editor.data.ColorPreset
import com.phonkskull.editor.data.ExportOptions
import com.phonkskull.editor.data.GlowTextOptions
import com.phonkskull.editor.data.PhonkTrack
import com.phonkskull.editor.data.ProcessOptions
import com.phonkskull.editor.data.SecureSettings
import com.phonkskull.editor.data.SecureSettingsStore
import com.phonkskull.editor.data.ShakeOptions
import com.phonkskull.editor.data.SkullOptions
import com.phonkskull.editor.data.TextGlowColor
import com.phonkskull.editor.drive.DriveManager
import com.phonkskull.editor.media.VideoProcessor
import com.phonkskull.editor.media.YouTubeUploader
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {

    // Video source
    var videoLocalPath by mutableStateOf<String?>(null)
    var videoUri by mutableStateOf<Uri?>(null)

    // Glow text
    var glowTextEnabled by mutableStateOf(true)
    var glowText by mutableStateOf("Wait for end...")
    var glowColor by mutableStateOf(TextGlowColor.RED)

    // Color grading
    var colorGradingEnabled by mutableStateOf(true)
    var colorPreset by mutableStateOf(ColorPreset.MOODY)

    // Skull
    var skullUri by mutableStateOf<Uri?>(null)
    var skullPath by mutableStateOf<String?>(null)
    var skullSizePct by mutableFloatStateOf(0.55f)
    var climaxTimeSec by mutableDoubleStateOf(0.0)
    var climaxTimeText by mutableStateOf("00:00:00")

    // Phonk
    var phonkTracks = mutableStateListOf<PhonkTrack>()
    var selectedPhonkTrack by mutableStateOf<PhonkTrack?>(null)
    var phonkLocalPath by mutableStateOf<String?>(null)
    var phonkVolume by mutableIntStateOf(85)
    var driveTracksLoading by mutableStateOf(false)

    // Shake
    var shakeEnabled by mutableStateOf(false)
    var shakeIntensity by mutableFloatStateOf(8f)

    // Export
    var saveToGallery by mutableStateOf(true)
    var uploadToYouTube by mutableStateOf(false)
    var ytTitle by mutableStateOf("")
    var ytPrivacy by mutableStateOf("private")

    // State
    var working by mutableStateOf(false)
    var progress by mutableIntStateOf(0)
    var progressMsg by mutableStateOf("")
    var outputPath by mutableStateOf<String?>(null)
    var errorMsg by mutableStateOf<String?>(null)

    // Settings
    var secureSettings by mutableStateOf(SecureSettings())
    private lateinit var settingsStore: SecureSettingsStore

    fun init(context: Context) {
        settingsStore = SecureSettingsStore(context)
        secureSettings = settingsStore.load()
    }

    fun saveSettings(s: SecureSettings) {
        secureSettings = s
        settingsStore.save(s)
    }

    fun attachVideoUri(context: Context, uri: Uri) {
        videoUri = uri
        val file = copyUriToCache(context, uri, "video_src.mp4")
        videoLocalPath = file?.absolutePath
    }

    fun attachSkullUri(context: Context, uri: Uri) {
        skullUri = uri
        val file = copyUriToCache(context, uri, "skull.png")
        skullPath = file?.absolutePath
    }

    fun attachPhonkUri(context: Context, uri: Uri) {
        val file = copyUriToCache(context, uri, "phonk_local.mp3")
        phonkLocalPath = file?.absolutePath
        selectedPhonkTrack = null
    }

    fun loadDriveTracks(context: Context) {
        val token = secureSettings.driveAccessToken
        if (token.isBlank()) {
            errorMsg = "Google Drive not connected. Add token in Settings."
            return
        }
        viewModelScope.launch {
            driveTracksLoading = true
            try {
                val tracks = DriveManager(context).listTracks(token)
                phonkTracks.clear()
                phonkTracks.addAll(tracks)
            } catch (e: Exception) {
                errorMsg = "Drive error: ${e.message}"
            } finally {
                driveTracksLoading = false
            }
        }
    }

    fun uploadPhonkToDrive(context: Context, uri: Uri) {
        val token = secureSettings.driveAccessToken
        if (token.isBlank()) { errorMsg = "Google Drive not connected."; return }
        viewModelScope.launch {
            driveTracksLoading = true
            try {
                val file = copyUriToCache(context, uri, "upload_${System.currentTimeMillis()}.mp3") ?: return@launch
                val track = DriveManager(context).uploadTrack(token, file)
                phonkTracks.add(0, track)
            } catch (e: Exception) {
                errorMsg = "Upload error: ${e.message}"
            } finally {
                driveTracksLoading = false
            }
        }
    }

    fun process(context: Context) {
        val src = videoLocalPath ?: run { errorMsg = "Pick a video first!"; return }
        working = true
        progress = 0
        progressMsg = "Starting..."
        outputPath = null
        errorMsg = null

        viewModelScope.launch {
            try {
                // Resolve phonk path
                var resolvedPhonk = phonkLocalPath
                if (resolvedPhonk == null && selectedPhonkTrack != null) {
                    progressMsg = "Downloading phonk from Drive..."
                    val cacheDir = File(context.cacheDir, "phonk")
                    cacheDir.mkdirs()
                    resolvedPhonk = DriveManager(context).downloadTrack(
                        secureSettings.driveAccessToken,
                        selectedPhonkTrack!!,
                        cacheDir
                    ).absolutePath
                }

                val opts = ProcessOptions(
                    sourcePath = src,
                    glowText = GlowTextOptions(glowTextEnabled, glowText, glowColor),
                    colorGrading = ColorGradingOptions(colorGradingEnabled, colorPreset),
                    skull = SkullOptions(skullUri, skullPath, skullSizePct, climaxTimeSec),
                    phonkPath = resolvedPhonk,
                    phonkVolume = phonkVolume,
                    shake = ShakeOptions(shakeEnabled, shakeIntensity),
                    export = ExportOptions(saveToGallery, uploadToYouTube, ytTitle, ytPrivacy),
                )

                val tempDir = File(context.cacheDir, "phonkskull_tmp")
                val result = VideoProcessor(context, tempDir).process(opts) { p, msg ->
                    progress = p
                    progressMsg = msg
                }

                // Save to gallery
                if (saveToGallery) {
                    saveToGallery(context, result.outputPath)
                }

                // YouTube upload
                if (uploadToYouTube && secureSettings.youtubeAccessToken.isNotBlank()) {
                    progressMsg = "Uploading to YouTube..."
                    YouTubeUploader().upload(
                        secureSettings.youtubeAccessToken,
                        File(result.outputPath),
                        ytTitle.ifBlank { "Phonk Edit #shorts" },
                        onProgress = { progress = it }
                    )
                }

                outputPath = result.outputPath
                progressMsg = "Done! ✅"
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                working = false
            }
        }
    }

    private fun copyUriToCache(context: Context, uri: Uri, name: String): File? {
        return try {
            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            file
        } catch (e: Exception) { null }
    }

    private fun saveToGallery(context: Context, path: String) {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "PhonkSkull_${System.currentTimeMillis()}.mp4")
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/PhonkSkull")
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { out ->
            File(path).inputStream().use { it.copyTo(out) }
        }
    }
}
