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
    sortEncoded: Int,
) : AndroidViewModel(application) {

    private val resolver = application.contentResolver

    private val source: ViewerSource = run {
        val (selection, args) = when (type) {
            "bucket" -> "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?" to arrayOf(id)
            "virtual" -> {
                val v = VirtualAlbum.fromKey(id) ?: VirtualAlbum.Recent
                albums.virtualSelection(v)
            }
            else -> MediaQuery.MEDIA_TYPE_SELECTION to null
        }
        ViewerSource(
            resolver = resolver,
            selection = selection,
            selectionArgs = args,
            sortOrder = AlbumSort.fromEncoded(sortEncoded).toSqlSort(),
        )
    }

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    private val _initialIndex = MutableStateFlow<Int?>(null)
    val initialIndex: StateFlow<Int?> = _initialIndex.asStateFlow()

    init {
        viewModelScope.launch {
            _count.value = source.load()
            _initialIndex.value = source.indexOf(startMediaId).coerceAtLeast(0)
        }
    }

    suspend fun itemAt(index: Int): MediaItem? = source.itemAt(index)

    fun idAt(index: Int): Long? = source.idAt(index)

    fun onDeleted(mediaId: Long) {
        source.remove(mediaId)
        _count.value = source.count
    }
}
