package foss.opengallery.app.ui.screens.recycle

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaActions
import foss.opengallery.app.data.db.TrashedItemEntity
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.data.trash.TrashRepository
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.OneUiPopupMenu
import foss.opengallery.app.ui.components.PopupEntry
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecycleBinViewModel(
    application: Application,
    private val trash: TrashRepository,
) : AndroidViewModel(application) {

    private val _systemItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val systemItems: StateFlow<List<MediaItem>> = _systemItems.asStateFlow()

    val legacyItems = trash.legacyItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usesSystemTrash get() = trash.usesSystemTrash

    init {
        refresh()
        viewModelScope.launch { trash.purgeExpired() }
    }

    fun refresh() {
        viewModelScope.launch { _systemItems.value = trash.systemTrashedItems() }
    }

    fun legacyRestore(entity: TrashedItemEntity) {
        viewModelScope.launch { trash.legacyRestore(entity) }
    }

    fun legacyDelete(entity: TrashedItemEntity) {
        viewModelScope.launch { trash.legacyDeleteForever(entity) }
    }
}

/** Recycle bin: 30-day retention note, restore / delete-forever / empty. */
@Composable
fun RecycleBinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vm = ogViewModel { c ->
        RecycleBinViewModel(
            context.applicationContext as Application,
            c.trashRepository,
        )
    }
    val systemItems by vm.systemItems.collectAsState()
    val legacyItems by vm.legacyItems.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<TrashedItemEntity?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { vm.refresh() }

    val count = if (vm.usesSystemTrash) systemItems.size else legacyItems.size

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Recycle bin",
            visible = true,
            subtitle = if (count == 1) "1 item" else "$count items",
            actions = listOf(HeaderAction.Back, HeaderAction.More),
            onAction = { action ->
                when (action) {
                    HeaderAction.Back -> onBack()
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
                entries = buildList {
                    if (vm.usesSystemTrash && systemItems.isNotEmpty()) {
                        add(PopupEntry("Restore all") {
                            consentLauncher.launch(
                                IntentSenderRequest.Builder(
                                    MediaActions.trashRequest(
                                        context.contentResolver,
                                        systemItems.map { it.uri },
                                        trash = false,
                                    ).intentSender
                                ).build()
                            )
                        })
                        add(PopupEntry("Empty") {
                            consentLauncher.launch(
                                IntentSenderRequest.Builder(
                                    MediaActions.deleteRequest(
                                        context.contentResolver,
                                        systemItems.map { it.uri },
                                    ).intentSender
                                ).build()
                            )
                        })
                    }
                },
            )
        }

        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Pictures and videos you delete stay in the Recycle bin " +
                        "for 30 days before being permanently deleted.",
                    style = OgType.Body,
                    color = OgColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }
            if (vm.usesSystemTrash) {
                items(systemItems, key = { it.id }) { item ->
                    TrashThumb(
                        model = item.uri,
                        daysLeft = null,
                        onClick = {
                            consentLauncher.launch(
                                IntentSenderRequest.Builder(
                                    MediaActions.trashRequest(
                                        context.contentResolver,
                                        listOf(item.uri),
                                        trash = false,
                                    ).intentSender
                                ).build()
                            )
                        },
                    )
                }
            } else {
                items(legacyItems, key = { it.id }) { entity ->
                    val daysLeft = 30 - ((System.currentTimeMillis() - entity.trashedAtMillis) /
                        (24L * 60 * 60 * 1000)).toInt()
                    TrashThumb(
                        model = java.io.File(entity.storedPath),
                        daysLeft = daysLeft.coerceAtLeast(0),
                        onClick = { vm.legacyRestore(entity) },
                        onLongClick = { confirmDelete = entity },
                    )
                }
            }
        }
    }

    confirmDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = OgColors.SurfacePopup,
            title = {
                Text("Delete permanently?", style = OgType.SectionHeader, color = OgColors.TextPrimary)
            },
            text = {
                Text(
                    "“${entity.originalName}” will be deleted forever. It cannot be recovered.",
                    style = OgType.Body,
                    color = OgColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.legacyDelete(entity)
                    confirmDelete = null
                }) { Text("Delete", color = OgColors.AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("Cancel", color = OgColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun TrashThumb(
    model: Any?,
    daysLeft: Int?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(0.75.dp)
            .background(OgColors.SurfaceChip)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick ?: {}),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (daysLeft != null) {
            Text(
                text = "$daysLeft days",
                style = OgType.ItemSecondary,
                color = OgColors.TextPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(4.dp),
            )
        }
    }
}
