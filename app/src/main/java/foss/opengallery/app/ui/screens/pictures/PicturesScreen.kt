package foss.opengallery.app.ui.screens.pictures

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaActions
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.FastScrubber
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.HeroHeader
import foss.opengallery.app.ui.components.OgIcons.drawPlay
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.components.SelectionAction
import foss.opengallery.app.ui.components.SelectionBadge
import foss.opengallery.app.ui.components.SelectionBottomBar
import foss.opengallery.app.ui.components.SelectionHeader
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.permissions.MediaAccessGate
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.launch

/**
 * The Pictures tab: hero title, date-grouped paged timeline, pinch density,
 * fast scrubber, long-press multi-select with the bottom action bar.
 *
 * @param onOpenItem invoked when a thumb is tapped outside selection mode.
 * @param onSelectionModeChange lets the app shell hide the main tab bar.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PicturesScreen(
    onOpenItem: (MediaItem) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
) {
    MediaAccessGate { _ ->
        val vm = ogViewModel { c -> PicturesViewModel(c.mediaRepository) }
        val cells = vm.timeline.collectAsLazyPagingItems()
        val columns by vm.columns.collectAsState()
        val selection by vm.selection.collectAsState()
        val selectionMode by vm.selectionMode.collectAsState()
        val gridState = rememberLazyGridState()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var menuOpen by remember { mutableStateOf(false) }

        // Tell the shell to swap the tab bar for the selection action bar.
        androidx.compose.runtime.LaunchedEffect(selectionMode) {
            onSelectionModeChange(selectionMode)
        }

        val trashLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { vm.clearSelection() }

        fun selectedUris(): List<Uri> = (0 until cells.itemCount).mapNotNull { i ->
            val cell = cells.peek(i) as? TimelineCell.Media ?: return@mapNotNull null
            if (cell.item.id in selection) cell.item.uri else null
        }

        val heroCollapsed by remember {
            derivedStateOf { gridState.firstVisibleItemIndex > 0 }
        }

        Column(Modifier.fillMaxSize().background(OgColors.Background)) {
            if (selectionMode) {
                SelectionHeader(selectedCount = selection.size)
            } else {
                CompactHeaderBar(
                    title = "Pictures",
                    visible = heroCollapsed,
                    actions = listOf(HeaderAction.Search, HeaderAction.More),
                    onAction = { action ->
                        when (action) {
                            HeaderAction.Search -> onOpenSearch()
                            HeaderAction.More -> menuOpen = true
                            else -> {}
                        }
                    },
                )
            }

            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .pinchColumns(onPinchIn = vm::pinchIn, onPinchOut = vm::pinchOut),
                ) {
                    if (!selectionMode) {
                        item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                            Box {
                                HeroHeader(
                                    title = "Pictures",
                                    heroHeight = 340.dp,
                                    actions = listOf(
                                        HeaderAction.MultiView,
                                        HeaderAction.Search,
                                        HeaderAction.More,
                                    ),
                                    onAction = { action ->
                                        when (action) {
                                            HeaderAction.Search -> onOpenSearch()
                                            HeaderAction.More -> menuOpen = true
                                            HeaderAction.MultiView -> vm.pinchOut()
                                            else -> {}
                                        }
                                    },
                                )
                                Box(Modifier.align(Alignment.BottomEnd)) {
                                    PicturesOverflowMenu(
                                        expanded = menuOpen,
                                        onDismiss = { menuOpen = false },
                                        onEdit = { vm.enterSelection() },
                                        onSelectAll = {
                                            vm.enterSelection()
                                            vm.selectAll(
                                                (0 until cells.itemCount).mapNotNull { i ->
                                                    (cells.peek(i) as? TimelineCell.Media)?.item?.id
                                                }
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                    items(
                        count = cells.itemCount,
                        key = cells.itemKey { cell ->
                            when (cell) {
                                is TimelineCell.Header -> "h:${cell.key}"
                                is TimelineCell.Media -> "m:${cell.item.id}"
                            }
                        },
                        span = { index ->
                            when (cells.peek(index)) {
                                is TimelineCell.Header -> GridItemSpan(maxLineSpan)
                                else -> GridItemSpan(1)
                            }
                        },
                    ) { index ->
                        when (val cell = cells[index]) {
                            is TimelineCell.Header -> DateHeader(cell.label)
                            is TimelineCell.Media -> TimelineThumb(
                                item = cell.item,
                                selectionMode = selectionMode,
                                selected = cell.item.id in selection,
                                onClick = {
                                    if (selectionMode) vm.toggleSelected(cell.item)
                                    else onOpenItem(cell.item)
                                },
                                onLongClick = {
                                    if (!selectionMode) vm.enterSelection(cell.item)
                                },
                            )
                            null -> {}
                        }
                    }
                }

                FastScrubber(
                    gridState = gridState,
                    itemCount = cells.itemCount,
                    labelForIndex = { index ->
                        (cells.peek(index.coerceIn(0, cells.itemCount - 1))
                            as? TimelineCell.Media)
                            ?.item
                            ?.let(TimelineFormat::scrubberLabel)
                    },
                )
            }
        }

        if (selectionMode) {
            SelectionBottomBar(
                actions = listOf(
                    SelectionAction("Cancel") { vm.clearSelection() },
                    SelectionAction("Share", enabled = selection.isNotEmpty()) {
                        context.startActivity(MediaActions.shareIntent(selectedUris()))
                    },
                    SelectionAction("Delete", enabled = selection.isNotEmpty()) {
                        val uris = selectedUris()
                        if (MediaActions.canUseSystemTrash()) {
                            trashLauncher.launch(
                                IntentSenderRequest.Builder(
                                    MediaActions.trashRequest(context.contentResolver, uris)
                                        .intentSender
                                ).build()
                            )
                        } else {
                            scope.launch {
                                MediaActions.deleteDirect(context, uris)
                                vm.clearSelection()
                            }
                        }
                    },
                )
            )
        }
    }
}

@Composable
private fun PicturesOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSelectAll: () -> Unit,
) {
    OneUiPopupMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        entries = listOf(
            PopupEntry("Edit", onClick = onEdit),
            PopupEntry("Select all", onClick = onSelectAll),
            PopupEntry("Create", enabled = false, onClick = {}),
            PopupEntry("Start slideshow", enabled = false, onClick = {}),
        ),
    )
}

@Composable
private fun DateHeader(label: String) {
    Text(
        text = label,
        style = OgType.SectionHeader,
        color = OgColors.TitleBlue,
        modifier = Modifier
            .fillMaxWidth()
            .background(OgColors.Background)
            .padding(start = 18.dp, top = 22.dp, bottom = 10.dp),
    )
}

@Composable
private fun TimelineThumb(
    item: MediaItem,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(0.75.dp)
            .background(OgColors.SurfaceChip)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (selected) 0.6f else 1f),
        )
        if (item.isVideo) {
            VideoBadge(
                durationMs = item.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
        if (selectionMode) {
            SelectionBadge(
                selected = selected,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun VideoBadge(durationMs: Long, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(14.dp)) { drawPlay(OgColors.TextPrimary) }
        val totalSec = durationMs / 1000
        val text = if (totalSec >= 3600) {
            "%d:%02d:%02d".format(totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
        } else {
            "%d:%02d".format(totalSec / 60, totalSec % 60)
        }
        Text(
            text = text,
            style = OgType.ItemSecondary,
            color = OgColors.TextPrimary,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}

/**
 * Two-finger pinch that steps grid density without stealing single-finger
 * scrolling.
 */
private fun Modifier.pinchColumns(
    onPinchIn: () -> Unit,
    onPinchOut: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var accumulated = 1f
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
            if (event.changes.size > 1) {
                accumulated *= event.calculateZoom()
                event.changes.forEach { it.consume() }
                if (accumulated > 1.3f) {
                    onPinchIn() // zoom in = bigger thumbs = fewer columns
                    accumulated = 1f
                } else if (accumulated < 0.77f) {
                    onPinchOut()
                    accumulated = 1f
                }
            }
        }
    }
}
