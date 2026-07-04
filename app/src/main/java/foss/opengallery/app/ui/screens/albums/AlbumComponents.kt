package foss.opengallery.app.ui.screens.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

/**
 * Album card: big rounded cover, name below, count under the name —
 * the 3-per-row card from the Albums tab of the reference design.
 */
@Composable
fun AlbumCard(
    title: String,
    count: Int?,
    cover: MediaItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    showDot: Boolean = false,
) {
    Column(modifier.padding(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(OgShapes.AlbumCover)
                .background(OgColors.SurfaceCard)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick ?: {}),
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover.uri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        ) {
            Text(
                text = title,
                style = OgType.ItemLabel,
                color = OgColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDot) {
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .size(7.dp)
                        .background(OgColors.NotificationDot, OgShapes.Chip)
                )
            }
        }
        if (count != null) {
            Text(
                text = count.toString(),
                style = OgType.ItemSecondary,
                color = OgColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp),
            )
        }
    }
}

/** Section header row with optional trailing action ("View all"). */
@Composable
fun AlbumsSectionHeader(
    title: String,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {},
) {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = OgType.SectionHeader,
            color = OgColors.TitleBlue,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = OgType.ItemLabel,
                color = OgColors.TextPrimary,
                modifier = Modifier
                    .clickable(onClick = onTrailingClick)
                    .padding(6.dp),
            )
        }
    }
}
