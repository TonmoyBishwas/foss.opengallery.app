package foss.opengallery.app.ui.screens.pictures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.permissions.MediaAccessGate
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/**
 * Pictures tab. M2 state: permission gate + a fast paged grid proving the
 * media pipeline. The full timeline treatment (collapsing hero title, date
 * headers, pinch density, scrubber, selection) lands in M3.
 */
@Composable
fun PicturesScreen() {
    MediaAccessGate { _ ->
        val vm = ogViewModel { container ->
            PicturesViewModel(container.mediaRepository)
        }
        val items = vm.timeline.collectAsLazyPagingItems()

        if (items.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pictures yet", style = OgType.Subtitle, color = OgColors.TextSecondary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .background(OgColors.Background),
            ) {
                items(
                    count = items.itemCount,
                    key = items.itemKey { it.id },
                ) { index ->
                    val item = items[index] ?: return@items
                    MediaThumb(item)
                }
            }
        }
    }
}

@Composable
private fun MediaThumb(item: MediaItem) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(0.75.dp)
            .background(OgColors.SurfaceChip)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
