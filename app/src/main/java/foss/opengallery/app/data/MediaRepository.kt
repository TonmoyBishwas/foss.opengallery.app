package foss.opengallery.app.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import foss.opengallery.app.data.model.Bucket
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    private val resolver = context.contentResolver

    /**
     * Emits whenever anything in the media store changes. Debounced: the
     * provider fires bursts of notifications (saves, downloads, its own
     * thumbnail writes) and invalidating pagers per-notification restarts
     * page loads mid-scroll.
     */
    @OptIn(FlowPreview::class)
    val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(MediaQuery.filesUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.debounce(700).flowOn(Dispatchers.IO)

    /** All photos+videos, newest first. */
    fun timelinePager(): Pair<Pager<Int, MediaItem>, () -> Unit> {
        var current: MediaPagingSource? = null
        val pager = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = PAGE_SIZE * 2,
                enablePlaceholders = false,
            )
        ) {
            MediaPagingSource(resolver, pageSize = PAGE_SIZE).also { current = it }
        }
        return pager to { current?.invalidate() }
    }

    /** Items of one bucket (folder album). */
    fun bucketPager(
        bucketId: Long,
        sortOrder: String = MediaQuery.SORT_DATE_ADDED_DESC,
    ): Pair<Pager<Int, MediaItem>, () -> Unit> =
        selectionPager(
            selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?",
            selectionArgs = arrayOf(bucketId.toString()),
            sortOrder = sortOrder,
        )

    /** Arbitrary-selection pager (virtual albums: videos, favourites, ...). */
    fun selectionPager(
        selection: String,
        selectionArgs: Array<String>? = null,
        sortOrder: String = MediaQuery.SORT_DATE_ADDED_DESC,
    ): Pair<Pager<Int, MediaItem>, () -> Unit> {
        var current: MediaPagingSource? = null
        val pager = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE * 2,
                prefetchDistance = PAGE_SIZE * 2,
                enablePlaceholders = false,
            )
        ) {
            MediaPagingSource(
                resolver,
                selection = selection,
                selectionArgs = selectionArgs,
                sortOrder = sortOrder,
                pageSize = PAGE_SIZE,
            ).also { current = it }
        }
        return pager to { current?.invalidate() }
    }

    /** Every (id, typed uri) matching a selection — backs "Select all". */
    suspend fun allUris(
        selection: String = MediaQuery.MEDIA_TYPE_SELECTION,
        args: Array<String>? = null,
        sortOrder: String = MediaQuery.SORT_DATE_ADDED_DESC,
    ): List<Pair<Long, Uri>> = withContext(Dispatchers.IO) {
        MediaQuery.queryAllUris(resolver, selection, args, sortOrder)
    }

    suspend fun countFor(selection: String, args: Array<String>? = null): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val images = MediaQuery.count(
                resolver,
                "($selection) AND ${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = " +
                    "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}",
                args,
            )
            val videos = MediaQuery.count(
                resolver,
                "($selection) AND ${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = " +
                    "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}",
                args,
            )
            images to videos
        }

    suspend fun buckets(): List<Bucket> = withContext(Dispatchers.IO) {
        MediaQuery.queryBuckets(resolver)
    }

    /** Newest item matching a selection — used for album covers. */
    suspend fun firstItemFor(selection: String, args: Array<String>? = null): MediaItem? =
        withContext(Dispatchers.IO) {
            MediaQuery.queryPage(resolver, offset = 0, limit = 1, selection = selection, selectionArgs = args)
                .firstOrNull()
        }

    suspend fun totalCount(): Int = withContext(Dispatchers.IO) {
        MediaQuery.count(resolver)
    }

    private companion object {
        const val PAGE_SIZE = 120
    }
}
