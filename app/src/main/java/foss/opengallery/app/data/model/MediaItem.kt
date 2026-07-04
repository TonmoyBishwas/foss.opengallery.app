package foss.opengallery.app.data.model

import android.net.Uri

/** One row of the timeline/grid: a photo or a video from MediaStore. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    /** Milliseconds since epoch: DATE_TAKEN if present, else DATE_ADDED. */
    val takenAtMillis: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    /** Video duration in ms, 0 for images. */
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String,
    val isFavorite: Boolean,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isGif: Boolean get() = mimeType == "image/gif"
}

/** A folder-backed album (MediaStore bucket). */
data class Bucket(
    val bucketId: Long,
    val name: String,
    val itemCount: Int,
    val coverItem: MediaItem?,
    val relativePath: String,
)
