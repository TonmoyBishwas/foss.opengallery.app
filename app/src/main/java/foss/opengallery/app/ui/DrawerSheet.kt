package foss.opengallery.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

/**
 * The ☰ drawer: a bottom sheet with shortcuts
 * (Videos, Favourites, Recent, Suggestions, Locations, Recycle bin, Settings).
 * Entries become functional as their screens land in later milestones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = OgShapes.Sheet,
        containerColor = OgColors.SurfaceSheet,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawerEntry("Videos")
                DrawerEntry("Favourites")
                DrawerEntry("Recent")
                DrawerEntry("Suggestions")
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawerEntry("Locations")
                DrawerEntry("Recycle bin")
                DrawerEntry("Settings")
                DrawerEntry("")
            }
        }
    }
}

@Composable
private fun DrawerEntry(label: String) {
    Text(
        text = label,
        style = OgType.ItemSecondary,
        color = OgColors.TextPrimary,
        modifier = Modifier.padding(6.dp),
    )
}
