package foss.opengallery.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import foss.opengallery.app.R
import androidx.compose.foundation.Canvas as FoundationCanvas
import androidx.compose.ui.res.stringResource
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

enum class MainTab { Pictures, Albums, Stories }

/**
 * Text-only bottom tab bar with the underlined active label and the
 * trailing hamburger menu button, in the reference design's style.
 */
@Composable
fun OneUiBottomTabs(
    current: MainTab,
    onSelect: (MainTab) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(OgColors.Background)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabLabel(stringResource(R.string.tab_pictures), current == MainTab.Pictures) {
            onSelect(MainTab.Pictures)
        }
        TabLabel(stringResource(R.string.tab_albums), current == MainTab.Albums) {
            onSelect(MainTab.Albums)
        }
        TabLabel(stringResource(R.string.tab_stories), current == MainTab.Stories) {
            onSelect(MainTab.Stories)
        }
        HamburgerButton(onMenuClick)
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Text(
            text = text,
            style = OgType.TabLabel,
            color = if (selected) OgColors.TitleBlue else OgColors.TextSecondary,
        )
        Box(
            Modifier
                .padding(top = 5.dp)
                .height(2.dp)
                .width(34.dp)
                .background(if (selected) OgColors.TitleBlue else Color.Transparent)
        )
    }
}

@Composable
private fun HamburgerButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    FoundationCanvas(
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(14.dp)
            .size(24.dp)
    ) {
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        listOf(0.26f, 0.5f, 0.74f).forEach { fy ->
            drawLine(
                color = OgColors.TextPrimary,
                start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * fy),
                end = androidx.compose.ui.geometry.Offset(w * 0.88f, h * fy),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
    }
}
