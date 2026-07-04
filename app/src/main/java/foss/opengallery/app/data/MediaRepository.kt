package foss.opengallery.app.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import foss.opengallery.app.data.model.Bucket
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    private val resolver = context.contentResolver

    /** Emits Unit whenever anything in the media store changes. */
    val changes: Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(MediaQuery.filesUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)

    /** All photos+videos, newest first. */
    fun timelinePager(): Pair<Pager<Int, MediaItem>, () -> Unit> {
        var current: MediaPagingSource? = null
        val pager = Pager(
            config = PagingConfig(
                pageSize = 120,
                initialLoadSize = 240,
                prefetchDistance = 240,
                enablePlaceholders = false,
            )
        ) {
            MediaPagingSource(resolver).also { current = it }
        }
        return pager to { current?.invalidate() }
    }

    /** Items of one bucket (folder album). */
    fun bucketPager(bucketId: Long): Pair<Pager<Int, MediaItem>, () -> Unit> {
        var current: MediaPagingSource? = null
        val selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?"
        val pager = Pager(
            config = PagingConfig(pageSize = 120, initialLoadSize = 240, enablePlaceholders = false)
        ) {
            MediaPagingSource(
                resolver,
                selection = selection,
                selectionArgs = arrayOf(bucketId.toString()),
            ).also { current = it }
        }
        return pager to { current?.invalidate() }
    }

    suspend fun buckets(): List<Bucket> = withContext(Dispatchers.IO) {
        MediaQuery.queryBuckets(resolver)
    }

    suspend fun totalCount(): Int = withContext(Dispatchers.IO) {
        MediaQuery.count(resolver)
    }
}
