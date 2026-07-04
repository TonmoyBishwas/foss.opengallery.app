package foss.opengallery.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Edge fast-scroll: a grabbable pill that jumps through the whole list,
 * with a floating label bubble (e.g. "Mar 2026") while dragging.
 * Deliberately thick enough to grab — a direct answer to the "scrollbar too
 * thin" complaint about stock galleries.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.FastScrubber(
    gridState: LazyGridState,
    itemCount: Int,
    labelForIndex: (Int) -> String?,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 0) return
    val scope = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(0f) }
    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var label by remember { mutableStateOf<String?>(null) }

    // Show while the grid scrolls, fade out shortly after it stops.
    LaunchedEffect(gridState.isScrollInProgress, dragging) {
        if (gridState.isScrollInProgress || dragging) {
            visible = true
        } else {
            delay(1400)
            visible = false
        }
    }
    // Track position follows the real scroll when not dragging.
    LaunchedEffect(gridState.firstVisibleItemIndex, itemCount, dragging) {
        if (!dragging && itemCount > 0) {
            fraction = gridState.firstVisibleItemIndex.toFloat() / itemCount
        }
    }

    val density = LocalDensity.current
    val thumbHeight = 48.dp
    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            // Keep the track below the status bar / header area and above the
            // bottom edge so the thumb never rides into system chrome.
            .padding(top = 72.dp, bottom = 24.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(36.dp)
                .onSizeChangedCompat { trackHeightPx = it.toFloat() }
                .pointerInput(itemCount) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            fraction = (offset.y / size.height).coerceIn(0f, 1f)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        fraction = (change.position.y / size.height).coerceIn(0f, 1f)
                        val target = (fraction * (itemCount - 1)).roundToInt()
                        label = labelForIndex(target)
                        scope.launch { gridState.scrollToItem(target) }
                    }
                }
        ) {
            val maxOffset = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
            Box(
                Modifier
                    .offset { IntOffset(0, (fraction * maxOffset).roundToInt()) }
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp)
                    .width(5.dp)
                    .height(thumbHeight)
                    .background(
                        if (dragging) OgColors.AccentBlue else OgColors.TextTertiary,
                        RoundedCornerShape(3.dp),
                    )
            )
            if (dragging && label != null) {
                Box(
                    Modifier
                        .offset { IntOffset(0, (fraction * maxOffset).roundToInt()) }
                        .align(Alignment.TopEnd)
                        .padding(end = 26.dp)
                        .background(OgColors.SurfacePopup, RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = label!!,
                        style = OgType.ItemLabel,
                        color = OgColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun Modifier.onSizeChangedCompat(block: (Int) -> Unit): Modifier =
    onSizeChanged { block(it.height) }
