package foss.opengallery.app.ui.screens.suggestions

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/** A group of byte-identical files (candidate duplicates). */
data class DuplicateGroup(val items: List<MediaItem>)

class SuggestionsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _duplicates = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicates: StateFlow<List<DuplicateGroup>> = _duplicates.asStateFlow()

    private val _largeVideos = MutableStateFlow<List<MediaItem>>(emptyList())
    val largeVideos: StateFlow<List<MediaItem>> = _largeVideos.asStateFlow()

    private val _scanning = MutableStateFlow(true)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    init {
        viewModelScope.launch {
            scan()
            _scanning.value = false
        }
    }

    /**
     * Exact-duplicate detection: candidates share a byte size, confirmed
     * by MD5 of the first 256 KB + full size (fast and safe enough for
     * suggestions). Large videos: > 200 MB.
     */
    private suspend fun scan() = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val all = ArrayList<MediaItem>()
        var offset = 0
        while (true) {
            val page = MediaQuery.queryPage(resolver, offset = offset, limit = 1000)
            if (page.isEmpty()) break
            all.addAll(page)
            offset += page.size
            if (all.size >= 20_000) break
        }

        _largeVideos.value = all
            .filter { it.isVideo && it.sizeBytes > 200L * 1024 * 1024 }
            .sortedByDescending { it.sizeBytes }
            .take(30)

        val bySize = all
            .filter { it.sizeBytes > 8 * 1024 }
            .groupBy { it.sizeBytes }
            .filterValues { it.size > 1 }

        val groups = ArrayList<DuplicateGroup>()
        var hashed = 0
        outer@ for ((_, candidates) in bySize) {
            val byHash = HashMap<String, MutableList<MediaItem>>()
            for (item in candidates) {
                if (hashed >= MAX_HASHES) break@outer
                val hash = headHash(item) ?: continue
                hashed++
                byHash.getOrPut(hash) { mutableListOf() }.add(item)
            }
            byHash.values.filter { it.size > 1 }.forEach {
                groups.add(DuplicateGroup(it))
            }
            if (groups.size >= MAX_GROUPS) break
        }
        _duplicates.value = groups
    }

    private fun headHash(item: MediaItem): String? = runCatching {
        val md = MessageDigest.getInstance("MD5")
        getApplication<Application>().contentResolver.openInputStream(item.uri)?.use { input ->
            val buf = ByteArray(64 * 1024)
            var total = 0
            while (total < 256 * 1024) {
                val read = input.read(buf)
                if (read <= 0) break
                md.update(buf, 0, read)
                total += read
            }
        } ?: return null
        md.digest().joinToString("") { "%02x".format(it) } + ":" + item.sizeBytes
    }.getOrNull()

    private companion object {
        const val MAX_HASHES = 2000
        const val MAX_GROUPS = 50
    }
}

/** Suggestions: duplicate groups and oversized videos to clean up. */
@Composable
fun SuggestionsScreen(
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel { _ ->
        SuggestionsViewModel(context.applicationContext as Application)
    }
    val duplicates by vm.duplicates.collectAsState()
    val largeVideos by vm.largeVideos.collectAsState()
    val scanning by vm.scanning.collectAsState()

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Suggestions",
            visible = true,
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (scanning) {
                item {
                    Text(
                        "Scanning your library…",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            if (duplicates.isNotEmpty()) {
                item {
                    Text(
                        "Duplicates",
                        style = OgType.SectionHeader,
                        color = OgColors.TitleBlue,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
                items(duplicates, key = { it.items.first().id }) { group ->
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        Text(
                            "${group.items.size} identical copies · " +
                                formatSize(group.items.first().sizeBytes),
                            style = OgType.ItemSecondary,
                            color = OgColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        LazyRow {
                            items(group.items, key = { it.id }) { item ->
                                Box(
                                    Modifier
                                        .size(96.dp)
                                        .padding(2.dp)
                                        .clip(OgShapes.AlbumCover)
                                        .background(OgColors.SurfaceChip)
                                        .clickable { onOpenItem(item) },
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (largeVideos.isNotEmpty()) {
                item {
                    Text(
                        "Large videos",
                        style = OgType.SectionHeader,
                        color = OgColors.TitleBlue,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
                items(largeVideos, key = { "lv${it.id}" }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenItem(item) }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(OgShapes.AlbumCover)
                                .background(OgColors.SurfaceChip),
                        ) {
                            AsyncImage(
                                model = item.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text(
                                item.displayName,
                                style = OgType.ItemLabel,
                                color = OgColors.TextPrimary,
                            )
                            Text(
                                formatSize(item.sizeBytes),
                                style = OgType.ItemSecondary,
                                color = OgColors.TextSecondary,
                            )
                        }
                    }
                }
            }
            if (!scanning && duplicates.isEmpty() && largeVideos.isEmpty()) {
                item {
                    Text(
                        "Nothing to clean up — your library looks tidy.",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.0f KB".format(bytes / 1024.0)
}
