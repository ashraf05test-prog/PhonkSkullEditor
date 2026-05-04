package com.phonkskull.editor.data

import android.net.Uri

enum class ColorPreset {
    NONE, MOODY, DARK_PHONK, CINEMATIC, COLD_BLUE, VINTAGE, HIGH_CONTRAST
}

enum class TextGlowColor { RED, GREEN }

data class GlowTextOptions(
    val enabled: Boolean = true,
    val text: String = "Wait for end...",
    val glowColor: TextGlowColor = TextGlowColor.RED,
    val fontSize: Float = 52f,
)

data class ColorGradingOptions(
    val enabled: Boolean = true,
    val preset: ColorPreset = ColorPreset.MOODY,
)

data class PhonkTrack(
    val id: String,
    val name: String,
    val driveFileId: String,
    val localPath: String? = null,
)

data class SkullOptions(
    val imageUri: Uri? = null,
    val imagePath: String? = null,
    val sizePct: Float = 0.55f,       // fraction of frame width
    val climaxTimeSec: Double = 0.0,  // when skull appears
)

data class ShakeOptions(
    val enabled: Boolean = false,
    val intensity: Float = 8f,
)

data class ExportOptions(
    val saveToGallery: Boolean = true,
    val uploadToYouTube: Boolean = false,
    val ytTitle: String = "",
    val ytPrivacy: String = "private",
)

data class ProcessOptions(
    val sourcePath: String,
    val glowText: GlowTextOptions = GlowTextOptions(),
    val colorGrading: ColorGradingOptions = ColorGradingOptions(),
    val skull: SkullOptions = SkullOptions(),
    val phonkPath: String? = null,
    val phonkVolume: Int = 85,
    val shake: ShakeOptions = ShakeOptions(),
    val export: ExportOptions = ExportOptions(),
)

data class ProcessResult(val outputPath: String)

data class SecureSettings(
    val youtubeClientId: String = "",
    val youtubeClientSecret: String = "",
    val youtubeAccessToken: String = "",
    val youtubeRefreshToken: String = "",
    val youtubeTokenExpiry: Long = 0L,
    val driveAccessToken: String = "",
    val driveRefreshToken: String = "",
    val driveTokenExpiry: Long = 0L,
)
