package foss.opengallery.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Privacy share: copies images to cache, wipes GPS EXIF tags from the
 * copies, and shares those via FileProvider — the originals are untouched.
 */
object StripShare {

    suspend fun buildIntent(context: Context, uris: List<Uri>): Intent? =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            // Clear stale share copies (best effort).
            dir.listFiles()?.filter {
                it.lastModified() < System.currentTimeMillis() - 60 * 60 * 1000
            }?.forEach { it.delete() }

            val shared = uris.mapNotNull { uri ->
                runCatching {
                    val out = File(dir, "share_${System.nanoTime()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching null
                    stripGps(out)
                    FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", out
                    )
                }.getOrNull()
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

    private fun stripGps(file: File) {
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            listOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ).forEach { exif.setAttribute(it, null) }
            exif.saveAttributes()
        }
    }
}
