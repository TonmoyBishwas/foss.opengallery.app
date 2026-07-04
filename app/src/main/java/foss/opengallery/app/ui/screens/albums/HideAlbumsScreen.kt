package foss.opengallery.app.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

/** Hide albums: per-folder switches, hidden folders vanish from Albums and Pictures. */
@Composable
fun HideAlbumsScreen(onBack: () -> Unit) {
    val vm = ogViewModelShared()
    val albums by vm.albums.collectAsState()

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Hide albums",
            visible = true,
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(albums, key = { it.bucket.bucketId }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = entry.bucket.coverItem?.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(OgShapes.AlbumCover),
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    ) {
                        Text(entry.bucket.name, style = OgType.ItemLabel, color = OgColors.TextPrimary)
                        Text(
                            "${entry.bucket.itemCount} items",
                            style = OgType.ItemSecondary,
                            color = OgColors.TextSecondary,
                        )
                    }
                    Switch(
                        checked = entry.hidden,
                        onCheckedChange = { vm.setHidden(entry.bucket.bucketId, it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = OgColors.SwitchTrackOn,
                            uncheckedTrackColor = OgColors.SwitchTrackOff,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ogViewModelShared(): AlbumsViewModel =
    foss.opengallery.app.ui.ogViewModel { c ->
        AlbumsViewModel(c.albumsRepository, c.mediaRepository)
    }
