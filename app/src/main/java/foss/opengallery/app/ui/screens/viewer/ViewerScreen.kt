package foss.opengallery.app.ui.screens.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaActions
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.HeaderIconButton
import foss.opengallery.app.ui.components.OgIcons.drawHeart
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.launch

/**
 * Fullscreen viewer: swipe pager with zoom, filmstrip, favourite/edit/
 * info/share/delete actions, and the overflow menu (copy to clipboard,
 * set as wallpaper, rename, print).
 */
@Composable
fun ViewerScreen(
    type: String,
    id: String,
    startMediaId: Long,
    sortEncoded: Int,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel(key = "viewer:$type:$id:$startMediaId") { c ->
        ViewerViewModel(
            application = context.applicationContext as android.app.Application,
            albums = c.albumsRepository,
            type = type,
            id = id,
            startMediaId = startMediaId,
            sortEncoded = sortEncoded,
        )
    }
    val count by vm.count.collectAsState()
    val initialIndex by vm.initialIndex.collectAsState()

    if (count == 0 || initialIndex == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(initialPage = initialIndex!!, pageCount = { count })
    val scope = rememberCoroutineScope()
    var chromeVisible by remember { mutableStateOf(true) }
    var pageZoomed by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var detailsFor by remember { mutableStateOf<MediaItem?>(null) }

    // Current item for the action bar / menus.
    val currentItem by produceState<MediaItem?>(null, pagerState.currentPage, count) {
        value = vm.itemAt(pagerState.currentPage)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            currentItem?.let { vm.onDeleted(it.id) }
            if (vm.count.value == 0) onBack()
        }
    }
    val favLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !pageZoomed,
            key = { page -> vm.idAt(page) ?: page.toLong() },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item by produceState<MediaItem?>(null, page) { value = vm.itemAt(page) }
            when {
                item == null -> Box(Modifier.fillMaxSize())
                item!!.isVideo -> VideoPage(
                    uri = item!!.uri,
                    isCurrentPage = pagerState.currentPage == page,
                    onTap = { chromeVisible = !chromeVisible },
                )
                else -> ZoomableImage(
                    model = item!!.uri,
                    contentDescription = item!!.displayName,
                    onTap = { chromeVisible = !chromeVisible },
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) pageZoomed = zoomed
                    },
                )
            }
        }

        // Top chrome: back + overflow.
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderIconButton(HeaderAction.Back) { onBack() }
                Box(Modifier.weight(1f))
                HeaderIconButton(HeaderAction.More) { menuOpen = true }
                OneUiPopupMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    entries = buildList {
                        add(PopupEntry("Copy to clipboard") {
                            currentItem?.let { copyToClipboard(context, it) }
                        })
                        add(PopupEntry("Set as wallpaper") {
                            currentItem?.let { setAsWallpaper(context, it) }
                        })
                        add(PopupEntry("Print") {
                            currentItem?.let { printImage(context, it) }
                        })
                    },
                )
            }
        }

        // Bottom chrome: filmstrip + actions.
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(OgColors.ScrimVeil)
                    .navigationBarsPadding()
            ) {
                Filmstrip(
                    vm = vm,
                    count = count,
                    currentPage = pagerState.currentPage,
                    onJump = { page -> scope.launch { pagerState.scrollToPage(page) } },
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Favourite
                    val interaction = remember { MutableInteractionSource() }
                    Canvas(
                        Modifier
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                            ) {
                                val item = currentItem ?: return@clickable
                                if (MediaActions.canUseSystemTrash()) {
                                    favLauncher.launch(
                                        IntentSenderRequest.Builder(
                                            MediaActions.favoriteRequest(
                                                context.contentResolver,
                                                listOf(item.uri),
                                                !item.isFavorite,
                                            ).intentSender
                                        ).build()
                                    )
                                }
                            }
                            .padding(14.dp)
                            .size(26.dp)
                    ) {
                        drawHeart(
                            color = if (currentItem?.isFavorite == true) OgColors.FavouriteHeart
                            else OgColors.TextPrimary,
                            filled = currentItem?.isFavorite == true,
                            strokeWidth = 2.2.dp.toPx(),
                        )
                    }
                    ViewerAction("Edit") {
                        currentItem?.let {
                            runCatching {
                                context.startActivity(MediaActions.editIntent(it.uri, it.mimeType))
                            }
                        }
                    }
                    ViewerAction("Info") { detailsFor = currentItem }
                    ViewerAction("Share") {
                        currentItem?.let {
                            context.startActivity(
                                MediaActions.shareIntent(listOf(it.uri), it.mimeType)
                            )
                        }
                    }
                    ViewerAction("Delete") {
                        val item = currentItem ?: return@ViewerAction
                        if (MediaActions.canUseSystemTrash()) {
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(
                                    MediaActions.trashRequest(
                                        context.contentResolver, listOf(item.uri)
                                    ).intentSender
                                ).build()
                            )
                        } else {
                            scope.launch {
                                MediaActions.deleteDirect(context, listOf(item.uri))
                                vm.onDeleted(item.id)
                                if (vm.count.value == 0) onBack()
                            }
                        }
                    }
                }
            }
        }
    }

    detailsFor?.let { item ->
        DetailsSheet(item = item, onDismiss = { detailsFor = null })
    }
}

@Composable
private fun ViewerAction(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = OgType.ItemLabel,
        color = OgColors.TextPrimary,
        modifier = Modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 14.dp),
    )
}

@Composable
private fun Filmstrip(
    vm: ViewerViewModel,
    count: Int,
    currentPage: Int,
    onJump: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentPage) {
        listState.animateScrollToItem(currentPage.coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(count = count, key = { it }) { page ->
            val item by produceState<MediaItem?>(null, page) { value = vm.itemAt(page) }
            Box(
                Modifier
                    .width(if (page == currentPage) 64.dp else 40.dp)
                    .fillMaxSize()
                    .clip(foss.opengallery.app.ui.theme.OgShapes.AlbumCover.copy(
                        all = androidx.compose.foundation.shape.CornerSize(8.dp)
                    ))
                    .background(OgColors.SurfaceChip)
                    .then(
                        if (page == currentPage)
                            Modifier.border(
                                1.5.dp, OgColors.TextPrimary,
                                foss.opengallery.app.ui.theme.OgShapes.AlbumCover.copy(
                                    all = androidx.compose.foundation.shape.CornerSize(8.dp)
                                )
                            )
                        else Modifier
                    )
                    .clickable { onJump(page) },
            ) {
                AsyncImage(
                    model = item?.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, item: MediaItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, item.displayName, item.uri))
}

private fun setAsWallpaper(context: Context, item: MediaItem) {
    runCatching {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_ATTACH_DATA).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(item.uri, item.mimeType)
                    putExtra("mimeType", item.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Set as",
            )
        )
    }
}

private fun printImage(context: Context, item: MediaItem) {
    runCatching {
        val helper = androidx.print.PrintHelper(context)
        helper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        helper.printBitmap(item.displayName, item.uri)
    }
}
