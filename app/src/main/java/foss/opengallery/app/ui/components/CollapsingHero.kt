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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.components.OgIcons.drawBack
import foss.opengallery.app.ui.components.OgIcons.drawMore
import foss.opengallery.app.ui.components.OgIcons.drawMultiView
import foss.opengallery.app.ui.components.OgIcons.drawPlus
import foss.opengallery.app.ui.components.OgIcons.drawSearch
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/** Which action glyphs to show in a header action row. */
enum class HeaderAction { MultiView, Search, More, Plus, Back }

/**
 * The expanded hero header (huge pale-blue title with the action icon row
 * at its lower edge) used by the main tab screens. Rendered as the first
 * item of a lazy list/grid; the matching pinned compact bar is
 * [CompactHeaderBar].
 */
@Composable
fun HeroHeader(
    title: String,
    heroHeight: Dp,
    actions: List<HeaderAction>,
    onAction: (HeaderAction) -> Unit,
    modifier: Modifier = Modifier,
    collapseFraction: Float = 0f,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .statusBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = OgType.HeroTitle,
                color = OgColors.TitleBlue,
                modifier = Modifier.alpha(1f - collapseFraction),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                HeaderIconButton(action) { onAction(action) }
            }
        }
    }
}

/** Compact pinned bar shown once the hero title scrolls away. */
@Composable
fun CompactHeaderBar(
    title: String,
    visible: Boolean,
    actions: List<HeaderAction>,
    onAction: (HeaderAction) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    if (!visible) return
    Column(
        modifier
            .fillMaxWidth()
            .background(OgColors.Background.copy(alpha = 0.97f))
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (HeaderAction.Back in actions) {
                HeaderIconButton(HeaderAction.Back) { onAction(HeaderAction.Back) }
            }
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(title, style = OgType.ScreenTitle, color = OgColors.TitleBlue)
                if (subtitle != null) {
                    Text(subtitle, style = OgType.Subtitle, color = OgColors.TitleBlue)
                }
            }
            actions.filter { it != HeaderAction.Back }.forEach { action ->
                HeaderIconButton(action) { onAction(action) }
            }
        }
    }
}

@Composable
fun HeaderIconButton(action: HeaderAction, tint: Color = OgColors.TextPrimary, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        modifier = Modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp)
            .size(26.dp)
    ) {
        val stroke = 2.2.dp.toPx()
        when (action) {
            HeaderAction.MultiView -> drawMultiView(tint, stroke)
            HeaderAction.Search -> drawSearch(tint, stroke)
            HeaderAction.More -> drawMore(tint)
            HeaderAction.Plus -> drawPlus(tint, stroke)
            HeaderAction.Back -> drawBack(tint, stroke)
        }
    }
}
