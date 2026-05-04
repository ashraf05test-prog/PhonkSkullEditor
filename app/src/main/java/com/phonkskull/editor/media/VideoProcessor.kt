package com.phonkskull.editor.media

import android.content.Context
import com.phonkskull.editor.data.ColorPreset
import com.phonkskull.editor.data.ProcessOptions
import com.phonkskull.editor.data.ProcessResult
import java.io.File
import java.util.Locale
import kotlin.math.min

class VideoProcessor(
    private val context: Context,
    private val tempDir: File,
) {
    suspend fun process(
        options: ProcessOptions,
        progress: (Int, String) -> Unit,
    ): ProcessResult {
        tempDir.mkdirs()
        val input = File(options.sourcePath)
        require(input.exists()) { "Source video missing: ${input.absolutePath}" }

        val (srcW, srcH) = FfmpegExecutor.probeSize(input)
        val duration = FfmpegExecutor.probeDuration(input)

        val frameW = 1080
        val frameH = 1920

        // Resolve climax time
        val climaxSec = min(options.skull.climaxTimeSec, duration - 0.1)
        val freezeDuration = 3.0   // skull appears → 3 sec → end
        val outputDur = if (climaxSec > 0.5) climaxSec + freezeDuration else duration

        progress(5, "Preparing assets...")

        // Render glow text PNG
        val glowFile: File? = if (options.glowText.enabled && options.glowText.text.isNotBlank()) {
            GlowTextRenderer.render(context, options.glowText, frameW, frameH, tempDir)
        } else null

        val skullFile: File? = options.skull.imagePath?.let { File(it) }?.takeIf { it.exists() }
        val phonkFile: File? = options.phonkPath?.let { File(it) }?.takeIf { it.exists() }

        progress(15, "Building filter graph...")

        val colorChain = colorFilter(options.colorGrading.preset)
        val colorPrefix = if (colorChain.isBlank()) "" else "$colorChain,"

        val ratio = if (srcH > 0) srcW.toDouble() / srcH.toDouble() else (9.0 / 16.0)
        val exactNineSixteen = kotlin.math.abs(ratio - (9.0 / 16.0)) < 0.01

        val fc = StringBuilder()

        // Scale to 9:16
        if (exactNineSixteen) {
            fc.append("[0:v]${colorPrefix}scale=$frameW:$frameH[_vbase]")
        } else if (srcH > srcW) {
            fc.append("[0:v]${colorPrefix}scale=$frameW:$frameH:force_original_aspect_ratio=decrease,")
            fc.append("pad=$frameW:$frameH:(ow-iw)/2:(oh-ih)/2:color=black[_vbase]")
        } else {
            fc.append("[0:v]${colorPrefix}scale=$frameW:$frameH:force_original_aspect_ratio=decrease,")
            fc.append("pad=$frameW:$frameH:(ow-iw)/2:(oh-ih)/2:color=black[_vbase]")
        }

        var inputIdx = 1
        val glowIdx = if (glowFile != null) inputIdx++ else -1
        val skullIdx = if (skullFile != null) inputIdx++ else -1
        val phonkIdx = if (phonkFile != null) inputIdx++ else -1

        var videoLabel = "_vbase"

        // Glow text overlay (top of screen, always visible)
        if (glowIdx >= 0) {
            fc.append(";[$videoLabel][$glowIdx:v]overlay=0:0:shortest=0[_vglow]")
            videoLabel = "_vglow"
        }

        // Freeze + skull + shake at climax
        if (climaxSec > 0.5) {
            val pt = "%.3f".format(Locale.US, climaxSec)
            val skullPx = (frameW * options.skull.sizePct).toInt()

            // Freeze frame at climax
            fc.append(";[$videoLabel]trim=end=$pt,setpts=PTS-STARTPTS,")
            fc.append("tpad=stop_mode=clone:stop_duration=$freezeDuration[_frozen]")

            // Vignette darkening after freeze
            val cx = "%.1f".format(Locale.US, frameW / 2.0)
            val cy = "%.1f".format(Locale.US, frameH / 2.0)
            val vigAlpha = "min(0.8,max(0,t-$pt)/$freezeDuration)*255*max(0,(hypot(X-$cx,Y-$cy)/hypot($cx,$cy)-0.3)/0.7)"
            fc.append(";nullsrc=size=${frameW}x${frameH}:rate=25,format=rgba,")
            fc.append("geq=r='0':g='0':b='0':a='$vigAlpha'[_vig]")
            fc.append(";[_frozen][_vig]overlay=0:0:shortest=1[_vignetted]")
            videoLabel = "_vignetted"

            // Screen shake on freeze
            if (options.shake.enabled) {
                val amp = options.shake.intensity
                fc.append(";[$videoLabel]crop=$frameW:$frameH:")
                fc.append("x='if(gte(t,$pt),${amp}*sin(t*60),0)':")
                fc.append("y='if(gte(t,$pt),${amp}*cos(t*47),0)'[_shaken]")
                videoLabel = "_shaken"
            }

            // Skull overlay at climax
            if (skullIdx >= 0) {
                val punch = "w='$skullPx*(1.2-0.2*min(1,max(0,t-$pt)/0.3))':h=-1:eval=frame"
                fc.append(";[$skullIdx:v]scale=$punch[_skull]")
                fc.append(";[$videoLabel][_skull]overlay=")
                fc.append("x='(main_w-overlay_w)/2':y='(main_h-overlay_h)/2':")
                fc.append("enable='gte(t,$pt)'[_vwithskull]")
                videoLabel = "_vwithskull"
            }
        }

        fc.append(";[$videoLabel]copy[vout]")

        // Audio
        val audioMix = mutableListOf<String>()
        audioMix += "[0:a]"
        if (phonkIdx >= 0) {
            fc.append(";[$phonkIdx:a]volume=${options.phonkVolume / 100.0},")
            fc.append("adelay=${(climaxSec * 1000).toInt()}|${(climaxSec * 1000).toInt()}[_ph]")
            audioMix += "[_ph]"
        }

        val audioOut = if (audioMix.size == 1) {
            fc.append(";${audioMix.first()}anull[_aout]")
            "_aout"
        } else {
            fc.append(";${audioMix.joinToString("")}amix=inputs=${audioMix.size}:")
            fc.append("duration=first:normalize=0[_aout]")
            "_aout"
        }

        val out = File(tempDir, "phonkskull_${System.currentTimeMillis()}.mp4")
        val args = mutableListOf("-y", "-i", FfmpegExecutor.ff(input))
        if (glowFile != null) args += listOf("-loop", "1", "-i", FfmpegExecutor.ff(glowFile))
        if (skullFile != null) args += listOf("-loop", "1", "-i", FfmpegExecutor.ff(skullFile))
        if (phonkFile != null) args += listOf("-i", FfmpegExecutor.ff(phonkFile))
        args += listOf("-filter_complex", FfmpegExecutor.shellEscape(fc.toString()),
            "-map", "[vout]", "-map", "[$audioOut]")
        args += listOf("-t", "%.3f".format(Locale.US, outputDur),
            "-c:v", "libx264", "-preset", "medium", "-crf", "18",
            "-c:a", "aac", "-b:a", "192k", FfmpegExecutor.ff(out))

        progress(30, "Encoding video...")
        FfmpegExecutor.exec(args.joinToString(" ")) { p, msg ->
            progress(30 + (p * 0.65).toInt(), msg)
        }

        progress(100, "Done!")
        return ProcessResult(out.absolutePath)
    }

    private fun colorFilter(preset: ColorPreset): String = when (preset) {
        ColorPreset.NONE -> ""
        ColorPreset.MOODY ->
            "eq=brightness=-0.05:contrast=1.15:saturation=0.75,colorbalance=rs=-0.05:gs=0:bs=0.08,vignette=PI/3"
        ColorPreset.DARK_PHONK ->
            "eq=brightness=-0.1:contrast=1.3:saturation=0.6,colorbalance=rs=0.1:gs=-0.05:bs=-0.1,vignette=PI/2.5"
        ColorPreset.CINEMATIC ->
            "eq=brightness=0.02:contrast=1.1:saturation=0.85,colorbalance=rs=0.1:gs=0:bs=-0.1"
        ColorPreset.COLD_BLUE ->
            "eq=brightness=0:contrast=1.05:saturation=1.1,colorbalance=rs=-0.1:gs=0:bs=0.15"
        ColorPreset.VINTAGE ->
            "eq=brightness=-0.02:contrast=1.2:saturation=0.7,vignette=PI/4"
        ColorPreset.HIGH_CONTRAST ->
            "eq=brightness=0:contrast=1.4:saturation=1.2"
    }
}
