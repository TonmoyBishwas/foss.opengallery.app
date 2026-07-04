package foss.opengallery.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

data class PopupEntry(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The floating rounded context menu used everywhere in the reference design
 * (Edit / Select all / Create / Start slideshow, Sort, Hide albums, ...).
 */
@Composable
fun OneUiPopupMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    entries: List<PopupEntry>,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        shape = OgShapes.Popup,
        containerColor = OgColors.SurfacePopup,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.widthIn(min = 220.dp)) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    style = OgType.MenuEntry,
                    color = if (entry.enabled) OgColors.TextPrimary else OgColors.TextTertiary,
                    modifier = Modifier
                        .clickable(enabled = entry.enabled) {
                            onDismiss()
                            entry.onClick()
                        }
                        .padding(horizontal = 26.dp, vertical = 16.dp)
                        .background(Color.Transparent),
                )
            }
        }
    }
}
