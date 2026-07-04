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
            EditorSection.Transform -> TransformControls(state, vm::update)
            EditorSection.Filters -> FilterControls(preview, state, vm::update)
            EditorSection.Tone -> ToneControls(state, vm::update)
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

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(
                if (section == EditorSection.Decorate) {
                    Modifier.pointerInput(drawColor) {
                        detectDragGestures(
                            onDragStart = { start ->
                                activeStroke = listOf(
                                    Offset(start.x / size.width, start.y / size.height)
                                )
                            },
                            onDrag = { change, _ ->
                                activeStroke = activeStroke?.plus(
                                    Offset(
                                        change.position.x / size.width,
                                        change.position.y / size.height,
                                    )
                                )
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
                    scaleX = if (state.flipHorizontal) -1f else 1f
                },
        )
        // Overlay strokes/texts/stickers preview.
        Canvas(Modifier.fillMaxSize()) {
            val native = drawContext.canvas.nativeCanvas
            foss.opengallery.app.util.SaveEdited.drawOverlays(
                native, size.width, size.height, state,
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
                        strokeWidth = 0.012f * size.width
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    val path = android.graphics.Path()
                    path.moveTo(points.first().x * size.width, points.first().y * size.height)
                    points.drop(1).forEach {
                        path.lineTo(it.x * size.width, it.y * size.height)
                    }
                    native.drawPath(path, paint)
                }
            }
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
private fun TransformControls(state: EditState, onUpdate: ((EditState) -> EditState) -> Unit) {
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
                                    centeredCrop(aspect)
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
                onUpdate { it.copy(straighten = (it.straighten + delta).coerceIn(-45f, 45f)) }
            },
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
private fun StraightenRuler(value: Float, onDragDegrees: (Float) -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pxPerDeg = with(density) { 6.dp.toPx() }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 24.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
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
) {
    Column(Modifier.fillMaxWidth()) {
        if (state.filterId != null) {
            Slider(
                value = state.filterIntensity,
                onValueChange = { v -> onUpdate { it.copy(filterIntensity = v) } },
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
            FilterSwatch("Original", preview, null, state.filterId == null) {
                onUpdate { it.copy(filterId = null) }
            }
            Filters.presets.forEach { preset ->
                FilterSwatch(preset.label, preview, preset, state.filterId == preset.id) {
                    onUpdate { it.copy(filterId = preset.id, filterIntensity = 100f) }
                }
            }
        }
    }
}

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
private fun ToneControls(state: EditState, onUpdate: ((EditState) -> EditState) -> Unit) {
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
                onUpdate { it.copy(tone = it.tone + (selected to v)) }
            },
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

/** Center crop of a given aspect within the unit square. */
private fun centeredCrop(aspect: Float): androidx.compose.ui.geometry.Rect {
    // The unit square maps to the image; approximate using square source.
    return if (aspect >= 1f) {
        val h = 1f / aspect
        androidx.compose.ui.geometry.Rect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    } else {
        val w = aspect
        androidx.compose.ui.geometry.Rect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
    }
}

@Composable
private fun editorSliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFFF7CE46),
    activeTrackColor = Color(0xFFF7CE46),
    inactiveTrackColor = OgColors.SurfaceChip,
)
