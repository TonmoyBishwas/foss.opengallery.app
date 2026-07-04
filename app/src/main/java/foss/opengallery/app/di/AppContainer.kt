package foss.opengallery.app.di

import android.content.Context
import foss.opengallery.app.data.AlbumsRepository
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.db.OgDatabase

/**
 * Hand-rolled DI: one lazily-constructed graph, owned by the Application.
 * Deliberately no framework — keeps APK and build times small.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: OgDatabase by lazy { OgDatabase.build(appContext) }
    val mediaRepository: MediaRepository by lazy { MediaRepository(appContext) }
    val albumsRepository: AlbumsRepository by lazy {
        AlbumsRepository(mediaRepository, database)
    }
}
