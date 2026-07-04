package foss.opengallery.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** User preferences. All local; nothing is synced anywhere. */
class SettingsRepository(private val context: Context) {

    object Keys {
        val AutoPlayMotion = booleanPreferencesKey("auto_play_motion")
        val AutoCreateStories = booleanPreferencesKey("auto_create_stories")
        val StripLocationOnShare = booleanPreferencesKey("strip_location_on_share")
        val ShowPlaceNames = booleanPreferencesKey("show_place_names")
        val IndexingEnabled = booleanPreferencesKey("indexing_enabled")
    }

    data class Settings(
        val autoPlayMotion: Boolean = true,
        val autoCreateStories: Boolean = true,
        val stripLocationOnShare: Boolean = false,
        val showPlaceNames: Boolean = true,
        val indexingEnabled: Boolean = true,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            autoPlayMotion = prefs[Keys.AutoPlayMotion] ?: true,
            autoCreateStories = prefs[Keys.AutoCreateStories] ?: true,
            stripLocationOnShare = prefs[Keys.StripLocationOnShare] ?: false,
            showPlaceNames = prefs[Keys.ShowPlaceNames] ?: true,
            indexingEnabled = prefs[Keys.IndexingEnabled] ?: true,
        )
    }

    suspend fun set(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }
}
