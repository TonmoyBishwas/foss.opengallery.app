package foss.opengallery.app.ui.screens.pictures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import foss.opengallery.app.data.MediaRepository
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PicturesViewModel(private val repo: MediaRepository) : ViewModel() {

    private val pagerAndInvalidate = repo.timelinePager()

    val timeline: Flow<PagingData<MediaItem>> =
        pagerAndInvalidate.first.flow.cachedIn(viewModelScope)

    init {
        // Refresh the paging data whenever MediaStore content changes.
        repo.changes
            .onEach { pagerAndInvalidate.second.invoke() }
            .launchIn(viewModelScope)
    }
}
