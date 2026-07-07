package foss.opengallery.app.ui.screens.viewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.data.AlbumSort
import foss.opengallery.app.data.AlbumsRepository
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.VirtualAlbum
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.data.viewer.ViewerSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fullscreen viewer state: a random-access window over the same query the
 * launching screen showed (timeline, bucket, virtual or custom album).
 */
class ViewerViewModel(
    application: Application,
    private val albums: AlbumsRepository,
    private val type: String,
    private val id: String,
    private val startMediaId: Long,
    private val sortEncoded: Int,
) : AndroidViewModel(application) {

    private val resolver = application.contentResolver

    // Built asynchronously: custom albums need their member ids from Room
    // before the selection exists. Null until load() completes.
    private var source: ViewerSource? = null

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _initialIndex = MutableStateFlow<Int?>(null)
    val initialIndex: StateFlow<Int?> = _initialIndex.asStateFlow()

    init {
        viewModelScope.launch {
            val (selection, args) = when (type) {
                "bucket" ->
                    "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?" to arrayOf(id)
                "virtual" -> {
                    val v = VirtualAlbum.fromKey(id) ?: VirtualAlbum.Recent
                    albums.virtualSelection(v)
                }
                "custom" -> {
                    val ids = albums.customAlbumItemIds(id.toLongOrNull() ?: -1L).first()
                    MediaQuery.idInSelection(ids)
                }
                else -> MediaQuery.MEDIA_TYPE_SELECTION to null
            }
            val s = ViewerSource(
                resolver = resolver,
                selection = selection,
                selectionArgs = args,
                sortOrder = AlbumSort.fromEncoded(sortEncoded).toSqlSort(),
            )
            source = s
            _count.value = s.load()
            _initialIndex.value = s.indexOf(startMediaId).coerceAtLeast(0)
        }
    }

    suspend fun itemAt(index: Int): MediaItem? = source?.itemAt(index)

    fun idAt(index: Int): Long? = source?.idAt(index)

    /** Drop the cached row so the next itemAt re-reads it (e.g. favourite). */
    fun invalidateItem(mediaId: Long) {
        source?.invalidate(mediaId)
    }

    fun onDeleted(mediaId: Long) {
        val s = source ?: return
        s.remove(mediaId)
        _count.value = s.count
    }
}
