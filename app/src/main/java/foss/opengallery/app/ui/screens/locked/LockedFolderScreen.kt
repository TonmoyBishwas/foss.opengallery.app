package foss.opengallery.app.ui.screens.locked

import android.app.Application
import android.content.ContentValues
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
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

    /** Decrypt a thumbnail-sized bitmap fully in memory (never on disk). */
    suspend fun thumbnail(item: LockedItemEntity): android.graphics.Bitmap? =
        withContext(Dispatchers.IO) {
            val (_, stream) = store.openDecrypted(item.id) ?: return@withContext null
            stream.use { input ->
                val bytes = input.readBytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = calculateSample(bounds.outWidth, bounds.outHeight, 512)
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            }
        }

    fun restoreToGallery(item: LockedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection =
                if (item.mimeType.startsWith("video/"))
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = resolver.insert(collection, values) ?: return@launch
            resolver.openOutputStream(target)?.use { output ->
                store.restore(item.id, output)
            }
            resolver.update(
                target,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null, null,
            )
            store.delete(item.id)
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
                    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) {
                        value = vm.thumbnail(item)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(0.75.dp)
                            .background(OgColors.SurfaceChip)
                            .combinedClickable(
                                onClick = { vm.restoreToGallery(item) },
                                onLongClick = { vm.deleteForever(item) },
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
                    }
                }
            }
        }
    }
}
