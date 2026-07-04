package foss.opengallery.app

import android.app.Application

class OpenGalleryApp : Application() {
    // AppContainer (manual DI) is introduced in the media-core milestone.
    override fun onCreate() {
        super.onCreate()
    }
}
