package foss.opengallery.app.data.trash

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.db.OgDatabase
import foss.opengallery.app.data.db.TrashedItemEntity
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Recycle bin backend.
 *
 * API 30+: the system trash (IS_TRASHED / createTrashRequest) — queried
 * with QUERY_ARG_MATCH_TRASHED, restored/emptied via consent dialogs.
 *
 * API 26–29: an app-managed bin — files are moved into app storage with a
 * Room record and can be re-inserted into MediaStore on restore. Items
 * older than 30 days are purged on access.
 */
class TrashRepository(
    private val context: Context,
    private val db: OgDatabase,
) {
    private val resolver = context.contentResolver
    private val trashDir: File by lazy {
        File(context.filesDir, "trash").apply { mkdirs() }
    }

    val legacyItems: Flow<List<TrashedItemEntity>> = db.trashDao().observeAll()

    val usesSystemTrash: Boolean get() = Build.VERSION.SDK_INT >= 30

    /** Trashed media on API 30+. */
    suspend fun systemTrashedItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < 30) return@withContext emptyList()
        val items = ArrayList<MediaItem>()
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putString(
                android.content.ContentResolver.QUERY_ARG_SQL_SELECTION,
                MediaQuery.MEDIA_TYPE_SELECTION,
            )
            putString(
                android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
            )
        }
        resolver.query(MediaQuery.filesUri, null, args, null)?.use { c ->
            val reader = MediaQuery.RowReader(c)
            while (c.moveToNext()) items.add(reader.read())
        }
        items
    }

    /** Legacy (26–29): move a file into the app bin. */
    suspend fun legacyTrash(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val stored = File(trashDir, "${System.nanoTime()}_${item.displayName}")
        runCatching {
            resolver.openInputStream(item.uri)?.use { input ->
                stored.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext false
            resolver.delete(item.uri, null, null)
            db.trashDao().insert(
                TrashedItemEntity(
                    originalName = item.displayName,
                    originalRelativePath = item.relativePath,
                    mimeType = item.mimeType,
                    sizeBytes = item.sizeBytes,
                    trashedAtMillis = System.currentTimeMillis(),
                    storedPath = stored.absolutePath,
                )
            )
            true
        }.getOrDefault(false)
    }

    /** Legacy restore: re-insert into MediaStore and drop the record. */
    suspend fun legacyRestore(entity: TrashedItemEntity): Boolean =
        withContext(Dispatchers.IO) {
            val stored = File(entity.storedPath)
            if (!stored.exists()) {
                db.trashDao().delete(entity.id)
                return@withContext false
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, entity.originalName)
                put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
            }
            val collection: Uri =
                if (entity.mimeType.startsWith("video/"))
                    @Suppress("DEPRECATION") MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else
                    @Suppress("DEPRECATION") MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val target = resolver.insert(collection, values) ?: return@withContext false
            runCatching {
                resolver.openOutputStream(target)?.use { output ->
                    stored.inputStream().use { input -> input.copyTo(output) }
                }
                stored.delete()
                db.trashDao().delete(entity.id)
                true
            }.getOrDefault(false)
        }

    suspend fun legacyDeleteForever(entity: TrashedItemEntity) = withContext(Dispatchers.IO) {
        File(entity.storedPath).delete()
        db.trashDao().delete(entity.id)
    }

    /** Purge legacy items past the 30-day window. */
    suspend fun purgeExpired() = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 30) return@withContext
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        db.trashDao().olderThan(cutoff).forEach { legacyDeleteForever(it) }
    }

    private companion object {
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
