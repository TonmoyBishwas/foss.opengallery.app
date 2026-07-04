package foss.opengallery.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Hand-drawn line icons in the rounded One UI spirit — all original vector
 * drawing code, no imported icon fonts, keeping the APK tiny.
 *
 * Each function draws into a DrawScope sized by the caller (use with
 * [androidx.compose.foundation.Canvas]).
 */
object OgIcons {

    fun DrawScope.drawSearch(color: Color, strokeWidth: Float) {
        val r = size.minDimension * 0.32f
        val c = Offset(size.width * 0.44f, size.height * 0.44f)
        drawCircle(color, r, c, style = Stroke(strokeWidth))
        drawLine(
            color,
            Offset(c.x + r * 0.72f, c.y + r * 0.72f),
            Offset(size.width * 0.85f, size.height * 0.85f),
            strokeWidth,
            StrokeCap.Round,
        )
    }

    fun DrawScope.drawMore(color: Color) {
        val r = size.minDimension * 0.055f
        val x = size.width / 2f
        listOf(0.22f, 0.5f, 0.78f).forEach { fy ->
            drawCircle(color, r * 1.6f, Offset(x, size.height * fy))
        }
    }

    fun DrawScope.drawPlus(color: Color, strokeWidth: Float) {
        val pad = size.minDimension * 0.16f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(color, Offset(cx, pad), Offset(cx, size.height - pad), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(pad, cy), Offset(size.width - pad, cy), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawBack(color: Color, strokeWidth: Float) {
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w * 0.62f, h * 0.2f), Offset(w * 0.34f, h * 0.5f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.62f, h * 0.8f), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawMultiView(color: Color, strokeWidth: Float) {
        // Two overlapping rounded rectangles (the "view mode" toggle glyph).
        val s = size.minDimension
        val r = CornerRadius(s * 0.12f)
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.3f, s * 0.14f),
            size = Size(s * 0.56f, s * 0.56f),
            cornerRadius = r,
            style = Stroke(strokeWidth),
        )
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.14f, s * 0.3f),
            size = Size(s * 0.56f, s * 0.56f),
            cornerRadius = r,
            style = Stroke(strokeWidth),
        )
    }

    fun DrawScope.drawCheck(color: Color, strokeWidth: Float) {
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w * 0.22f, h * 0.52f), Offset(w * 0.42f, h * 0.72f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(w * 0.42f, h * 0.72f), Offset(w * 0.78f, h * 0.3f), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawPlay(color: Color) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.32f, h * 0.22f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.32f, h * 0.78f)
            close()
        }
        drawPath(path, color)
    }

    fun DrawScope.drawHeart(color: Color, filled: Boolean, strokeWidth: Float) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.08f, h * 0.52f, w * 0.16f, h * 0.16f, w * 0.5f, h * 0.34f)
            cubicTo(w * 0.84f, h * 0.16f, w * 0.92f, h * 0.52f, w * 0.5f, h * 0.82f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}
