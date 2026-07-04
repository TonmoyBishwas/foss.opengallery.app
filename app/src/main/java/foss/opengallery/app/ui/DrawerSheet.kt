package foss.opengallery.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.components.OgIcons.drawClock
import foss.opengallery.app.ui.components.OgIcons.drawGear
import foss.opengallery.app.ui.components.OgIcons.drawHeart
import foss.opengallery.app.ui.components.OgIcons.drawLock
import foss.opengallery.app.ui.components.OgIcons.drawPin
import foss.opengallery.app.ui.components.OgIcons.drawSparkle
import foss.opengallery.app.ui.components.OgIcons.drawTrash
import foss.opengallery.app.ui.components.OgIcons.drawVideos
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

private data class DrawerItem(
    val label: String,
    val route: String?,
    val icon: DrawScope.(Color, Float) -> Unit,
)

/**
 * The ☰ drawer: a bottom sheet of icon shortcuts. Entries whose screens are
 * not built yet carry a null route and render dimmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val rows = listOf(
        listOf(
            DrawerItem("Videos", Routes.virtualAlbum("videos", "Videos")) { c, w -> drawVideos(c, w) },
            DrawerItem("Favourites", Routes.virtualAlbum("favourites", "Favourites")) { c, w -> drawHeart(c, false, w) },
            DrawerItem("Recent", Routes.virtualAlbum("recent", "Recent")) { c, w -> drawClock(c, w) },
            DrawerItem("Suggestions", "suggestions") { c, _ -> drawSparkle(c) },
        ),
        listOf(
            DrawerItem("Locations", "locations") { c, w -> drawPin(c, w) },
            DrawerItem("Recycle bin", "recycleBin") { c, w -> drawTrash(c, w) },
            DrawerItem("Settings", "settings") { c, w -> drawGear(c, w) },
            DrawerItem("Locked folder", "lockedFolder") { c, w -> drawLock(c, w) },
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = OgShapes.Sheet,
        containerColor = OgColors.SurfaceSheet,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            rows.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { item ->
                        val interaction = remember { MutableInteractionSource() }
                        val tint = if (item.route != null) OgColors.TextPrimary else OgColors.TextTertiary
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    enabled = item.route != null,
                                ) { item.route?.let(onNavigate) }
                                .padding(vertical = 6.dp),
                        ) {
                            Canvas(Modifier.size(26.dp)) {
                                item.icon(this, tint, 1.8.dp.toPx())
                            }
                            Text(
                                text = item.label,
                                style = OgType.ItemSecondary,
                                textAlign = TextAlign.Center,
                                color = tint,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
