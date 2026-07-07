package foss.opengallery.app.ui.screens.pictures

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PicturesViewModel(private val repo: MediaRepository) : ViewModel() {

    private val pagerAndInvalidate = repo.timelinePager()

    /** Day/Month/Year timeline grouping. */
    private val _grouping = MutableStateFlow(TimelineGrouping.Day)
    val grouping: StateFlow<TimelineGrouping> = _grouping.asStateFlow()

    /** Grid density (columns). */
    private val _columns = MutableStateFlow(4)
    val columns: StateFlow<Int> = _columns.asStateFlow()

    /**
     * Multi-select state as id → content uri, so share/delete work even for
     * items selected via "Select all" that paging never loaded.
     */
    private val _selection = MutableStateFlow<Map<Long, Uri>>(emptyMap())
    val selection: StateFlow<Map<Long, Uri>> = _selection.asStateFlow()
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val basePaging: Flow<PagingData<MediaItem>> =
        pagerAndInvalidate.first.flow.cachedIn(viewModelScope)

    val timeline: Flow<PagingData<TimelineCell>> =
        _grouping.flatMapLatest { grouping -> basePaging.withDateHeaders(grouping) }
            .cachedIn(viewModelScope)

    init {
        repo.changes
            .onEach { pagerAndInvalidate.second.invoke() }
            .launchIn(viewModelScope)
    }

    fun pinchIn() {
        // Fewer columns = bigger thumbs; below the minimum, zoom the grouping in.
        val c = _columns.value
        if (c > MIN_COLUMNS) _columns.value = c - 1
        else _grouping.update { g ->
            when (g) {
                TimelineGrouping.Year -> TimelineGrouping.Month
                TimelineGrouping.Month -> TimelineGrouping.Day
                TimelineGrouping.Day -> g
            }
        }
    }

    fun pinchOut() {
        val c = _columns.value
        if (c < MAX_COLUMNS) _columns.value = c + 1
        else _grouping.update { g ->
            when (g) {
                TimelineGrouping.Day -> TimelineGrouping.Month
                TimelineGrouping.Month -> TimelineGrouping.Year
                TimelineGrouping.Year -> g
            }
        }
    }

    fun enterSelection(initial: MediaItem? = null) {
        _selectionMode.value = true
        initial?.let { _selection.value = mapOf(it.id to it.uri) }
    }

    fun toggleSelected(item: MediaItem) {
        _selection.update { sel ->
            if (item.id in sel) sel - item.id else sel + (item.id to item.uri)
        }
    }

    fun clearSelection() {
        _selectionMode.value = false
        _selection.value = emptyMap()
    }

    /** Selects the entire library, not just the pages loaded so far. */
    fun selectAll() {
        viewModelScope.launch {
            _selection.value = repo.allUris().toMap()
        }
    }

    fun selectedUris(): List<Uri> = _selection.value.values.toList()

    private companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 6
    }
}
