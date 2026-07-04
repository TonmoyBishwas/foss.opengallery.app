package foss.opengallery.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * App-local metadata that MediaStore cannot hold: per-album preferences,
 * user-created (virtual) albums, and their memberships. More tables join in
 * later milestones (trash fallback, tags, faces, search index).
 */

@Entity(tableName = "album_meta")
data class AlbumMetaEntity(
    @PrimaryKey val bucketId: Long,
    val hidden: Boolean = false,
    val pinned: Boolean = false,
    /** Encoded sort: see [AlbumSort]. 0 = default (date added desc). */
    val sortMode: Int = 0,
)

@Entity(tableName = "custom_album")
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Optional parent group id (albums can live inside a Group). */
    val groupId: Long? = null,
    /** True when this row is a group of albums rather than an album. */
    val isGroup: Boolean = false,
    val createdAtMillis: Long,
)

@Entity(tableName = "custom_album_item", primaryKeys = ["albumId", "mediaId"])
data class CustomAlbumItemEntity(
    val albumId: Long,
    val mediaId: Long,
    val addedAtMillis: Long,
)

/** Pre-Android-11 fallback trash record (file moved into app storage). */
@Entity(tableName = "trashed_item")
data class TrashedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val originalRelativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val trashedAtMillis: Long,
    /** Absolute path of the moved file inside the app's trash dir. */
    val storedPath: String,
)

/** Pre-Android-11 fallback favourite flag. */
@Entity(tableName = "favorite_item")
data class FavoriteItemEntity(
    @PrimaryKey val mediaId: Long,
)

/** One encrypted item in the Locked Folder. */
@Entity(tableName = "locked_item")
data class LockedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAtMillis: Long,
    /** Encrypted blob file name inside filesDir/locked. */
    val fileName: String,
)

/** Per-photo on-device intelligence index (OCR text + labels). */
@Entity(tableName = "media_index")
data class MediaIndexEntity(
    @PrimaryKey val mediaId: Long,
    val ocrText: String,
    val labels: String,
    val faceCount: Int,
    val indexedAtMillis: Long,
)

/** FTS mirror of [MediaIndexEntity] for fast free-text search. */
@Fts4(contentEntity = MediaIndexEntity::class)
@Entity(tableName = "media_index_fts")
data class MediaIndexFtsEntity(
    val ocrText: String,
    val labels: String,
)

/** One detected face: its embedding and the person cluster it belongs to. */
@Entity(tableName = "face")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    /** 192 floats, little-endian bytes. */
    val embedding: ByteArray,
    val personId: Long?,
)

/** A person cluster; user-nameable. */
@Entity(tableName = "person")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val coverMediaId: Long,
    /** Cluster centroid, same encoding as FaceEntity.embedding. */
    val centroid: ByteArray,
    val faceCount: Int,
)

@Dao
interface IndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(index: MediaIndexEntity)

    @Query("SELECT mediaId FROM media_index")
    suspend fun indexedIds(): List<Long>

    @Query(
        "SELECT media_index.mediaId FROM media_index JOIN media_index_fts " +
            "ON media_index.rowid = media_index_fts.rowid " +
            "WHERE media_index_fts MATCH :query"
    )
    suspend fun search(query: String): List<Long>

    @Query("SELECT COUNT(*) FROM media_index")
    fun observeIndexedCount(): Flow<Int>

    @Query("SELECT mediaId FROM media_index WHERE ocrText != ''")
    suspend fun idsWithText(): List<Long>
}

@Dao
interface FaceDao {
    @Insert
    suspend fun insert(face: FaceEntity): Long

    @Query("SELECT * FROM face WHERE personId IS NULL")
    suspend fun unassigned(): List<FaceEntity>

    @Query("UPDATE face SET personId = :personId WHERE id = :faceId")
    suspend fun assign(faceId: Long, personId: Long)

    @Query("SELECT * FROM person ORDER BY faceCount DESC")
    fun observePeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM person")
    suspend fun people(): List<PersonEntity>

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Query("UPDATE person SET centroid = :centroid, faceCount = :faceCount WHERE id = :id")
    suspend fun updatePerson(id: Long, centroid: ByteArray, faceCount: Int)

    @Query("UPDATE person SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("SELECT DISTINCT mediaId FROM face WHERE personId = :personId")
    suspend fun mediaIdsForPerson(personId: Long): List<Long>
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trashed_item ORDER BY trashedAtMillis DESC")
    fun observeAll(): Flow<List<TrashedItemEntity>>

    @Insert
    suspend fun insert(item: TrashedItemEntity): Long

    @Query("DELETE FROM trashed_item WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM trashed_item WHERE trashedAtMillis < :cutoff")
    suspend fun olderThan(cutoff: Long): List<TrashedItemEntity>
}

@Dao
interface FavoriteDao {
    @Query("SELECT mediaId FROM favorite_item")
    fun observeIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(item: FavoriteItemEntity)

    @Query("DELETE FROM favorite_item WHERE mediaId = :mediaId")
    suspend fun remove(mediaId: Long)
}

@Dao
interface LockedDao {
    @Query("SELECT * FROM locked_item ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<LockedItemEntity>>

    @Insert
    suspend fun insert(item: LockedItemEntity): Long

    @Query("SELECT * FROM locked_item WHERE id = :id")
    suspend fun get(id: Long): LockedItemEntity?

    @Query("DELETE FROM locked_item WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AlbumMetaDao {
    @Query("SELECT * FROM album_meta")
    fun observeAll(): Flow<List<AlbumMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: AlbumMetaEntity)

    @Query("SELECT * FROM album_meta WHERE bucketId = :bucketId")
    suspend fun get(bucketId: Long): AlbumMetaEntity?
}

@Dao
interface CustomAlbumDao {
    @Query("SELECT * FROM custom_album ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<CustomAlbumEntity>>

    @Insert
    suspend fun insert(album: CustomAlbumEntity): Long

    @Query("UPDATE custom_album SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM custom_album WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addItems(items: List<CustomAlbumItemEntity>)

    @Query("DELETE FROM custom_album_item WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun removeItems(albumId: Long, mediaIds: List<Long>)

    @Query("SELECT mediaId FROM custom_album_item WHERE albumId = :albumId ORDER BY addedAtMillis DESC")
    fun observeItemIds(albumId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM custom_album_item WHERE albumId = :albumId")
    fun observeItemCount(albumId: Long): Flow<Int>
}

@Database(
    entities = [
        AlbumMetaEntity::class,
        CustomAlbumEntity::class,
        CustomAlbumItemEntity::class,
        TrashedItemEntity::class,
        FavoriteItemEntity::class,
        LockedItemEntity::class,
        MediaIndexEntity::class,
        MediaIndexFtsEntity::class,
        FaceEntity::class,
        PersonEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class OgDatabase : RoomDatabase() {
    abstract fun albumMetaDao(): AlbumMetaDao
    abstract fun customAlbumDao(): CustomAlbumDao
    abstract fun trashDao(): TrashDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun lockedDao(): LockedDao
    abstract fun indexDao(): IndexDao
    abstract fun faceDao(): FaceDao

    companion object {
        fun build(context: Context): OgDatabase =
            Room.databaseBuilder(context, OgDatabase::class.java, "opengallery.db")
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
    }
}
