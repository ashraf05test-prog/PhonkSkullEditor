package com.phonkskull.editor.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.phonkskull.editor.data.GlowTextOptions
import com.phonkskull.editor.data.TextGlowColor
import java.io.File

object GlowTextRenderer {

    fun render(
        context: Context,
        options: GlowTextOptions,
        frameW: Int,
        frameH: Int,
        tempDir: File,
    ): File {
        val bmp = Bitmap.createBitmap(frameW, (frameH * 0.15f).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val glowRgb = when (options.glowColor) {
            TextGlowColor.RED -> Color.RED
            TextGlowColor.GREEN -> Color.GREEN
        }

        // Draw glow layers (blur simulation via multiple strokes)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = options.fontSize * (frameW / 540f)
            color = glowRgb
            alpha = 80
            strokeWidth = 20f
            style = Paint.Style.STROKE
            setShadowLayer(30f, 0f, 0f, glowRgb)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = options.fontSize * (frameW / 540f)
            color = Color.WHITE
            setShadowLayer(15f, 0f, 0f, glowRgb)
        }

        val text = options.text.uppercase()
        val textW = textPaint.measureText(text)
        val x = (frameW - textW) / 2f
        val y = bmp.height * 0.65f

        // Multiple glow passes
        repeat(4) {
            glowPaint.alpha = 40 + it * 15
            canvas.drawText(text, x, y, glowPaint)
        }
        canvas.drawText(text, x, y, textPaint)

        val outFile = File(tempDir, "glowtext_${System.currentTimeMillis()}.png")
        outFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return outFile
    }
}
