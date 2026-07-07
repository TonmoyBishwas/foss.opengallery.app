package foss.opengallery.app.data

import android.provider.MediaStore
import foss.opengallery.app.data.db.AlbumMetaEntity
import foss.opengallery.app.data.db.CustomAlbumEntity
import foss.opengallery.app.data.db.CustomAlbumItemEntity
import foss.opengallery.app.data.db.OgDatabase
import foss.opengallery.app.data.model.Bucket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** A folder album joined with its stored preferences. */
data class AlbumEntry(
    val bucket: Bucket,
    val hidden: Boolean,
    val pinned: Boolean,
    val sort: AlbumSort,
)

/** Virtual albums always shown alongside folder albums. */
enum class VirtualAlbum(
    val key: String,
    val title: String,
    /** MediaStore bucket backing this album, when it maps to one — the
     *  single source both the SQL selection and card counts match on. */
    val bucketName: String? = null,
) {
    Recent("recent", "Recent"),
    Favourites("favourites", "Favourites"),
    Camera("camera", "Camera", bucketName = "Camera"),
    Screenshots("screenshots", "Screenshots", bucketName = "Screenshots"),
    Videos("videos", "Videos"),
    Download("download", "Download", bucketName = "Download"),
    ;

    companion object {
        fun fromKey(key: String): VirtualAlbum? = entries.firstOrNull { it.key == key }
    }
}

class AlbumsRepository(
    private val media: MediaRepository,
    private val db: OgDatabase,
) {
    private val metaDao = db.albumMetaDao()
    private val customDao = db.customAlbumDao()

    /** All folder albums merged with metadata, refreshed on media changes. */
    val albums: Flow<List<AlbumEntry>> =
        combine(
            media.changes.onStart { emit(Unit) },
            metaDao.observeAll(),
        ) { _, metas -> metas }
            .map { metas ->
                val metaById = metas.associateBy(AlbumMetaEntity::bucketId)
                media.buckets().map { bucket ->
                    val meta = metaById[bucket.bucketId]
                    AlbumEntry(
                        bucket = bucket,
                        hidden = meta?.hidden ?: false,
                        pinned = meta?.pinned ?: false,
                        sort = AlbumSort.fromEncoded(meta?.sortMode ?: 0),
                    )
                }.sortedWith(
                    compareByDescending<AlbumEntry> { it.pinned }
                        .thenBy { it.bucket.name.lowercase() }
                )
            }

    val customAlbums: Flow<List<CustomAlbumEntity>> = customDao.observeAll()

    suspend fun setHidden(bucketId: Long, hidden: Boolean) {
        val existing = metaDao.get(bucketId) ?: AlbumMetaEntity(bucketId)
        metaDao.upsert(existing.copy(hidden = hidden))
    }

    suspend fun setPinned(bucketId: Long, pinned: Boolean) {
        val existing = metaDao.get(bucketId) ?: AlbumMetaEntity(bucketId)
        metaDao.upsert(existing.copy(pinned = pinned))
    }

    suspend fun setSort(bucketId: Long, sort: AlbumSort) {
        val existing = metaDao.get(bucketId) ?: AlbumMetaEntity(bucketId)
        metaDao.upsert(existing.copy(sortMode = sort.encoded))
    }

    suspend fun sortFor(bucketId: Long): AlbumSort =
        AlbumSort.fromEncoded(metaDao.get(bucketId)?.sortMode ?: 0)

    suspend fun createCustomAlbum(name: String, isGroup: Boolean, now: Long): Long =
        customDao.insert(
            CustomAlbumEntity(name = name.trim(), isGroup = isGroup, createdAtMillis = now)
        )

    suspend fun renameCustomAlbum(id: Long, name: String) = customDao.rename(id, name.trim())

    suspend fun deleteCustomAlbum(id: Long) = customDao.delete(id)

    suspend fun addToCustomAlbum(albumId: Long, mediaIds: List<Long>, now: Long) =
        customDao.addItems(mediaIds.map { CustomAlbumItemEntity(albumId, it, now) })

    fun customAlbumItemIds(albumId: Long): Flow<List<Long>> = customDao.observeItemIds(albumId)

    /** SQL selection for a virtual album, or null when it needs id lists. */
    fun virtualSelection(album: VirtualAlbum): Pair<String, Array<String>?> = when (album) {
        VirtualAlbum.Recent -> MediaQuery.MEDIA_TYPE_SELECTION to null
        VirtualAlbum.Favourites ->
            "${MediaQuery.MEDIA_TYPE_SELECTION} AND ${MediaStore.MediaColumns.IS_FAVORITE} = 1" to null
        VirtualAlbum.Camera, VirtualAlbum.Screenshots, VirtualAlbum.Download ->
            "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_display_name = ?" to
                arrayOf(album.bucketName!!)
        VirtualAlbum.Videos ->
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}" to null
    }
}
