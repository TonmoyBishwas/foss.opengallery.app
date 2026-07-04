package foss.opengallery.app.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.db.OgDatabase
import foss.opengallery.app.data.db.PersonEntity
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
    application: Application,
    private val db: OgDatabase,
) : AndroidViewModel(application) {

    private val resolver = application.contentResolver

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<MediaItem>>(emptyList())
    val results: StateFlow<List<MediaItem>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    val people: StateFlow<List<PersonEntity>> = db.faceDao().observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val indexedCount: StateFlow<Int> = db.indexDao().observeIndexedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var searchJob: Job? = null

    fun setQuery(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // debounce typing
            _searching.value = true
            _results.value = search(text.trim())
            _searching.value = false
        }
    }

    /**
     * Free-text search: filename + folder LIKE match unioned with the
     * FTS index over OCR text and object labels.
     */
    private suspend fun search(text: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val byName = MediaQuery.queryPage(
                resolver, offset = 0, limit = 200,
                selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND " +
                    "(${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? " +
                    "OR bucket_display_name LIKE ?)",
                selectionArgs = arrayOf("%$text%", "%$text%"),
            )
            val ftsIds = runCatching {
                db.indexDao().search(sanitizeFts(text))
            }.getOrDefault(emptyList())
            val byIndex = if (ftsIds.isEmpty()) emptyList() else queryByIds(ftsIds.take(300))
            (byName + byIndex).distinctBy { it.id }
        }

    private fun sanitizeFts(text: String): String =
        text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token -> token.replace(Regex("[^\\p{L}\\p{N}]"), "") + "*" }

    suspend fun queryByIds(ids: List<Long>): List<MediaItem> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext emptyList()
            val placeholders = ids.joinToString(",") { "?" }
            MediaQuery.queryPage(
                resolver, offset = 0, limit = ids.size,
                selection = "${android.provider.MediaStore.MediaColumns._ID} IN ($placeholders)",
                selectionArgs = ids.map(Long::toString).toTypedArray(),
            )
        }

    suspend fun personItems(personId: Long): List<MediaItem> =
        queryByIds(db.faceDao().mediaIdsForPerson(personId))

    suspend fun personCover(person: PersonEntity): MediaItem? =
        queryByIds(listOf(person.coverMediaId)).firstOrNull()

    fun renamePerson(id: Long, name: String) {
        viewModelScope.launch { db.faceDao().rename(id, name) }
    }
}
