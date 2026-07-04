package foss.opengallery.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

private data class DrawerItem(val label: String, val route: String?)

/**
 * The ☰ drawer: a bottom sheet of shortcuts. Entries whose screens are not
 * built yet carry a null route and render dimmed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val rows = listOf(
        listOf(
            DrawerItem("Videos", Routes.virtualAlbum("videos", "Videos")),
            DrawerItem("Favourites", Routes.virtualAlbum("favourites", "Favourites")),
            DrawerItem("Recent", Routes.virtualAlbum("recent", "Recent")),
            DrawerItem("Suggestions", null),
        ),
        listOf(
            DrawerItem("Locations", null),
            DrawerItem("Recycle bin", "recycleBin"),
            DrawerItem("Settings", null),
            DrawerItem("Locked folder", "lockedFolder"),
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
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { item ->
                        val interaction = remember { MutableInteractionSource() }
                        Text(
                            text = item.label,
                            style = OgType.ItemSecondary,
                            textAlign = TextAlign.Center,
                            color = if (item.route != null) OgColors.TextPrimary
                            else OgColors.TextTertiary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    enabled = item.route != null,
                                ) { item.route?.let(onNavigate) }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}
