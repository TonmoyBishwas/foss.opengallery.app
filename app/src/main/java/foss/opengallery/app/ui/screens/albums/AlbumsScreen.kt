package foss.opengallery.app.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import foss.opengallery.app.data.VirtualAlbum
import foss.opengallery.app.ui.Routes
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.HeroHeader
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.permissions.MediaAccessGate
import foss.opengallery.app.ui.theme.OgColors

/**
 * Albums tab: hero title, "Essential albums" section (virtual albums) and
 * user albums, View all -> the full folder list.
 */
@Composable
fun AlbumsScreen(
    onNavigate: (String) -> Unit = {},
) {
    MediaAccessGate { _ ->
        val vm = ogViewModel { c -> AlbumsViewModel(c.albumsRepository, c.mediaRepository) }
        val virtuals by vm.virtuals.collectAsState()
        val customs by vm.customAlbums.collectAsState()
        val gridState = rememberLazyGridState()
        var menuOpen by remember { mutableStateOf(false) }
        var createOpen by remember { mutableStateOf(false) }
        val heroCollapsed by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

        Column(Modifier.fillMaxSize().background(OgColors.Background)) {
            CompactHeaderBar(
                title = "Albums",
                visible = heroCollapsed,
                actions = listOf(HeaderAction.Plus, HeaderAction.Search, HeaderAction.More),
                onAction = { action ->
                    when (action) {
                        HeaderAction.Plus -> createOpen = true
                        HeaderAction.Search -> onNavigate("search")
                        HeaderAction.More -> menuOpen = true
                        else -> {}
                    }
                },
            )
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        Box {
                            HeroHeader(
                                title = "Albums",
                                heroHeight = 320.dp,
                                actions = listOf(
                                    HeaderAction.Plus,
                                    HeaderAction.Search,
                                    HeaderAction.More,
                                ),
                                onAction = { action ->
                                    when (action) {
                                        HeaderAction.Plus -> createOpen = true
                                        HeaderAction.More -> menuOpen = true
                                        else -> {}
                                    }
                                },
                            )
                            Box(Modifier.align(Alignment.BottomEnd)) {
                                OneUiPopupMenu(
                                    expanded = menuOpen,
                                    onDismiss = { menuOpen = false },
                                    entries = listOf(
                                        PopupEntry("Create") { createOpen = true },
                                        PopupEntry("Hide albums") {
                                            onNavigate("hideAlbums")
                                        },
                                    ),
                                )
                            }
                        }
                    }

                    item(key = "essentialHeader", span = { GridItemSpan(maxLineSpan) }) {
                        AlbumsSectionHeader(
                            title = "Essential albums",
                            trailing = "View all",
                            onTrailingClick = { onNavigate(Routes.ALL_ALBUMS) },
                        )
                    }

                    items(virtuals, key = { "v:${it.album.key}" }) { v ->
                        AlbumCard(
                            title = v.album.title,
                            count = v.count,
                            cover = v.cover,
                            showDot = v.album == VirtualAlbum.Screenshots,
                            onClick = {
                                onNavigate(Routes.virtualAlbum(v.album.key, v.album.title))
                            },
                        )
                    }

                    if (customs.isNotEmpty()) {
                        item(key = "customHeader", span = { GridItemSpan(maxLineSpan) }) {
                            AlbumsSectionHeader(title = "My albums")
                        }
                        items(customs, key = { "c:${it.id}" }) { album ->
                            AlbumCard(
                                title = album.name,
                                count = null,
                                cover = null,
                                onClick = {
                                    onNavigate(Routes.customAlbum(album.id, album.name))
                                },
                            )
                        }
                    }
                }
            }
        }

        if (createOpen) {
            CreateAlbumSheet(
                onDismiss = { createOpen = false },
                onCreate = { name, isGroup ->
                    vm.createAlbum(name, isGroup)
                    createOpen = false
                },
            )
        }
    }
}
