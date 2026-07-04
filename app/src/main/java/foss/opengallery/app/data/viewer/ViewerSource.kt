package foss.opengallery.app.data.viewer

import android.content.ContentResolver
import android.util.LruCache
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.model.MediaItem
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Random-access source for the fullscreen viewer: loads the full ordered
 * id list once (cheap — a single _ID projection pass), then fetches items
 * lazily by id with an LRU cache. This allows instant jumps to any page,
 * which offset-based paging cannot do without placeholders.
 */
class ViewerSource(
    private val resolver: ContentResolver,
    private val selection: String,
    private val selectionArgs: Array<String>?,
    private val sortOrder: String,
) {
    private var ids: LongArray = LongArray(0)
    private val cache = LruCache<Long, MediaItem>(64)

    val count: Int get() = ids.size

    suspend fun load(): Int = withContext(Dispatchers.IO) {
        val list = ArrayList<Long>(1024)
        resolver.query(
            MediaQuery.filesUri,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            sortOrder,
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (c.moveToNext()) list.add(c.getLong(idCol))
        }
        ids = list.toLongArray()
        ids.size
    }

    fun indexOf(mediaId: Long): Int = ids.indexOfFirst { it == mediaId }

    fun idAt(index: Int): Long? = ids.getOrNull(index)

    suspend fun itemAt(index: Int): MediaItem? {
        val id = ids.getOrNull(index) ?: return null
        cache.get(id)?.let { return it }
        return withContext(Dispatchers.IO) {
            MediaQuery.queryPage(
                resolver = resolver,
                offset = 0,
                limit = 1,
                selection = "${MediaStore.MediaColumns._ID} = ?",
                selectionArgs = arrayOf(id.toString()),
                sortOrder = MediaQuery.SORT_DATE_ADDED_DESC,
            ).firstOrNull()?.also { cache.put(id, it) }
        }
    }

    /** Drop a deleted id so the pager collapses around it. */
    fun remove(mediaId: Long) {
        cache.remove(mediaId)
        ids = ids.filter { it != mediaId }.toLongArray()
    }
}
