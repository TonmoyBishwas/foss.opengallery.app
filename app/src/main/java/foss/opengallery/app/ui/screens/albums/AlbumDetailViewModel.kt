package foss.opengallery.app.ui.screens.albums

import android.net.Uri
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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

    /** id → content uri; a map so bulk actions don't depend on loaded pages. */
    private val _selection = MutableStateFlow<Map<Long, Uri>>(emptyMap())
    val selection: StateFlow<Map<Long, Uri>> = _selection.asStateFlow()
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private var invalidateCurrent: (() -> Unit)? = null

    /**
     * The SQL selection for this album. For custom albums it tracks the Room
     * membership list; buckets and virtual albums are static.
     */
    private val albumSelection: StateFlow<Pair<String, Array<String>?>> = when (type) {
        "custom" -> {
            val albumId = id.toLongOrNull() ?: -1L
            albums.customAlbumItemIds(albumId).map { ids -> customSelection(ids) }
        }
        else -> flowOf(staticSelection())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialSelection())

    val items: Flow<PagingData<MediaItem>> =
        combine(sort, albumSelection) { s, sel -> s to sel }
            .flatMapLatest { (s, sel) ->
                val (pager, invalidate) = media.selectionPager(sel.first, sel.second, s.toSqlSort())
                invalidateCurrent = invalidate
                pager.flow
            }
            .cachedIn(viewModelScope)

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
        // Membership edits should refresh the header counts too.
        if (type == "custom") {
            albumSelection
                .onEach { refreshCounts() }
                .launchIn(viewModelScope)
        }
    }

    private fun initialSelection(): Pair<String, Array<String>?> =
        if (type == "custom") customSelection(emptyList()) else staticSelection()

    private fun staticSelection(): Pair<String, Array<String>?> = when (type) {
        "bucket" -> "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_id = ?" to arrayOf(id)
        else -> {
            val v = VirtualAlbum.fromKey(id) ?: VirtualAlbum.Recent
            albums.virtualSelection(v)
        }
    }

    /** Membership filter; `_ID IN (-1)` so an empty album shows nothing. */
    private fun customSelection(ids: List<Long>): Pair<String, Array<String>?> =
        MediaQuery.idInSelection(ids)

    private fun refreshCounts() {
        viewModelScope.launch {
            val (selection, args) = albumSelection.value
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
        initial?.let { _selection.value = mapOf(it.id to it.uri) }
    }

    fun toggleSelected(item: MediaItem) {
        _selection.update { sel ->
            if (item.id in sel) sel - item.id else sel + (item.id to item.uri)
        }
    }

    /** Selects every item in the album, not just the pages loaded so far. */
    fun selectAll() {
        viewModelScope.launch {
            val (selection, args) = albumSelection.value
            _selection.value = media.allUris(selection, args, sort.value.toSqlSort()).toMap()
        }
    }

    fun clearSelection() {
        _selectionMode.value = false
        _selection.value = emptyMap()
    }

    fun selectedUris(): List<Uri> = _selection.value.values.toList()
}
