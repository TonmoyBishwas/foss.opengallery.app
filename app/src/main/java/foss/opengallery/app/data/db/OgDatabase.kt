package foss.opengallery.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
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
    ],
    version = 2,
    exportSchema = false,
)
abstract class OgDatabase : RoomDatabase() {
    abstract fun albumMetaDao(): AlbumMetaDao
    abstract fun customAlbumDao(): CustomAlbumDao
    abstract fun trashDao(): TrashDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun lockedDao(): LockedDao

    companion object {
        fun build(context: Context): OgDatabase =
            Room.databaseBuilder(context, OgDatabase::class.java, "opengallery.db")
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
    }
}
