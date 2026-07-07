package foss.opengallery.app.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.components.OgIcons.drawCrop
import foss.opengallery.app.ui.components.OgIcons.drawDecorate
import foss.opengallery.app.ui.components.OgIcons.drawFilterTriad
import foss.opengallery.app.ui.components.OgIcons.drawFlip
import foss.opengallery.app.ui.components.OgIcons.drawRotate
import foss.opengallery.app.ui.components.OgIcons.drawTone
import foss.opengallery.app.ui.components.OgIcons.drawUndoArrow
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val EditorAccent = Color(0xFFF7CE46)
private val EditorChip = Color(0xFF26262B)

/** Editor sections in the bottom navigation, mirroring the reference design. */
enum class EditorSection(val label: String) {
    Transform("Transform"), Filters("Filters"), Tone("Tone"), Decorate("Decorate"),
}

/**
 * The photo editor. Live preview is driven entirely by a ColorMatrix on
 * the GPU-composited Image plus overlay drawing — no heavyweight
 * dependencies. Saving renders at full resolution and always writes a
 * copy with EXIF preserved.
 */
@Composable
fun EditorScreen(
    mediaId: Long,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel(key = "editor:$mediaId") { _ ->
        EditorViewModel(context.applicationContext as android.app.Application, mediaId)
    }
    val preview by vm.preview.collectAsState()
    val state by vm.state.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val canRedo by vm.canRedo.collectAsState()
    val saveStatus by vm.saveStatus.collectAsState()
    var section by rememberSaveable { mutableStateOf(EditorSection.Transform) }

    LaunchedEffect(saveStatus) {
        if (saveStatus == SaveStatus.Done) onClose()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar: circular undo/redo — Revert / Save, as in the reference.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UndoRedoButton(redo = false, enabled = canUndo) { vm.undo() }
            Spacer(Modifier.width(10.dp))
            UndoRedoButton(redo = true, enabled = canRedo) { vm.redo() }
            Box(Modifier.weight(1f))
            EditorTextButton("Revert", enabled = !state.isIdentity) { vm.revert() }
            EditorTextButton(
                if (saveStatus == SaveStatus.Saving) "Saving…" else "Save",
                enabled = !state.isIdentity && saveStatus != SaveStatus.Saving,
            ) { vm.save() }
        }

        // Preview area.
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let { bitmap ->
                EditorPreview(
                    bitmap = bitmap,
                    state = state,
                    section = section,
                    onUpdate = vm::update,
                )
            }
        }

        // Section-specific controls.
        when (section) {
            EditorSection.Transform -> TransformControls(
                state = state,
                baseAspect = preview?.let { it.width.toFloat() / it.height } ?: 1f,
                onUpdate = vm::update,
                onUpdateLive = vm::updateLive,
                onCommit = vm::commitGesture,
            )
            EditorSection.Filters -> FilterControls(
                preview, state, vm::update, vm::updateLive, vm::commitGesture,
            )
            EditorSection.Tone -> ToneControls(state, vm::updateLive, vm::commitGesture)
            EditorSection.Decorate -> DecorateControls(state, vm::update)
        }

        // Bottom section switcher: icons, active one in the editor accent.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorSection.entries.forEach { s ->
                val interaction = remember { MutableInteractionSource() }
                val tint = if (s == section) EditorAccent else OgColors.TextSecondary
                Canvas(
                    Modifier
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                        ) { section = s }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .size(26.dp)
                ) {
                    val w = 2.dp.toPx()
                    when (s) {
                        EditorSection.Transform -> drawCrop(tint, w)
                        EditorSection.Filters -> drawFilterTriad(tint, w)
                        EditorSection.Tone -> drawTone(tint, w)
                        EditorSection.Decorate -> drawDecorate(tint, w)
                    }
                }
            }
        }
    }
}

@Composable
private fun UndoRedoButton(redo: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(38.dp)
            .background(EditorChip, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            drawUndoArrow(
                if (enabled) OgColors.TextPrimary else OgColors.TextTertiary,
                2.dp.toPx(),
                mirrored = redo,
            )
        }
    }
}

@Composable
private fun EditorTextButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = OgType.ItemLabel,
        color = if (enabled) OgColors.TextPrimary else OgColors.TextTertiary,
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

/** The live preview: color matrix + straighten/flip + overlays + draw capture. */
@Composable
private fun EditorPreview(
    bitmap: android.graphics.Bitmap,
    state: EditState,
    section: EditorSection,
    onUpdate: ((EditState) -> EditState) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var activeStroke by remember { mutableStateOf<List<Offset>?>(null) }
    var drawColor by remember { mutableStateOf(Color(0xFFF7CE46)) }

    val androidMatrix = remember(state) { ToneMatrix.build(state) }
    val composeFilter = remember(androidMatrix) {
        ColorFilter.colorMatrix(ComposeColorMatrix(androidMatrix.array.copyOf()))
    }
    // Where the fitted image actually sits inside the container. Overlay
    // coords are normalized against this rect, not the whole container, so
    // strokes land in the same spot in the saved file (no letterbox drift).
    val fitRect = remember(canvasSize, bitmap, state.rotate90) {
        fittedImageRect(canvasSize, bitmap.width, bitmap.height, state.rotate90)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(
                if (section == EditorSection.Decorate) {
                    Modifier.pointerInput(drawColor, fitRect) {
                        val r = fitRect ?: androidx.compose.ui.geometry.Rect(
                            0f, 0f, size.width.toFloat(), size.height.toFloat()
                        )
                        fun norm(p: Offset) = Offset(
                            ((p.x - r.left) / r.width).coerceIn(0f, 1f),
                            ((p.y - r.top) / r.height).coerceIn(0f, 1f),
                        )
                        detectDragGestures(
                            onDragStart = { start ->
                                activeStroke = listOf(norm(start))
                            },
                            onDrag = { change, _ ->
                                activeStroke = activeStroke?.plus(norm(change.position))
                            },
                            onDragEnd = {
                                val points = activeStroke
                                activeStroke = null
                                if (points != null && points.size > 1) {
                                    onUpdate { s ->
                                        s.copy(
                                            strokes = s.strokes + DrawStroke(
                                                points = points,
                                                color = drawColor,
                                                widthFraction = 0.012f,
                                                isHighlighter = false,
                                            )
                                        )
                                    }
                                }
                            },
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = composeFilter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = state.straighten + 90f * state.rotate90
                    // Same zoom the save applies, so the wedge crop is visible
                    // live; plus a re-fit when a 90° turn swaps the aspect.
                    var s = straightenScale(
                        bitmap.width.toFloat(), bitmap.height.toFloat(), state.straighten
                    )
                    if (state.rotate90 % 2 == 1 && canvasSize != IntSize.Zero) {
                        val cw = canvasSize.width.toFloat()
                        val ch = canvasSize.height.toFloat()
                        val bw = bitmap.width.toFloat()
                        val bh = bitmap.height.toFloat()
                        val base = minOf(cw / bw, ch / bh)
                        if (base > 0f) s *= minOf(cw / bh, ch / bw) / base
                    }
                    scaleX = (if (state.flipHorizontal) -1f else 1f) * s
                    scaleY = s
                },
        )
        // Overlay strokes/texts/stickers preview, drawn over the fitted rect.
        Canvas(Modifier.fillMaxSize()) {
            val r = fitRect ?: androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)
            val native = drawContext.canvas.nativeCanvas
            val checkpoint = native.save()
            native.translate(r.left, r.top)
            native.clipRect(0f, 0f, r.width, r.height)
            foss.opengallery.app.util.SaveEdited.drawOverlays(
                native, r.width, r.height, state,
            )
            // In-progress stroke.
            activeStroke?.let { points ->
                if (points.size > 1) {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(
                            255,
                            (drawColor.red * 255).toInt(),
                            (drawColor.green * 255).toInt(),
                            (drawColor.blue * 255).toInt(),
                        )
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 0.012f * r.width
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    val path = android.graphics.Path()
                    path.moveTo(points.first().x * r.width, points.first().y * r.height)
                    points.drop(1).forEach {
                        path.lineTo(it.x * r.width, it.y * r.height)
                    }
                    native.drawPath(path, paint)
                }
            }
            native.restoreToCount(checkpoint)
        }
        // Vignette live preview.
        val vignette = state.tone(ToneKey.Vignette)
        if (vignette > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(vignette / 100f * 0.8f)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black,
                        )
                    )
            )
        }
    }
}

/** ContentScale.Fit rect of the displayed image inside the container. */
private fun fittedImageRect(
    container: IntSize,
    bitmapW: Int,
    bitmapH: Int,
    rotate90: Int,
): androidx.compose.ui.geometry.Rect? {
    if (container == IntSize.Zero || bitmapW <= 0 || bitmapH <= 0) return null
    val cw = container.width.toFloat()
    val ch = container.height.toFloat()
    val (ew, eh) = if (rotate90 % 2 == 1) {
        bitmapH.toFloat() to bitmapW.toFloat()
    } else {
        bitmapW.toFloat() to bitmapH.toFloat()
    }
    val s = minOf(cw / ew, ch / eh)
    val w = ew * s
    val h = eh * s
    return androidx.compose.ui.geometry.Rect(
        (cw - w) / 2f, (ch - h) / 2f, (cw + w) / 2f, (ch + h) / 2f
    )
}

/** Aspect presets cycled by the pill button, like the reference "Free" chip. */
private val AspectPresets = listOf(
    "Free" to null,
    "1:1" to 1f,
    "4:3" to 4f / 3f,
    "3:4" to 3f / 4f,
    "16:9" to 16f / 9f,
)

/** Transform: flip / rotate / aspect pill + the straighten ruler dial. */
@Composable
private fun TransformControls(
    state: EditState,
    baseAspect: Float,
    onUpdate: ((EditState) -> EditState) -> Unit,
    onUpdateLive: ((EditState) -> EditState) -> Unit,
    onCommit: () -> Unit,
) {
    var aspectIndex by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxWidth()) {
        // Centered pill: flip · rotate · aspect.
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .background(EditorChip, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillIconButton({ c, w -> drawFlip(c, w) }) {
                onUpdate { it.copy(flipHorizontal = !it.flipHorizontal) }
            }
            PillIconButton({ c, w -> drawRotate(c, w) }) {
                onUpdate { it.copy(rotate90 = (it.rotate90 + 1) % 4) }
            }
            Text(
                text = AspectPresets[aspectIndex].first,
                style = OgType.ItemSecondary,
                color = OgColors.TextPrimary,
                modifier = Modifier
                    .clickable {
                        aspectIndex = (aspectIndex + 1) % AspectPresets.size
                        val aspect = AspectPresets[aspectIndex].second
                        onUpdate {
                            it.copy(
                                crop = if (aspect == null) {
                                    androidx.compose.ui.geometry.Rect(0f, 0f, 1f, 1f)
                                } else {
                                    // Crop coords apply after rotate90, where
                                    // the pixel aspect may be swapped.
                                    val src = if (it.rotate90 % 2 == 1) {
                                        1f / baseAspect
                                    } else {
                                        baseAspect
                                    }
                                    centeredCrop(aspect, src)
                                }
                            )
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Text(
            text = if (state.straighten.roundToInt() != 0) {
                "Straighten  ${state.straighten.roundToInt()}°"
            } else {
                "Straighten"
            },
            style = OgType.ItemSecondary,
            color = OgColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp),
        )
        StraightenRuler(
            value = state.straighten,
            onDragDegrees = { delta ->
                onUpdateLive { it.copy(straighten = (it.straighten + delta).coerceIn(-45f, 45f)) }
            },
            onDragFinished = onCommit,
        )
    }
}

@Composable
private fun PillIconButton(
    draw: DrawScope.(Color, Float) -> Unit,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Canvas(
        Modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .size(22.dp)
    ) {
        draw(OgColors.TextPrimary, 1.8.dp.toPx())
    }
}

/**
 * The tick-mark straighten dial: the ruler slides under a fixed center
 * indicator as you drag, exactly like the reference editor.
 */
@Composable
private fun StraightenRuler(
    value: Float,
    onDragDegrees: (Float) -> Unit,
    onDragFinished: () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pxPerDeg = with(density) { 6.dp.toPx() }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onDragFinished,
                    onDragCancel = onDragFinished,
                ) { change, dragAmount ->
                    change.consume()
                    // Ruler follows the finger: dragging right lowers the angle.
                    onDragDegrees(-dragAmount / pxPerDeg)
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        var deg = -45
        while (deg <= 45) {
            val x = cx + (deg - value) * pxPerDeg
            if (x in 0f..size.width) {
                val tall = deg % 15 == 0
                val h = if (tall) 9.dp.toPx() else 5.dp.toPx()
                drawLine(
                    if (tall) OgColors.TextSecondary else OgColors.TextTertiary,
                    Offset(x, cy - h),
                    Offset(x, cy + h),
                    1.5.dp.toPx(),
                    StrokeCap.Round,
                )
            }
            deg += 3
        }
        drawLine(
            EditorAccent,
            Offset(cx, cy - 13.dp.toPx()),
            Offset(cx, cy + 13.dp.toPx()),
            2.5.dp.toPx(),
            StrokeCap.Round,
        )
    }
}

/** Filters: preset strip with live-preview swatches + intensity slider. */
@Composable
private fun FilterControls(
    preview: android.graphics.Bitmap?,
    state: EditState,
    onUpdate: ((EditState) -> EditState) -> Unit,
    onUpdateLive: ((EditState) -> EditState) -> Unit,
    onCommit: () -> Unit,
) {
    // Swatches don't need the 2048px preview; a thumbnail redraws 11× cheaper.
    // Scaled off the main thread — a synchronous createScaledBitmap during
    // composition dropped frames every time the Filters section was opened.
    val swatchBitmap by produceState<android.graphics.Bitmap?>(null, preview) {
        value = preview?.let {
            val minSide = minOf(it.width, it.height)
            if (minSide <= SWATCH_PX) it
            else withContext(Dispatchers.Default) {
                val s = SWATCH_PX.toFloat() / minSide
                android.graphics.Bitmap.createScaledBitmap(
                    it,
                    (it.width * s).roundToInt().coerceAtLeast(1),
                    (it.height * s).roundToInt().coerceAtLeast(1),
                    true,
                )
            }
        }
    }
    Column(Modifier.fillMaxWidth()) {
        if (state.filterId != null) {
            Slider(
                value = state.filterIntensity,
                onValueChange = { v -> onUpdateLive { it.copy(filterIntensity = v) } },
                onValueChangeFinished = onCommit,
                valueRange = 0f..100f,
                colors = editorSliderColors(),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            FilterSwatch("Original", swatchBitmap, null, state.filterId == null) {
                onUpdate { it.copy(filterId = null) }
            }
            Filters.presets.forEach { preset ->
                FilterSwatch(preset.label, swatchBitmap, preset, state.filterId == preset.id) {
                    onUpdate { it.copy(filterId = preset.id, filterIntensity = 100f) }
                }
            }
        }
    }
}

private const val SWATCH_PX = 128

@Composable
private fun FilterSwatch(
    label: String,
    preview: android.graphics.Bitmap?,
    preset: FilterPreset?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(5.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .background(OgColors.SurfaceChip),
        ) {
            preview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    colorFilter = preset?.let { p ->
                        ColorFilter.colorMatrix(ComposeColorMatrix(p.matrix().array.copyOf()))
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            label,
            style = OgType.ItemSecondary,
            color = if (selected) Color(0xFFF7CE46) else OgColors.TextSecondary,
        )
    }
}

/** Tone: the adjustment dial row + slider. */
@Composable
private fun ToneControls(
    state: EditState,
    onUpdateLive: ((EditState) -> EditState) -> Unit,
    onCommit: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(ToneKey.Brightness) }
    Column(Modifier.fillMaxWidth()) {
        Text(
            selected.label,
            style = OgType.ItemSecondary,
            color = OgColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Slider(
            value = state.tone(selected),
            onValueChange = { v ->
                onUpdateLive { it.copy(tone = it.tone + (selected to v)) }
            },
            onValueChangeFinished = onCommit,
            valueRange = if (selected == ToneKey.Vignette) 0f..100f else -100f..100f,
            colors = editorSliderColors(),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            ToneKey.entries.forEach { key ->
                val active = state.tone(key) != 0f
                Text(
                    text = key.label,
                    style = OgType.ItemSecondary,
                    color = when {
                        key == selected -> Color(0xFFF7CE46)
                        active -> OgColors.TextPrimary
                        else -> OgColors.TextSecondary
                    },
                    modifier = Modifier
                        .clickable { selected = key }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Decorate: draw color choices, sticker & text insertion. */
@Composable
private fun DecorateControls(state: EditState, onUpdate: ((EditState) -> EditState) -> Unit) {
    var textInput by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            "Draw directly on the photo. Add stickers and text below.",
            style = OgType.ItemSecondary,
            color = OgColors.TextSecondary,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StickerKind.entries.forEach { kind ->
                EditorTextButton(kind.label) {
                    onUpdate {
                        it.copy(
                            stickers = it.stickers + StickerOverlay(
                                kind = kind,
                                center = Offset(0.5f, 0.5f),
                            )
                        )
                    }
                }
            }
            EditorTextButton("Clear") {
                onUpdate { it.copy(strokes = emptyList(), stickers = emptyList(), texts = emptyList()) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                singleLine = true,
                placeholder = { Text("Text overlay") },
                modifier = Modifier.weight(1f),
            )
            EditorTextButton("Add", enabled = textInput.isNotBlank()) {
                onUpdate {
                    it.copy(
                        texts = it.texts + TextOverlay(
                            text = textInput,
                            center = Offset(0.5f, 0.85f),
                            color = Color.White,
                        )
                    )
                }
                textInput = ""
            }
        }
    }
}

/**
 * Center crop in normalized source coords whose PIXEL aspect is [aspect],
 * given the source's own pixel aspect [srcAspect] (both w/h).
 */
private fun centeredCrop(aspect: Float, srcAspect: Float): androidx.compose.ui.geometry.Rect {
    val r = aspect / srcAspect
    return if (r >= 1f) {
        val h = 1f / r
        androidx.compose.ui.geometry.Rect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    } else {
        androidx.compose.ui.geometry.Rect((1f - r) / 2f, 0f, (1f + r) / 2f, 1f)
    }
}

@Composable
private fun editorSliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFFF7CE46),
    activeTrackColor = Color(0xFFF7CE46),
    inactiveTrackColor = OgColors.SurfaceChip,
)
