package foss.opengallery.app.di

import android.content.Context
import foss.opengallery.app.data.MediaRepository

/**
 * Hand-rolled DI: one lazily-constructed graph, owned by the Application.
 * Deliberately no framework — keeps APK and build times small.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val mediaRepository: MediaRepository by lazy { MediaRepository(appContext) }
}
