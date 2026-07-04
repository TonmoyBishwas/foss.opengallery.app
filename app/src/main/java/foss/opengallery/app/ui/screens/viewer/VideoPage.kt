package foss.opengallery.app.ui.screens.viewer

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import foss.opengallery.app.ui.components.OgIcons.drawPlay
import foss.opengallery.app.ui.theme.OgColors

/**
 * Video page: poster frame with a play button; tapping play swaps in an
 * ExoPlayer surface with the standard controller. The player is torn down
 * whenever the page stops being current so swiping stays cheap.
 */
@Composable
fun VideoPage(
    uri: Uri,
    isCurrentPage: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }

    // Kill playback when swiped away.
    if (!isCurrentPage && playing) playing = false

    Box(modifier.fillMaxSize().clickable { onTap() }, contentAlignment = Alignment.Center) {
        if (playing) {
            val player = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(ExoMediaItem.fromUri(uri))
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(Unit) {
                onDispose { player.release() }
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .size(64.dp)
                    .background(OgColors.ScrimVeil, foss.opengallery.app.ui.theme.OgShapes.Chip)
                    .clickable { playing = true },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(30.dp)) { drawPlay(OgColors.TextPrimary) }
            }
        }
    }
}
