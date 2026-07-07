package foss.opengallery.app.data.trash

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
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
        resolver.query(MediaQuery.filesUri, TRASH_PROJECTION, args, null)?.use { c ->
            val reader = MediaQuery.RowReader(c)
            while (c.moveToNext()) items.add(reader.read())
        }
        items
    }

    /** Outcome of one legacy trash attempt. */
    sealed interface LegacyTrashOutcome {
        object Done : LegacyTrashOutcome
        object Failed : LegacyTrashOutcome

        /** API 29 non-owned item: launch, then retry the same uri on OK. */
        data class NeedsConsent(val sender: IntentSender) : LegacyTrashOutcome
    }

    /**
     * Legacy (26–29) delete path: copy into the app bin, then delete the
     * original. Copy-before-delete so a failed delete never loses data;
     * the bin record is only written after the original is actually gone.
     */
    suspend fun legacyTrashUri(uri: Uri): LegacyTrashOutcome = withContext(Dispatchers.IO) {
        val id = runCatching { ContentUris.parseId(uri) }.getOrNull()
            ?: return@withContext LegacyTrashOutcome.Failed
        val item = MediaQuery.queryPage(
            resolver, offset = 0, limit = 1,
            selection = "${MediaStore.MediaColumns._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull() ?: return@withContext LegacyTrashOutcome.Failed
        legacyTrash(item)
    }

    /** Legacy (26–29): move a file into the app bin. */
    suspend fun legacyTrash(item: MediaItem): LegacyTrashOutcome = withContext(Dispatchers.IO) {
        val stored = File(trashDir, "${System.nanoTime()}_${item.displayName}")
        val copied = runCatching {
            resolver.openInputStream(item.uri)?.use { input ->
                stored.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            stored.delete()
            return@withContext LegacyTrashOutcome.Failed
        }
        // Keep the original timestamp on the copy so restore can put it back.
        if (item.dateModifiedSeconds > 0) {
            stored.setLastModified(item.dateModifiedSeconds * 1000)
        }
        try {
            if (resolver.delete(item.uri, null, null) <= 0) {
                stored.delete()
                return@withContext LegacyTrashOutcome.Failed
            }
        } catch (e: SecurityException) {
            stored.delete()
            return@withContext if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
                LegacyTrashOutcome.NeedsConsent(e.userAction.actionIntent.intentSender)
            } else {
                LegacyTrashOutcome.Failed
            }
        }
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
        LegacyTrashOutcome.Done
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
                // Restore into the original folder and keep the original dates
                // (the stored copy carries the pre-trash mtime).
                val originalMillis = stored.lastModified()
                if (originalMillis > 0) {
                    put(MediaStore.MediaColumns.DATE_ADDED, originalMillis / 1000)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, originalMillis / 1000)
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    if (entity.originalRelativePath.isNotBlank()) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, entity.originalRelativePath)
                    }
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else if (entity.originalRelativePath.isNotBlank()) {
                    @Suppress("DEPRECATION")
                    put(
                        MediaStore.MediaColumns.DATA,
                        entity.originalRelativePath + entity.originalName,
                    )
                }
            }
            val collection: Uri =
                if (entity.mimeType.startsWith("video/"))
                    @Suppress("DEPRECATION") MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else
                    @Suppress("DEPRECATION") MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            // Insert can throw (e.g. API 29 rejects RELATIVE_PATH pointing at
            // a directory the collection doesn't allow, like Download/ for
            // images) — retry once without the original path.
            val target = runCatching { resolver.insert(collection, values) }
                .getOrNull()
                ?: runCatching {
                    values.remove(MediaStore.MediaColumns.RELATIVE_PATH)
                    @Suppress("DEPRECATION")
                    values.remove(MediaStore.MediaColumns.DATA)
                    resolver.insert(collection, values)
                }.getOrNull()
                ?: return@withContext false
            // Only delete the stored copy after a VERIFIED write: a null
            // output stream or failed copy must never destroy the last copy.
            val written = runCatching {
                resolver.openOutputStream(target)?.use { output ->
                    stored.inputStream().use { input -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (!written) {
                runCatching { resolver.delete(target, null, null) }
                return@withContext false
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.update(
                        target,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null, null,
                    )
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

        /** Columns RowReader consumes — this query only runs on API 30+. */
        val TRASH_PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            "datetaken",
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            "duration",
            "bucket_id",
            "bucket_display_name",
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.IS_FAVORITE,
        )
    }
}
