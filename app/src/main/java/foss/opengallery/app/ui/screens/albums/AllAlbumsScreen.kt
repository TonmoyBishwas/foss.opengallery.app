package foss.opengallery.app.ui.screens.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.Routes
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.HeroHeader
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/** View modes for All albums: flat card grid or the nested folder tree. */
private enum class AllAlbumsMode { Grid, Folders }

/**
 * "All albums" — every folder album, with a switchable nested-folders view
 * (the most-requested feature missing from stock galleries).
 */
@Composable
fun AllAlbumsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val vm = ogViewModel { c -> AlbumsViewModel(c.albumsRepository, c.mediaRepository) }
    val albums by vm.albums.collectAsState()
    val gridState = rememberLazyGridState()
    var menuOpen by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(AllAlbumsMode.Grid) }
    var folderPath by remember { mutableStateOf("") } // current tree prefix
    val heroCollapsed by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    val visibleAlbums = albums.filter { !it.hidden }

    fun folderUp() {
        folderPath = folderPath.trimEnd('/')
            .substringBeforeLast('/', "")
            .let { if (it.isEmpty()) "" else "$it/" }
    }
    // System back mirrors the header Back arrow while inside the tree.
    BackHandler(enabled = mode == AllAlbumsMode.Folders && folderPath.isNotEmpty()) {
        folderUp()
    }

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "All albums",
            visible = true,
            actions = listOf(HeaderAction.Back, HeaderAction.More),
            onAction = { action ->
                when (action) {
                    HeaderAction.Back -> {
                        if (mode == AllAlbumsMode.Folders && folderPath.isNotEmpty()) {
                            folderUp()
                        } else onBack()
                    }
                    HeaderAction.More -> menuOpen = true
                    else -> {}
                }
            },
        )
        // Anchor at the right edge so the menu drops under the ⋮ button.
        Box(Modifier.align(Alignment.End).padding(end = 8.dp)) {
            OneUiPopupMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                entries = listOf(
                    PopupEntry(
                        if (mode == AllAlbumsMode.Grid) "Show folder tree" else "Show album grid"
                    ) {
                        mode =
                            if (mode == AllAlbumsMode.Grid) AllAlbumsMode.Folders
                            else AllAlbumsMode.Grid
                        folderPath = ""
                    },
                ),
            )
        }

        when (mode) {
            AllAlbumsMode.Grid -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleAlbums, key = { it.bucket.bucketId }) { entry ->
                        AlbumCard(
                            title = entry.bucket.name,
                            count = entry.bucket.itemCount,
                            cover = entry.bucket.coverItem,
                            onClick = {
                                onNavigate(
                                    Routes.bucketAlbum(entry.bucket.bucketId, entry.bucket.name)
                                )
                            },
                        )
                    }
                }
            }
            AllAlbumsMode.Folders -> {
                FolderTree(
                    albums = visibleAlbums,
                    path = folderPath,
                    onEnterFolder = { folderPath = it },
                    onOpenAlbum = { entry ->
                        onNavigate(Routes.bucketAlbum(entry.bucket.bucketId, entry.bucket.name))
                    },
                )
            }
        }
    }
}

/**
 * Nested folder navigation built from each bucket's RELATIVE_PATH
 * (e.g. "DCIM/Camera/"). Directories first, then albums at this level.
 */
@Composable
private fun FolderTree(
    albums: List<foss.opengallery.app.data.AlbumEntry>,
    path: String,
    onEnterFolder: (String) -> Unit,
    onOpenAlbum: (foss.opengallery.app.data.AlbumEntry) -> Unit,
) {
    val childDirs = remember(albums, path) {
        albums.mapNotNull { entry ->
            val rel = entry.bucket.relativePath
            if (!rel.startsWith(path)) return@mapNotNull null
            val remainder = rel.removePrefix(path).trimEnd('/')
            if (remainder.isEmpty()) null
            else remainder.substringBefore('/')
        }.distinct().sorted()
    }
    val leafAlbums = remember(albums, path) {
        albums.filter { it.bucket.relativePath.trimEnd('/') == path.trimEnd('/') && path.isNotEmpty() }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(1), modifier = Modifier.fillMaxSize()) {
        if (path.isNotEmpty()) {
            item(key = "path") {
                Text(
                    text = "/$path",
                    style = OgType.Subtitle,
                    color = OgColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
        items(childDirs, key = { "d:$it" }) { dir ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEnterFolder("$path$dir/") }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📁", modifier = Modifier.padding(end = 14.dp))
                Text(dir, style = OgType.ItemLabel, color = OgColors.TextPrimary)
            }
        }
        items(leafAlbums, key = { "a:${it.bucket.bucketId}" }) { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(entry) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🖼", modifier = Modifier.padding(end = 14.dp))
                Column {
                    Text(entry.bucket.name, style = OgType.ItemLabel, color = OgColors.TextPrimary)
                    Text(
                        "${entry.bucket.itemCount} items",
                        style = OgType.ItemSecondary,
                        color = OgColors.TextSecondary,
                    )
                }
            }
        }
    }
}
