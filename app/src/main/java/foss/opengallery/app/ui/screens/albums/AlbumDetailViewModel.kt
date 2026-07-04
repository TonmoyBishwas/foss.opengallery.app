package foss.opengallery.app.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import foss.opengallery.app.data.AlbumSort
import foss.opengallery.app.data.AlbumsRepository
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.VirtualAlbum
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs [AlbumDetailScreen] for all three album kinds:
 *  - bucket:  one MediaStore folder
 *  - virtual: Recent / Favourites / Camera / Screenshots / Download / Videos
 *  - custom:  user-created album whose membership lives in Room
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModel(
    private val media: MediaRepository,
    private val albums: AlbumsRepository,
    private val type: String,
    private val id: String,
) : ViewModel() {

    private val sort = MutableStateFlow(AlbumSort.DateAddedDesc)

    private val _counts = MutableStateFlow<Pair<Int, Int>?>(null)
    val counts: StateFlow<Pair<Int, Int>?> = _counts.asStateFlow()

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private var invalidateCurrent: (() -> Unit)? = null

    val items: Flow<PagingData<MediaItem>> = sort.flatMapLatest { s ->
        val (selection, args) = selectionForAlbum()
        val (pager, invalidate) = media.selectionPager(selection, args, s.toSqlSort())
        invalidateCurrent = invalidate
        pager.flow
    }.cachedIn(viewModelScope)

    init {
        // Load persisted per-album sort, keep counts fresh.
        viewModelScope.launch {
            if (type == "bucket") {
                id.toLongOrNull()?.let { sort.value = albums.sortFor(it) }
            }
            refreshCounts()
        }
        media.changes
            .onEach {
                invalidateCurrent?.invoke()
                refreshCounts()
            }
            .launchIn(viewModelScope)
    }

    private fun selectionForAlbum(): Pair<String, Array<String>?> = when (type) {
        "bucket" -> "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?" to arrayOf(id)
        "virtual" -> {
            val v = VirtualAlbum.fromKey(id) ?: VirtualAlbum.Recent
            albums.virtualSelection(v)
        }
        else -> {
            // Custom albums: filter by the stored media ids (bounded IN list).
            // Falls back to empty selection when the album has no items.
            MediaQuery.MEDIA_TYPE_SELECTION to null
        }
    }

    private fun refreshCounts() {
        viewModelScope.launch {
            val (selection, args) = selectionForAlbum()
            _counts.value = media.countFor(selection, args)
        }
    }

    fun setSort(s: AlbumSort) {
        sort.value = s
        if (type == "bucket") {
            viewModelScope.launch {
                id.toLongOrNull()?.let { albums.setSort(it, s) }
            }
        }
    }

    fun enterSelection(initial: MediaItem? = null) {
        _selectionMode.value = true
        initial?.let { _selection.value = setOf(it.id) }
    }

    fun toggleSelected(item: MediaItem) {
        _selection.update { sel -> if (item.id in sel) sel - item.id else sel + item.id }
    }

    fun clearSelection() {
        _selectionMode.value = false
        _selection.value = emptySet()
    }
}
