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
    val trashRepository: foss.opengallery.app.data.trash.TrashRepository by lazy {
        foss.opengallery.app.data.trash.TrashRepository(appContext, database)
    }
    val lockedStore: foss.opengallery.app.data.locked.LockedStore by lazy {
        foss.opengallery.app.data.locked.LockedStore(appContext, database)
    }
    val settingsRepository: foss.opengallery.app.data.settings.SettingsRepository by lazy {
        foss.opengallery.app.data.settings.SettingsRepository(appContext)
    }
}
