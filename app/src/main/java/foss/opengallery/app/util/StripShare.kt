package foss.opengallery.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Privacy share: copies images to cache, wipes GPS EXIF tags from the
 * copies, and shares those via FileProvider — the originals are untouched.
 *
 * Stripping is fail-safe: if ExifInterface cannot rewrite the format (PNG,
 * HEIC on some API levels), the copy is re-encoded without any metadata;
 * if that also fails the file is excluded from the share rather than
 * leaking location.
 */
object StripShare {

    private val GPS_TAGS = listOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
    )

    suspend fun buildIntent(context: Context, uris: List<Uri>): Intent? =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            // Clear stale share copies (best effort).
            dir.listFiles()?.filter {
                it.lastModified() < System.currentTimeMillis() - 60 * 60 * 1000
            }?.forEach { it.delete() }

            val shared = uris.mapNotNull { uri ->
                runCatching { prepareCopy(context, dir, uri) }.getOrNull()
            }
            if (shared.isEmpty()) return@withContext null

            val intent = if (shared.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, shared.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shared))
                }
            }
            Intent.createChooser(
                intent.apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                null,
            )
        }

    /** Copies one item, guarantees it carries no GPS, or returns null. */
    private fun prepareCopy(context: Context, dir: File, uri: Uri): Uri? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        var out = File(dir, "share_${System.nanoTime()}.$ext")
        resolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        if (!stripGpsVerified(out)) {
            val reencoded = reencodeWithoutMetadata(out)
            out.delete()
            out = reencoded ?: return null
        }
        return FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", out
        )
    }

    /** Strips GPS tags and re-reads the file to confirm they are gone. */
    private fun stripGpsVerified(file: File): Boolean {
        val hadGps = runCatching {
            ExifInterface(file.absolutePath).latLong != null
        }.getOrDefault(false)
        if (!hadGps) return true

        runCatching {
            val exif = ExifInterface(file.absolutePath)
            GPS_TAGS.forEach { exif.setAttribute(it, null) }
            exif.saveAttributes()
        }.getOrElse { return false }
        // saveAttributes can silently fail on formats it can't write.
        return runCatching {
            ExifInterface(file.absolutePath).latLong == null
        }.getOrDefault(false)
    }

    /** Last resort: decode + re-encode, which drops all metadata. */
    private fun reencodeWithoutMetadata(source: File): File? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_REENCODE_SIDE) {
            sample *= 2
        }
        val bitmap = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
        val out = File(source.parentFile, "share_${System.nanoTime()}.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        out
    }.getOrNull()

    private const val MAX_REENCODE_SIDE = 4096
}
