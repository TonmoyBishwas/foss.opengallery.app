package foss.opengallery.app.data

import android.content.ContentResolver
import androidx.paging.PagingSource
import androidx.paging.PagingState
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pages the unified image+video stream out of MediaStore by offset.
 * Invalidated by [MediaRepository] whenever MediaStore reports a change.
 */
class MediaPagingSource(
    private val resolver: ContentResolver,
    private val selection: String = MediaQuery.MEDIA_TYPE_SELECTION,
    private val selectionArgs: Array<String>? = null,
    private val sortOrder: String = MediaQuery.SORT_DATE_ADDED_DESC,
) : PagingSource<Int, MediaItem>() {

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        return (anchor - state.config.initialLoadSize / 2).coerceAtLeast(0)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> =
        withContext(Dispatchers.IO) {
            try {
                val offset = params.key ?: 0
                val items = MediaQuery.queryPage(
                    resolver = resolver,
                    offset = offset,
                    limit = params.loadSize,
                    selection = selection,
                    selectionArgs = selectionArgs,
                    sortOrder = sortOrder,
                )
                LoadResult.Page(
                    data = items,
                    prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                    nextKey = if (items.size < params.loadSize) null else offset + items.size,
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
}
