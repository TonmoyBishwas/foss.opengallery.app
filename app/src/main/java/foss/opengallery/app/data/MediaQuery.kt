package foss.opengallery.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import foss.opengallery.app.data.model.MediaItem

/**
 * Low-level MediaStore access. Narrow projections only (CursorWindow is
 * ~2 MB); all queries run off the main thread by callers.
 */
object MediaQuery {

    val filesUri: Uri =
        if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Files.getContentUri("external")

    /** Selection limiting the Files collection to images + videos. */
    const val MEDIA_TYPE_SELECTION =
        "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (" +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}," +
            "${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"

    private val PROJECTION: Array<String> = buildList {
        add(MediaStore.MediaColumns._ID)
        add(MediaStore.MediaColumns.DISPLAY_NAME)
        add(MediaStore.MediaColumns.MIME_TYPE)
        add(MediaStore.MediaColumns.DATE_ADDED)
        add(MediaStore.MediaColumns.DATE_MODIFIED)
        add("datetaken")
        add(MediaStore.MediaColumns.SIZE)
        add(MediaStore.MediaColumns.WIDTH)
        add(MediaStore.MediaColumns.HEIGHT)
        add("duration")
        add("bucket_id")
        add("bucket_display_name")
        if (Build.VERSION.SDK_INT >= 29) {
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.IS_FAVORITE)
        } else {
            @Suppress("DEPRECATION")
            add(MediaStore.MediaColumns.DATA)
        }
    }.toTypedArray()

    /** Default sort: newest first by the moment the file landed on device. */
    const val SORT_DATE_ADDED_DESC = "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"

    fun queryPage(
        resolver: ContentResolver,
        offset: Int,
        limit: Int,
        selection: String = MEDIA_TYPE_SELECTION,
        selectionArgs: Array<String>? = null,
        sortOrder: String = SORT_DATE_ADDED_DESC,
    ): List<MediaItem> {
        val items = ArrayList<MediaItem>(limit)
        // LIMIT/OFFSET must reach SQLite, otherwise every page walks the whole
        // table through 2 MB cursor windows and deep scrolling turns O(n²).
        // Three grammars: 30+ has public paging args; 29 enforces strict sort
        // grammar (LIMIT in sortOrder throws) but honors the SQL-limit arg;
        // 26–28 pass the sort string straight to SQLite.
        val cursor = when {
            Build.VERSION.SDK_INT >= 30 -> {
                val args = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    selectionArgs?.let {
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it)
                    }
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                }
                resolver.query(filesUri, PROJECTION, args, null)
            }
            Build.VERSION.SDK_INT == 29 -> {
                val args = Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    selectionArgs?.let {
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, it)
                    }
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    // Public constants only exist from 30, but Android 10's
                    // MediaProvider honors the SQL-limit query arg.
                    putString("android:query-arg-sql-limit", "$limit OFFSET $offset")
                }
                resolver.query(filesUri, PROJECTION, args, null)
            }
            else -> resolver.query(
                filesUri, PROJECTION, selection, selectionArgs,
                "$sortOrder LIMIT $limit OFFSET $offset",
            )
        }
        cursor?.use { c ->
            // Belt and braces for 29: if an OEM provider ignored the limit
            // arg the cursor holds the full result — skip rows manually so
            // pages stay correct (slow, but never wrong).
            val pagingIgnored =
                Build.VERSION.SDK_INT == 29 && offset > 0 && c.count > limit
            val positioned = if (pagingIgnored) c.moveToPosition(offset) else c.moveToFirst()
            if (!positioned) return items
            val reader = RowReader(c)
            do {
                items.add(reader.read())
            } while (items.size < limit && c.moveToNext())
        }
        return items
    }

    /**
     * Ids + typed content URIs of every row matching [selection], in sort
     * order. Narrow two-column projection so "Select all" over a huge
     * library stays cheap.
     */
    fun queryAllUris(
        resolver: ContentResolver,
        selection: String = MEDIA_TYPE_SELECTION,
        selectionArgs: Array<String>? = null,
        sortOrder: String = SORT_DATE_ADDED_DESC,
    ): List<Pair<Long, Uri>> {
        val result = ArrayList<Pair<Long, Uri>>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
        resolver.query(filesUri, projection, selection, selectionArgs, sortOrder)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val isVideo =
                    c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                result.add(id to ContentUris.withAppendedId(collectionUri(isVideo), id))
            }
        }
        return result
    }

    /**
     * Selection matching exactly [ids] (plus the media-type filter). The
     * single shared builder for custom-album membership — keep the album
     * grid, viewer and covers agreeing on one implementation.
     */
    fun idInSelection(ids: Collection<Long>): Pair<String, Array<String>?> {
        val inList = if (ids.isEmpty()) "-1" else ids.joinToString(",")
        return "$MEDIA_TYPE_SELECTION AND " +
            "${MediaStore.MediaColumns._ID} IN ($inList)" to null
    }

    internal fun collectionUri(isVideo: Boolean): Uri = if (isVideo) {
        if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    fun count(
        resolver: ContentResolver,
        selection: String = MEDIA_TYPE_SELECTION,
        selectionArgs: Array<String>? = null,
    ): Int {
        resolver.query(
            filesUri, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null
        )?.use { return it.count }
        return 0
    }

    /** Reads all rows (projection only) and groups into buckets in memory. */
    fun queryBuckets(resolver: ContentResolver): List<foss.opengallery.app.data.model.Bucket> {
        data class Acc(var count: Int, var cover: MediaItem, var path: String)

        val map = LinkedHashMap<Long, Acc>()
        resolver.query(
            filesUri, PROJECTION, MEDIA_TYPE_SELECTION, null, SORT_DATE_ADDED_DESC
        )?.use { c ->
            if (!c.moveToFirst()) return emptyList()
            val reader = RowReader(c)
            val bucketCol = c.getColumnIndex("bucket_id")
            do {
                // Only materialize a full MediaItem for the first (newest) row
                // of each bucket — it becomes the cover; the rest just count.
                val bid = if (bucketCol >= 0) c.getLong(bucketCol) else 0L
                val acc = map[bid]
                if (acc == null) {
                    val item = reader.read()
                    map[bid] = Acc(1, item, item.relativePath)
                } else {
                    acc.count++
                }
            } while (c.moveToNext())
        }
        return map.map { (id, acc) ->
            foss.opengallery.app.data.model.Bucket(
                bucketId = id,
                name = acc.cover.bucketName,
                itemCount = acc.count,
                coverItem = acc.cover,
                relativePath = acc.path,
            )
        }
    }

    /** Cursor column caching + row mapping. */
    class RowReader(private val c: Cursor) {
        private val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        private val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        private val mime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        private val dateAdded = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        private val dateModified = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
        private val dateTaken = c.getColumnIndex("datetaken")
        private val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        private val width = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
        private val height = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        private val duration = c.getColumnIndex("duration")
        private val bucketId = c.getColumnIndex("bucket_id")
        private val bucketName = c.getColumnIndex("bucket_display_name")
        private val relPath =
            if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH) else -1
        private val favorite =
            if (Build.VERSION.SDK_INT >= 29) c.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE) else -1
        @Suppress("DEPRECATION")
        private val data =
            if (Build.VERSION.SDK_INT < 29) c.getColumnIndex(MediaStore.MediaColumns.DATA) else -1

        fun read(): MediaItem {
            val itemId = c.getLong(id)
            val mimeType = c.getString(mime) ?: "application/octet-stream"
            // Typed item URI (image/video collection) — required by the
            // createTrashRequest/createFavoriteRequest/share APIs.
            val contentUri = ContentUris.withAppendedId(
                collectionUri(mimeType.startsWith("video/")), itemId
            )
            val addedSec = c.getLong(dateAdded)
            val takenMs = if (dateTaken >= 0 && !c.isNull(dateTaken)) c.getLong(dateTaken) else 0L
            val relativePath = when {
                relPath >= 0 -> c.getString(relPath) ?: ""
                data >= 0 -> c.getString(data)?.substringBeforeLast('/', "")?.plus("/") ?: ""
                else -> ""
            }
            return MediaItem(
                id = itemId,
                uri = contentUri,
                displayName = c.getString(name) ?: "",
                mimeType = mimeType,
                takenAtMillis = if (takenMs > 0) takenMs else addedSec * 1000,
                dateAddedSeconds = addedSec,
                dateModifiedSeconds = c.getLong(dateModified),
                sizeBytes = c.getLong(size),
                width = if (width >= 0) c.getInt(width) else 0,
                height = if (height >= 0) c.getInt(height) else 0,
                durationMs = if (duration >= 0 && !c.isNull(duration)) c.getLong(duration) else 0L,
                bucketId = if (bucketId >= 0) c.getLong(bucketId) else 0L,
                bucketName = if (bucketName >= 0) c.getString(bucketName) ?: "" else "",
                relativePath = relativePath,
                isFavorite = favorite >= 0 && c.getInt(favorite) == 1,
            )
        }
    }
}
