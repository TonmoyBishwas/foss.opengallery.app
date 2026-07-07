package foss.opengallery.app.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.data.AlbumEntry
import foss.opengallery.app.data.AlbumsRepository
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.VirtualAlbum
import foss.opengallery.app.data.db.CustomAlbumEntity
import foss.opengallery.app.data.model.Bucket
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cover + count info for a virtual album card. */
data class VirtualAlbumUi(
    val album: VirtualAlbum,
    val count: Int,
    val cover: MediaItem?,
)

/** Cover + count info for a user-created album card. */
data class CustomAlbumUi(
    val album: CustomAlbumEntity,
    val count: Int,
    val cover: MediaItem?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModel(
    private val albumsRepo: AlbumsRepository,
    private val mediaRepo: MediaRepository,
) : ViewModel() {

    /** Folder albums with metadata (hidden ones filtered by callers). */
    val albums: StateFlow<List<AlbumEntry>> =
        albumsRepo.albums.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Derived from the same single buckets() scan that feeds [albums] — the
     * Albums tab used to run a second full scan per change tick. Only
     * Favourites and Videos need their own queries.
     */
    val virtuals: StateFlow<List<VirtualAlbumUi>> = albums
        .mapLatest { entries -> buildVirtuals(entries.map { it.bucket }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customAlbums: StateFlow<List<CustomAlbumUi>> = albumsRepo.customAlbums
        .flatMapLatest { albums ->
            if (albums.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    albums.map { album ->
                        // Room invalidation is table-level: without distinct,
                        // one album's edit re-queries every album's cover.
                        albumsRepo.customAlbumItemIds(album.id)
                            .distinctUntilChanged()
                            .map { ids -> album to ids }
                    }
                ) { it.toList() }
            }
        }
        .mapLatest { pairs ->
            pairs.map { (album, ids) -> CustomAlbumUi(album, ids.size, customCover(ids)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun buildVirtuals(buckets: List<Bucket>): List<VirtualAlbumUi> {
        // Each bucket's coverItem is its newest; the global newest is the
        // max across covers (the entries list is sorted by pin/name).
        fun newestOf(list: List<Bucket>): MediaItem? =
            list.maxByOrNull { it.coverItem?.dateAddedSeconds ?: 0L }?.coverItem

        fun fromBuckets(name: String): Pair<Int, MediaItem?> {
            val matches = buckets.filter { it.name == name }
            return matches.sumOf { it.itemCount } to newestOf(matches)
        }
        return listOf(
            VirtualAlbum.Recent,
            VirtualAlbum.Favourites,
            VirtualAlbum.Camera,
            VirtualAlbum.Screenshots,
            VirtualAlbum.Download,
            VirtualAlbum.Videos,
        ).map { v ->
            when (v) {
                VirtualAlbum.Recent -> VirtualAlbumUi(
                    v,
                    buckets.sumOf { it.itemCount },
                    newestOf(buckets),
                )
                VirtualAlbum.Camera, VirtualAlbum.Screenshots, VirtualAlbum.Download -> {
                    val (count, cover) = fromBuckets(v.bucketName!!)
                    VirtualAlbumUi(v, count, cover)
                }
                VirtualAlbum.Favourites, VirtualAlbum.Videos -> {
                    val (selection, args) = albumsRepo.virtualSelection(v)
                    val (images, videos) = mediaRepo.countFor(selection, args)
                    VirtualAlbumUi(v, images + videos, mediaRepo.firstItemFor(selection, args))
                }
            }
        }
    }

    /** Newest member as cover; IN list bounded to keep the SQL small. */
    private suspend fun customCover(ids: List<Long>): MediaItem? {
        if (ids.isEmpty()) return null
        // ids arrive newest-added first.
        val (selection, args) = MediaQuery.idInSelection(ids.take(400))
        return mediaRepo.firstItemFor(selection, args)
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
