package com.phonkskull.editor.media

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FfmpegExecutor {

    fun ff(file: File): String = shellEscape(file.absolutePath)

    fun shellEscape(s: String): String =
        if (s.none { it == '\'' || it == ' ' || it == '(' || it == ')' || it == '[' || it == ']' }) s
        else "'${s.replace("'", "'\\''")}'"

    suspend fun exec(
        cmd: String,
        onProgress: (Int, String) -> Unit = { _, _ -> },
    ) = suspendCancellableCoroutine { cont ->
        val session = FFmpegKit.executeAsync(cmd, { session ->
            if (ReturnCode.isSuccess(session.returnCode)) cont.resume(Unit)
            else cont.resumeWithException(
                RuntimeException("FFmpeg failed [rc=${session.returnCode}]: ${session.failStackTrace}")
            )
        }, { log ->
            // parse progress from ffmpeg stderr if needed
        }, { stats ->
            val pct = ((stats.time / 1000.0) * 100).toInt().coerceIn(0, 99)
            onProgress(pct, "Encoding...")
        })
        cont.invokeOnCancellation { session.cancel() }
    }

    fun probeSize(file: File): Pair<Int, Int> {
        val session = FFprobeKit.execute(
            "-v error -select_streams v:0 -show_entries stream=width,height " +
                    "-of csv=p=0 ${ff(file)}"
        )
        val out = session.output?.trim() ?: return 1080 to 1920
        val parts = out.split(",")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 1080) to
                (parts.getOrNull(1)?.toIntOrNull() ?: 1920)
    }

    fun probeDuration(file: File): Double {
        val session = FFprobeKit.execute(
            "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 ${ff(file)}"
        )
        return session.output?.trim()?.toDoubleOrNull() ?: 0.0
    }
}
