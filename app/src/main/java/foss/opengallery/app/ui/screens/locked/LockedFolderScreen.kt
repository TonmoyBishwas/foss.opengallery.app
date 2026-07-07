package foss.opengallery.app.ui.screens.locked

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.data.db.LockedItemEntity
import foss.opengallery.app.data.locked.LockedStore
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.OgIcons.drawPlay
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LockedFolderViewModel(
    application: Application,
    private val store: LockedStore,
) : AndroidViewModel(application) {

    val items = store.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Decoded thumbs, so scrolling doesn't re-decrypt files. ~24 MB cap. */
    private val thumbCache = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    /**
     * Decrypt a thumbnail-sized bitmap without ever touching disk — two
     * streaming passes (bounds, then sampled decode) instead of buffering
     * the whole plaintext, which OOMed on large files. Videos can't be
     * BitmapFactory-decoded; they render as a placeholder tile.
     */
    suspend fun thumbnail(item: LockedItemEntity): Bitmap? {
        if (item.mimeType.startsWith("video/")) return null
        thumbCache.get(item.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            store.openDecrypted(item.id)?.second?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return@withContext null
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
            val sample = calculateSample(bounds.outWidth, bounds.outHeight, 512)
            val bitmap = store.openDecrypted(item.id)?.second?.use { input ->
                BitmapFactory.decodeStream(
                    input, null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            }
            bitmap?.also { thumbCache.put(item.id, it) }
        }
    }

    fun restoreToGallery(item: LockedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val isVideo = item.mimeType.startsWith("video/")
            val collection = if (Build.VERSION.SDK_INT >= 29) {
                if (isVideo)
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val target = resolver.insert(collection, values) ?: return@launch
            // The encrypted item is only deleted after a verified write —
            // a failed restore must never destroy the sole copy.
            val ok = runCatching {
                resolver.openOutputStream(target)?.use { output ->
                    store.restore(item.id, output)
                } ?: false
            }.getOrDefault(false)
            if (ok) {
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.update(
                        target,
                        ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                        null, null,
                    )
                }
                store.delete(item.id)
            } else {
                runCatching { resolver.delete(target, null, null) }
            }
        }
    }

    fun deleteForever(item: LockedItemEntity) {
        viewModelScope.launch { store.delete(item.id) }
    }

    private fun calculateSample(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= target && height / (sample * 2) >= target) {
            sample *= 2
        }
        return sample
    }
}

/** The Locked Folder screen (behind [LockedAuthGate]). */
@Composable
fun LockedFolderScreen(onBack: () -> Unit) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Locked folder",
            visible = true,
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LockedAuthGate(unlocked = unlocked, onUnlocked = { unlocked = true }) {
            val vm = ogViewModel { c ->
                LockedFolderViewModel(
                    context.applicationContext as Application,
                    c.lockedStore,
                )
            }
            val items by vm.items.collectAsState()
            var confirmRestore by remember { mutableStateOf<LockedItemEntity?>(null) }
            var confirmDelete by remember { mutableStateOf<LockedItemEntity?>(null) }

            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Items here are encrypted on this device and hidden from " +
                            "every other app. Tap an item to move it back to your gallery; " +
                            "long-press to delete it permanently.",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
                items(items, key = { it.id }) { item ->
                    val bitmap by produceState<Bitmap?>(null, item.id) {
                        value = vm.thumbnail(item)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(0.75.dp)
                            .background(OgColors.SurfaceChip)
                            .combinedClickable(
                                onClick = { confirmRestore = item },
                                onLongClick = { confirmDelete = item },
                            ),
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = item.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (item.mimeType.startsWith("video/")) {
                            Canvas(Modifier.align(Alignment.Center).size(28.dp)) {
                                drawPlay(OgColors.TextPrimary)
                            }
                        }
                    }
                }
            }

            confirmRestore?.let { item ->
                LockedConfirmDialog(
                    title = "Move back to gallery?",
                    text = "“${item.displayName}” will be decrypted and " +
                        "restored to your gallery, then removed from the Locked folder.",
                    confirmLabel = "Move",
                    onConfirm = {
                        vm.restoreToGallery(item)
                        confirmRestore = null
                    },
                    onDismiss = { confirmRestore = null },
                )
            }
            confirmDelete?.let { item ->
                LockedConfirmDialog(
                    title = "Delete permanently?",
                    text = "“${item.displayName}” will be deleted forever. " +
                        "It cannot be recovered.",
                    confirmLabel = "Delete",
                    onConfirm = {
                        vm.deleteForever(item)
                        confirmDelete = null
                    },
                    onDismiss = { confirmDelete = null },
                )
            }
        }
    }
}

@Composable
private fun LockedConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OgColors.SurfacePopup,
        title = { Text(title, style = OgType.SectionHeader, color = OgColors.TextPrimary) },
        text = { Text(text, style = OgType.Body, color = OgColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = OgColors.AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OgColors.TextSecondary)
            }
        },
    )
}
