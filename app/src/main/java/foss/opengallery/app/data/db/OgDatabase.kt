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
    ],
    version = 1,
    exportSchema = false,
)
abstract class OgDatabase : RoomDatabase() {
    abstract fun albumMetaDao(): AlbumMetaDao
    abstract fun customAlbumDao(): CustomAlbumDao

    companion object {
        fun build(context: Context): OgDatabase =
            Room.databaseBuilder(context, OgDatabase::class.java, "opengallery.db")
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
    }
}
