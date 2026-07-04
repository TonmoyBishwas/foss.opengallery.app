package foss.opengallery.app.ui.screens.viewer

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ExifDetails(
    val camera: String?,
    val aperture: String?,
    val exposure: String?,
    val iso: String?,
    val focalLength: String?,
)

/** The viewer's info sheet: date, file, size/resolution, camera EXIF. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var exif by remember { mutableStateOf<ExifDetails?>(null) }

    LaunchedEffect(item.id) {
        exif = readExif(context, item)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OgColors.SurfaceSheet,
        shape = foss.opengallery.app.ui.theme.OgShapes.Sheet,
    ) {
        Column(Modifier.padding(horizontal = 26.dp).padding(bottom = 34.dp)) {
            Text(
                text = formatDate(item.takenAtMillis),
                style = OgType.SectionHeader,
                color = OgColors.TextPrimary,
            )
            Text(
                text = item.displayName,
                style = OgType.Body,
                color = OgColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "/${item.relativePath}",
                style = OgType.Body,
                color = OgColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )

            Text(
                text = if (item.isVideo) "Video info" else "Image info",
                style = OgType.ItemLabel,
                color = OgColors.TextPrimary,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = formatSize(item.sizeBytes),
                    style = OgType.Body,
                    color = OgColors.TextSecondary,
                )
                if (item.width > 0 && item.height > 0) {
                    Text(
                        text = "   ${item.width}x${item.height}",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                    )
                }
                if (item.isVideo && item.durationMs > 0) {
                    Text(
                        text = "   ${item.durationMs / 1000}s",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                    )
                }
            }

            exif?.let { details ->
                if (details.camera != null) {
                    Text(
                        text = "Camera",
                        style = OgType.ItemLabel,
                        color = OgColors.TextPrimary,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = listOfNotNull(
                            details.camera,
                            details.aperture,
                            details.exposure,
                            details.iso,
                            details.focalLength,
                        ).joinToString("   "),
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

private suspend fun readExif(context: Context, item: MediaItem): ExifDetails? =
    withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext null
        runCatching {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                ExifDetails(
                    camera = listOfNotNull(make, model).joinToString(" ").ifBlank { null },
                    aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                        ?.toDoubleOrNull()
                        ?.let { if (it < 1) "1/${(1 / it).toInt()}s" else "${it}s" },
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        ?.let { "ISO $it" },
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                        ?.split("/")
                        ?.let { parts ->
                            val num = parts.getOrNull(0)?.toDoubleOrNull()
                            val den = parts.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                            if (num != null && den != 0.0) "%.0fmm".format(num / den) else null
                        },
                )
            }
        }.getOrNull()
    }

private fun formatDate(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · h:mm a", Locale.getDefault())
    return fmt.format(dt)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1L shl 20 -> "%.2f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1L shl 10 -> "%.2f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
