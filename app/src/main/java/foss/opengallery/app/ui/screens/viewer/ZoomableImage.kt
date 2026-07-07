package foss.opengallery.app.ui.screens.viewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage

/**
 * Pinch/double-tap zoomable image page. Reports whether it is zoomed so the
 * parent pager can disable page swiping while panning.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // If this page is removed while zoomed (delete-while-zoomed), the parent
    // never hears the zoom end and page swiping stays locked — release it.
    DisposableEffect(Unit) {
        onDispose { onZoomChanged(false) }
    }

    fun clampOffset(raw: Offset, s: Float): Offset {
        if (s <= 1f) return Offset.Zero
        val maxX = (containerSize.width * (s - 1f)) / 2f
        val maxY = (containerSize.height * (s - 1f)) / 2f
        return Offset(
            raw.x.coerceIn(-maxX, maxX),
            raw.y.coerceIn(-maxY, maxY),
        )
    }

    fun setScale(newScale: Float, pivotDelta: Offset = Offset.Zero) {
        val s = newScale.coerceIn(1f, MAX_SCALE)
        scale = s
        offset = clampOffset(offset + pivotDelta, s)
        onZoomChanged(s > 1.02f)
        if (s <= 1.02f) offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapPos ->
                        if (scale > 1.02f) {
                            setScale(1f)
                        } else {
                            // Zoom toward the tap point.
                            val target = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            offset = clampOffset((center - tapPos) * (target - 1f), target)
                            scale = target
                            onZoomChanged(true)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (event.changes.size > 1 || scale > 1.02f) {
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val newScale = (scale * zoomChange).coerceIn(1f, MAX_SCALE)
                                scale = newScale
                                offset = clampOffset(offset + panChange, newScale)
                                onZoomChanged(newScale > 1.02f)
                                if (newScale <= 1.02f) offset = Offset.Zero
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private const val MAX_SCALE = 8f
