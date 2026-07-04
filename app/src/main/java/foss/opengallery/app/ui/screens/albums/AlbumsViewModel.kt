package foss.opengallery.app.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.data.AlbumEntry
import foss.opengallery.app.data.AlbumsRepository
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.VirtualAlbum
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cover + count info for a virtual album card. */
data class VirtualAlbumUi(
    val album: VirtualAlbum,
    val count: Int,
    val cover: MediaItem?,
)

class AlbumsViewModel(
    private val albumsRepo: AlbumsRepository,
    private val mediaRepo: MediaRepository,
) : ViewModel() {

    /** Folder albums with metadata (hidden ones filtered by callers). */
    val albums: StateFlow<List<AlbumEntry>> =
        albumsRepo.albums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _virtuals = MutableStateFlow<List<VirtualAlbumUi>>(emptyList())
    val virtuals: StateFlow<List<VirtualAlbumUi>> = _virtuals.asStateFlow()

    val customAlbums = albumsRepo.customAlbums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshVirtuals()
        mediaRepo.changes.onEach { refreshVirtuals() }.launchIn(viewModelScope)
    }

    private fun refreshVirtuals() {
        viewModelScope.launch {
            _virtuals.value = listOf(
                VirtualAlbum.Recent,
                VirtualAlbum.Favourites,
                VirtualAlbum.Camera,
                VirtualAlbum.Screenshots,
                VirtualAlbum.Download,
                VirtualAlbum.Videos,
            ).map { v ->
                val (selection, args) = albumsRepo.virtualSelection(v)
                val (images, videos) = mediaRepo.countFor(selection, args)
                val cover = mediaRepo.firstItemFor(selection, args)
                VirtualAlbumUi(v, images + videos, cover)
            }
        }
    }

    fun setHidden(bucketId: Long, hidden: Boolean) {
        viewModelScope.launch { albumsRepo.setHidden(bucketId, hidden) }
    }

    fun setPinned(bucketId: Long, pinned: Boolean) {
        viewModelScope.launch { albumsRepo.setPinned(bucketId, pinned) }
    }

    fun createAlbum(name: String, isGroup: Boolean) {
        viewModelScope.launch {
            albumsRepo.createCustomAlbum(name, isGroup, System.currentTimeMillis())
        }
    }

    fun renameAlbum(id: Long, name: String) {
        viewModelScope.launch { albumsRepo.renameCustomAlbum(id, name) }
    }

    fun deleteAlbum(id: Long) {
        viewModelScope.launch { albumsRepo.deleteCustomAlbum(id) }
    }
}
