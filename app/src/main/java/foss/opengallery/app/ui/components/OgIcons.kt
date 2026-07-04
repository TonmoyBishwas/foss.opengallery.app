package foss.opengallery.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

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
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.08f, h * 0.52f, w * 0.16f, h * 0.16f, w * 0.5f, h * 0.34f)
            cubicTo(w * 0.84f, h * 0.16f, w * 0.92f, h * 0.52f, w * 0.5f, h * 0.82f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }

    fun DrawScope.drawClose(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawLine(color, Offset(s * 0.26f, s * 0.26f), Offset(s * 0.74f, s * 0.74f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(s * 0.74f, s * 0.26f), Offset(s * 0.26f, s * 0.74f), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawShare(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val a = Offset(s * 0.72f, s * 0.2f)
        val b = Offset(s * 0.26f, s * 0.5f)
        val c = Offset(s * 0.72f, s * 0.8f)
        drawLine(color, a, b, strokeWidth)
        drawLine(color, b, c, strokeWidth)
        val r = s * 0.1f
        listOf(a, b, c).forEach { drawCircle(color, r, it) }
    }

    fun DrawScope.drawTrash(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawLine(color, Offset(s * 0.16f, s * 0.26f), Offset(s * 0.84f, s * 0.26f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(s * 0.4f, s * 0.26f), Offset(s * 0.44f, s * 0.14f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(s * 0.44f, s * 0.14f), Offset(s * 0.56f, s * 0.14f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(s * 0.56f, s * 0.14f), Offset(s * 0.6f, s * 0.26f), strokeWidth, StrokeCap.Round)
        val body = Path().apply {
            moveTo(s * 0.25f, s * 0.34f)
            lineTo(s * 0.3f, s * 0.84f)
            lineTo(s * 0.7f, s * 0.84f)
            lineTo(s * 0.75f, s * 0.34f)
        }
        drawPath(body, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    fun DrawScope.drawPencil(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.25f, s * 0.62f)
            lineTo(s * 0.69f, s * 0.18f)
            lineTo(s * 0.82f, s * 0.31f)
            lineTo(s * 0.38f, s * 0.75f)
            lineTo(s * 0.19f, s * 0.81f)
            close()
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color, Offset(s * 0.25f, s * 0.62f), Offset(s * 0.38f, s * 0.75f), strokeWidth * 0.8f, StrokeCap.Round)
    }

    fun DrawScope.drawInfo(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawCircle(color, s * 0.38f, center, style = Stroke(strokeWidth))
        drawCircle(color, strokeWidth * 0.72f, Offset(s * 0.5f, s * 0.33f))
        drawLine(color, Offset(s * 0.5f, s * 0.46f), Offset(s * 0.5f, s * 0.68f), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawVideos(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.1f, s * 0.18f),
            size = Size(s * 0.8f, s * 0.64f),
            cornerRadius = CornerRadius(s * 0.14f),
            style = Stroke(strokeWidth),
        )
        val tri = Path().apply {
            moveTo(s * 0.42f, s * 0.37f)
            lineTo(s * 0.65f, s * 0.5f)
            lineTo(s * 0.42f, s * 0.63f)
            close()
        }
        drawPath(tri, color)
    }

    fun DrawScope.drawClock(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawCircle(color, s * 0.38f, center, style = Stroke(strokeWidth))
        drawLine(color, center, Offset(s * 0.5f, s * 0.29f), strokeWidth, StrokeCap.Round)
        drawLine(color, center, Offset(s * 0.66f, s * 0.58f), strokeWidth, StrokeCap.Round)
    }

    fun DrawScope.drawSparkle(color: Color) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.08f)
            quadraticTo(s * 0.56f, s * 0.44f, s * 0.92f, s * 0.5f)
            quadraticTo(s * 0.56f, s * 0.56f, s * 0.5f, s * 0.92f)
            quadraticTo(s * 0.44f, s * 0.56f, s * 0.08f, s * 0.5f)
            quadraticTo(s * 0.44f, s * 0.44f, s * 0.5f, s * 0.08f)
            close()
        }
        drawPath(path, color)
    }

    fun DrawScope.drawPin(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val c = Offset(s * 0.5f, s * 0.38f)
        val r = s * 0.24f
        drawCircle(color, r, c, style = Stroke(strokeWidth))
        drawLine(color, Offset(c.x - r * 0.7f, c.y + r * 0.7f), Offset(s * 0.5f, s * 0.88f), strokeWidth, StrokeCap.Round)
        drawLine(color, Offset(c.x + r * 0.7f, c.y + r * 0.7f), Offset(s * 0.5f, s * 0.88f), strokeWidth, StrokeCap.Round)
        drawCircle(color, strokeWidth * 0.62f, c)
    }

    fun DrawScope.drawGear(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawCircle(color, s * 0.24f, center, style = Stroke(strokeWidth))
        drawCircle(color, s * 0.09f, center, style = Stroke(strokeWidth))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0 + 22.5)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                color,
                center + Offset(dir.x * s * 0.24f, dir.y * s * 0.24f),
                center + Offset(dir.x * s * 0.36f, dir.y * s * 0.36f),
                strokeWidth,
                StrokeCap.Round,
            )
        }
    }

    fun DrawScope.drawLock(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.22f, s * 0.44f),
            size = Size(s * 0.56f, s * 0.42f),
            cornerRadius = CornerRadius(s * 0.1f),
            style = Stroke(strokeWidth),
        )
        drawArc(
            color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.34f, s * 0.16f),
            size = Size(s * 0.32f, s * 0.46f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawCircle(color, strokeWidth * 0.62f, Offset(s * 0.5f, s * 0.64f))
    }

    /** Curved undo arrow; mirrored draws the redo variant. */
    fun DrawScope.drawUndoArrow(color: Color, strokeWidth: Float, mirrored: Boolean = false) {
        withTransform({ if (mirrored) scale(-1f, 1f) }) {
            val s = size.minDimension
            val r = s * 0.32f
            val c = center
            val endDeg = 165.0
            drawArc(
                color,
                startAngle = -80f,
                sweepAngle = (endDeg + 80).toFloat(),
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            val a = Math.toRadians(endDeg)
            val tip = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            val dir = Offset(-sin(a).toFloat(), cos(a).toFloat())
            arrowHead(color, tip, dir, s * 0.17f, strokeWidth)
        }
    }

    fun DrawScope.drawCrop(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val p1 = Path().apply {
            moveTo(s * 0.3f, s * 0.1f)
            lineTo(s * 0.3f, s * 0.7f)
            lineTo(s * 0.9f, s * 0.7f)
        }
        val p2 = Path().apply {
            moveTo(s * 0.1f, s * 0.3f)
            lineTo(s * 0.7f, s * 0.3f)
            lineTo(s * 0.7f, s * 0.9f)
        }
        drawPath(p1, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(p2, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    fun DrawScope.drawFilterTriad(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val r = s * 0.2f
        drawCircle(color, r, Offset(s * 0.5f, s * 0.32f), style = Stroke(strokeWidth))
        drawCircle(color, r, Offset(s * 0.33f, s * 0.62f), style = Stroke(strokeWidth))
        drawCircle(color, r, Offset(s * 0.67f, s * 0.62f), style = Stroke(strokeWidth))
    }

    fun DrawScope.drawTone(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawCircle(color, s * 0.17f, center, style = Stroke(strokeWidth))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                color,
                center + Offset(dir.x * s * 0.27f, dir.y * s * 0.27f),
                center + Offset(dir.x * s * 0.38f, dir.y * s * 0.38f),
                strokeWidth,
                StrokeCap.Round,
            )
        }
    }

    fun DrawScope.drawDecorate(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        val c = Offset(s * 0.46f, s * 0.48f)
        val r = s * 0.3f
        drawArc(
            color,
            startAngle = 25f,
            sweepAngle = 315f,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawCircle(color, strokeWidth * 0.62f, Offset(c.x - r * 0.36f, c.y - r * 0.22f))
        drawCircle(color, strokeWidth * 0.62f, Offset(c.x + r * 0.36f, c.y - r * 0.22f))
        drawArc(
            color,
            startAngle = 35f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.45f, c.y - r * 0.45f),
            size = Size(r * 0.9f, r * 0.9f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(s * 0.7f, s * 0.7f), Offset(s * 0.88f, s * 0.88f), strokeWidth * 1.5f, StrokeCap.Round)
    }

    fun DrawScope.drawFlip(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        var y = s * 0.14f
        while (y < s * 0.84f) {
            drawLine(color, Offset(s * 0.5f, y), Offset(s * 0.5f, y + s * 0.07f), strokeWidth * 0.8f, StrokeCap.Round)
            y += s * 0.15f
        }
        val left = Path().apply {
            moveTo(s * 0.38f, s * 0.3f)
            lineTo(s * 0.38f, s * 0.7f)
            lineTo(s * 0.12f, s * 0.7f)
            close()
        }
        val right = Path().apply {
            moveTo(s * 0.62f, s * 0.3f)
            lineTo(s * 0.62f, s * 0.7f)
            lineTo(s * 0.88f, s * 0.7f)
            close()
        }
        drawPath(left, color)
        drawPath(right, color, style = Stroke(strokeWidth, join = StrokeJoin.Round))
    }

    fun DrawScope.drawRotate(color: Color, strokeWidth: Float) {
        val s = size.minDimension
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.14f, s * 0.36f),
            size = Size(s * 0.5f, s * 0.5f),
            cornerRadius = CornerRadius(s * 0.09f),
            style = Stroke(strokeWidth),
        )
        val c = Offset(s * 0.56f, s * 0.36f)
        val r = s * 0.28f
        val endDeg = -35.0
        drawArc(
            color,
            startAngle = -160f,
            sweepAngle = (endDeg + 160).toFloat(),
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        val a = Math.toRadians(endDeg)
        val tip = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
        val dir = Offset(-sin(a).toFloat(), cos(a).toFloat())
        arrowHead(color, tip, dir, s * 0.15f, strokeWidth)
    }

    private fun DrawScope.arrowHead(color: Color, tip: Offset, dir: Offset, len: Float, strokeWidth: Float) {
        fun rot(v: Offset, deg: Double): Offset {
            val a = Math.toRadians(deg)
            return Offset(
                (v.x * cos(a) - v.y * sin(a)).toFloat(),
                (v.x * sin(a) + v.y * cos(a)).toFloat(),
            )
        }
        val back = Offset(-dir.x, -dir.y)
        val w1 = rot(back, 30.0)
        val w2 = rot(back, -30.0)
        drawLine(color, tip, tip + Offset(w1.x * len, w1.y * len), strokeWidth, StrokeCap.Round)
        drawLine(color, tip, tip + Offset(w2.x * len, w2.y * len), strokeWidth, StrokeCap.Round)
    }
}
