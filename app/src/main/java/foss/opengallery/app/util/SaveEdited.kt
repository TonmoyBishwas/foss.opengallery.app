package foss.opengallery.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import foss.opengallery.app.ui.screens.editor.EditState
import foss.opengallery.app.ui.screens.editor.StickerKind
import foss.opengallery.app.ui.screens.editor.ToneKey
import foss.opengallery.app.ui.screens.editor.ToneMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-resolution render + non-destructive save.
 *
 * The editor NEVER overwrites the original: output is a new MediaStore row
 * written with the IS_PENDING two-phase pattern, and EXIF metadata is
 * copied from the source (a top complaint about stock editors is losing
 * it). Orientation is reset because rotation is baked into the pixels.
 */
object SaveEdited {

    suspend fun render(source: Bitmap, state: EditState): Bitmap =
        withContext(Dispatchers.Default) {
            var bitmap = source

            // 1) Geometry: straighten -> rotate90/flip -> crop.
            if (state.straighten != 0f) {
                val m = android.graphics.Matrix().apply { postRotate(state.straighten) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }
            if (state.rotate90 != 0 || state.flipHorizontal) {
                val m = android.graphics.Matrix().apply {
                    if (state.flipHorizontal) postScale(-1f, 1f)
                    postRotate(90f * state.rotate90)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
            }
            if (state.crop != androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)) {
                val left = (state.crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                val top = (state.crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                val w = (state.crop.width * bitmap.width).toInt().coerceIn(1, bitmap.width - left)
                val h = (state.crop.height * bitmap.height).toInt().coerceIn(1, bitmap.height - top)
                bitmap = Bitmap.createBitmap(bitmap, left, top, w, h)
            }

            // 2) Color: the same matrix the live preview showed.
            val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(ToneMatrix.build(state))
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            // 3) Vignette.
            val vignette = state.tone(ToneKey.Vignette)
            if (vignette > 0f) {
                val radius = maxOf(out.width, out.height) * 0.75f
                val alpha = (vignette / 100f * 200f).toInt().coerceIn(0, 255)
                val p = Paint().apply {
                    shader = RadialGradient(
                        out.width / 2f, out.height / 2f, radius,
                        intArrayOf(0x00000000, alpha shl 24),
                        floatArrayOf(0.55f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), p)
            }

            // 4) Overlays (normalized coords scale to output size).
            drawOverlays(canvas, out.width.toFloat(), out.height.toFloat(), state)
            out
        }

    fun drawOverlays(canvas: Canvas, w: Float, h: Float, state: EditState) {
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        state.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            strokePaint.color = stroke.color.toArgbCompat()
            strokePaint.strokeWidth = stroke.widthFraction * w
            strokePaint.alpha = if (stroke.isHighlighter) 110 else 255
            val path = Path()
            path.moveTo(stroke.points.first().x * w, stroke.points.first().y * h)
            stroke.points.drop(1).forEach { p -> path.lineTo(p.x * w, p.y * h) }
            canvas.drawPath(path, strokePaint)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        state.texts.forEach { overlay ->
            textPaint.color = overlay.color.toArgbCompat()
            textPaint.textSize = 0.06f * w * overlay.scale
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                overlay.text,
                overlay.center.x * w,
                overlay.center.y * h,
                textPaint,
            )
        }

        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 0.008f * w
            strokeCap = Paint.Cap.ROUND
        }
        state.stickers.forEach { sticker ->
            val cx = sticker.center.x * w
            val cy = sticker.center.y * h
            val r = 0.08f * w * sticker.scale
            drawSticker(canvas, sticker.kind, cx, cy, r, stickerPaint)
        }
    }

    fun drawSticker(canvas: Canvas, kind: StickerKind, cx: Float, cy: Float, r: Float, p: Paint) {
        when (kind) {
            StickerKind.Arrow -> {
                canvas.drawLine(cx - r, cy, cx + r, cy, p)
                canvas.drawLine(cx + r, cy, cx + r * 0.4f, cy - r * 0.5f, p)
                canvas.drawLine(cx + r, cy, cx + r * 0.4f, cy + r * 0.5f, p)
            }
            StickerKind.Check -> {
                canvas.drawLine(cx - r * 0.8f, cy, cx - r * 0.2f, cy + r * 0.6f, p)
                canvas.drawLine(cx - r * 0.2f, cy + r * 0.6f, cx + r * 0.9f, cy - r * 0.6f, p)
            }
            StickerKind.Cross -> {
                canvas.drawLine(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, p)
                canvas.drawLine(cx + r * 0.7f, cy - r * 0.7f, cx - r * 0.7f, cy + r * 0.7f, p)
            }
            StickerKind.CircleOutline -> canvas.drawCircle(cx, cy, r * 0.85f, p)
            StickerKind.SquareOutline ->
                canvas.drawRect(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f, p)
            StickerKind.Star -> {
                val path = Path()
                for (i in 0 until 10) {
                    val angle = Math.PI / 2 + i * Math.PI / 5
                    val radius = if (i % 2 == 0) r else r * 0.45f
                    val x = cx + (radius * Math.cos(angle)).toFloat()
                    val y = cy - (radius * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                canvas.drawPath(path, p)
            }
            StickerKind.Heart -> {
                val path = Path()
                path.moveTo(cx, cy + r * 0.7f)
                path.cubicTo(cx - r * 1.1f, cy - r * 0.1f, cx - r * 0.5f, cy - r, cx, cy - r * 0.35f)
                path.cubicTo(cx + r * 0.5f, cy - r, cx + r * 1.1f, cy - r * 0.1f, cx, cy + r * 0.7f)
                canvas.drawPath(path, p)
            }
            StickerKind.Burst -> {
                for (i in 0 until 8) {
                    val angle = i * Math.PI / 4
                    canvas.drawLine(
                        cx + (r * 0.45f * Math.cos(angle)).toFloat(),
                        cy + (r * 0.45f * Math.sin(angle)).toFloat(),
                        cx + (r * Math.cos(angle)).toFloat(),
                        cy + (r * Math.sin(angle)).toFloat(),
                        p,
                    )
                }
            }
        }
    }

    /** Two-phase MediaStore insert + EXIF copy. Returns the new item URI. */
    suspend fun saveAsCopy(
        context: Context,
        sourceUri: Uri,
        sourceName: String,
        rendered: Bitmap,
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val base = sourceName.substringBeforeLast('.')
        val newName = "${base}_edited_${System.currentTimeMillis() / 1000}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/OpenGallery")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION") MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val target = resolver.insert(collection, values) ?: return@withContext null

        runCatching {
            resolver.openOutputStream(target)?.use { out ->
                rendered.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            copyExif(context, sourceUri, target)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                resolver.update(
                    target,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null, null,
                )
            }
            target
        }.getOrElse {
            resolver.delete(target, null, null)
            null
        }
    }

    /** Copies the important EXIF tags from source onto the saved copy. */
    private fun copyExif(context: Context, source: Uri, target: Uri) {
        runCatching {
            val tags = listOf(
                ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
            )
            val values = HashMap<String, String>()
            context.contentResolver.openInputStream(source)?.use { input ->
                val src = ExifInterface(input)
                tags.forEach { tag -> src.getAttribute(tag)?.let { values[tag] = it } }
            }
            if (values.isEmpty()) return
            context.contentResolver.openFileDescriptor(target, "rw")?.use { pfd ->
                val dst = ExifInterface(pfd.fileDescriptor)
                values.forEach { (tag, value) -> dst.setAttribute(tag, value) }
                dst.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL.toString(),
                )
                dst.saveAttributes()
            }
        }
    }

    private fun androidx.compose.ui.graphics.Color.toArgbCompat(): Int =
        android.graphics.Color.argb(
            (alpha * 255).toInt(), (red * 255).toInt(),
            (green * 255).toInt(), (blue * 255).toInt(),
        )
}
