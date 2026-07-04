package foss.opengallery.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/** Top header shown in multi-select mode ("Select items" / "3 selected"). */
@Composable
fun SelectionHeader(
    selectedCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(OgColors.Background)
            .statusBarsPadding()
            .padding(start = 24.dp, top = 18.dp, bottom = 10.dp)
    ) {
        Text(
            text = if (selectedCount == 0) "Select items" else "$selectedCount selected",
            style = OgType.ScreenTitle,
            color = OgColors.TitleBlue,
        )
    }
}

data class SelectionAction(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

/** Bottom action bar replacing the main tabs while selecting. */
@Composable
fun SelectionBottomBar(
    actions: List<SelectionAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(OgColors.Background)
            .navigationBarsPadding()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            val interaction = remember { MutableInteractionSource() }
            Text(
                text = action.label,
                style = OgType.TabLabel,
                color = if (action.enabled) OgColors.TextPrimary else OgColors.TextTertiary,
                modifier = Modifier
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = action.enabled,
                        onClick = action.onClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

/** Round check badge drawn on selected thumbnails. */
@Composable
fun SelectionBadge(selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(26.dp)) {
        Canvas(Modifier.size(26.dp)) {
            if (selected) {
                drawCircle(OgColors.SelectionCheck)
            } else {
                drawCircle(OgColors.ScrimVeil)
                drawCircle(
                    OgColors.TextPrimary,
                    style = Stroke(1.6.dp.toPx()),
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                )
            }
            if (selected) {
                val w = size.width
                val h = size.height
                drawLine(
                    OgColors.TextPrimary,
                    androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.54f),
                    androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.7f),
                    2.2.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    OgColors.TextPrimary,
                    androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.7f),
                    androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.34f),
                    2.2.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        }
    }
}
