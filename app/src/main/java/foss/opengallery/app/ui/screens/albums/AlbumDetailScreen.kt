package foss.opengallery.app.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import foss.opengallery.app.data.AlbumSort
import foss.opengallery.app.data.MediaActions
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.OgIcons.drawClose
import foss.opengallery.app.ui.components.OgIcons.drawShare
import foss.opengallery.app.ui.components.OgIcons.drawTrash
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.components.SelectionAction
import foss.opengallery.app.ui.components.SelectionBadge
import foss.opengallery.app.ui.components.SelectionBottomBar
import foss.opengallery.app.ui.components.SelectionHeader
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/**
 * One album's grid: bucket, virtual or custom. Title + "N images M videos"
 * subtitle, sort menu with persistent per-album order, multi-select.
 */
@Composable
fun AlbumDetailScreen(
    type: String,
    id: String,
    title: String,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val vm = ogViewModel(key = "album:$type:$id") { c ->
        AlbumDetailViewModel(c.mediaRepository, c.albumsRepository, type, id)
    }
    val items = vm.items.collectAsLazyPagingItems()
    val counts by vm.counts.collectAsState()
    val selection by vm.selection.collectAsState()
    val selectionMode by vm.selectionMode.collectAsState()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }

    // 30+: system trash in binder-safe batches. 26–29: the app recycle bin,
    // per the recycle-bin hard rule — never straight to permanent deletion.
    val systemTrashRunner =
        foss.opengallery.app.ui.components.rememberSystemTrashRunner { vm.clearSelection() }
    val legacyTrashRunner =
        foss.opengallery.app.ui.components.rememberLegacyTrashRunner { vm.clearSelection() }

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        if (selectionMode) {
            SelectionHeader(selectedCount = selection.size)
        } else {
            CompactHeaderBar(
                title = title,
                visible = true,
                subtitle = counts?.let { (img, vid) ->
                    buildString {
                        if (img > 0) append("$img image").append(if (img > 1) "s" else "")
                        if (img > 0 && vid > 0) append(" ")
                        if (vid > 0) append("$vid video").append(if (vid > 1) "s" else "")
                        if (img == 0 && vid == 0) append("No items")
                    }
                },
                actions = listOf(HeaderAction.Back, HeaderAction.More),
                onAction = { action ->
                    when (action) {
                        HeaderAction.Back -> onBack()
                        HeaderAction.More -> menuOpen = true
                        else -> {}
                    }
                },
            )
        }
        // Anchor at the right edge so the menu drops under the ⋮ button.
        Box(Modifier.align(Alignment.End).padding(end = 8.dp)) {
            OneUiPopupMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                entries = listOf(
                    PopupEntry("Edit") { vm.enterSelection() },
                    PopupEntry("Select all") {
                        vm.enterSelection()
                        vm.selectAll()
                    },
                    PopupEntry("Sort") { sortOpen = true },
                ),
            )
            OneUiPopupMenu(
                expanded = sortOpen,
                onDismiss = { sortOpen = false },
                entries = AlbumSort.entries.map { sort ->
                    PopupEntry(sort.label) { vm.setSort(sort) }
                },
            )
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) { index ->
                    val item = items[index] ?: return@items
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(0.75.dp)
                            .background(OgColors.SurfaceChip)
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) vm.toggleSelected(item)
                                    else onOpenItem(item)
                                },
                                onLongClick = {
                                    if (!selectionMode) vm.enterSelection(item)
                                },
                            ),
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (item.id in selection) 0.6f else 1f),
                        )
                        if (item.isVideo) {
                            Text(
                                text = formatDuration(item.durationMs),
                                style = OgType.ItemSecondary,
                                color = OgColors.TextPrimary,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp),
                            )
                        }
                        if (selectionMode) {
                            SelectionBadge(
                                selected = item.id in selection,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp),
                            )
                        }
                    }
                }
            }
        }

        if (selectionMode) {
            SelectionBottomBar(
                actions = listOf(
                    SelectionAction("Cancel", icon = { c, w -> drawClose(c, w) }) {
                        vm.clearSelection()
                    },
                    SelectionAction(
                        "Share",
                        enabled = selection.isNotEmpty(),
                        icon = { c, w -> drawShare(c, w) },
                    ) {
                        context.startActivity(MediaActions.shareIntent(vm.selectedUris()))
                    },
                    SelectionAction(
                        "Delete",
                        enabled = selection.isNotEmpty(),
                        icon = { c, w -> drawTrash(c, w) },
                    ) {
                        val uris = vm.selectedUris()
                        if (MediaActions.canUseSystemTrash()) {
                            systemTrashRunner.start(uris)
                        } else {
                            legacyTrashRunner.start(uris)
                        }
                    },
                )
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return if (totalSec >= 3600) {
        "%d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
    } else {
        "%d:%02d".format(totalSec / 60, totalSec % 60)
    }
}
