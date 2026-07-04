package foss.opengallery.app.ui.screens.editor

import android.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

/** Tone adjustment keys (each -100..100, 0 = neutral). */
enum class ToneKey(val label: String) {
    Brightness("Brightness"),
    Exposure("Exposure"),
    Contrast("Contrast"),
    Saturation("Saturation"),
    Warmth("Warmth"),
    Tint("Tint"),
    Highlights("Highlights"),
    Shadows("Shadows"),
    Sharpness("Sharpness"),
    Vignette("Vignette"),
}

/** A freehand stroke in the Decorate/Draw layer (normalized 0..1 coords). */
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val widthFraction: Float,
    val isHighlighter: Boolean,
)

/** A text overlay (normalized center position). */
data class TextOverlay(
    val text: String,
    val center: Offset,
    val color: Color,
    val scale: Float = 1f,
)

/** A vector sticker overlay (our own shapes, drawn in code). */
data class StickerOverlay(
    val kind: StickerKind,
    val center: Offset,
    val scale: Float = 1f,
)

enum class StickerKind(val label: String) {
    Arrow("Arrow"), Check("Check"), Cross("Cross"),
    CircleOutline("Circle"), SquareOutline("Square"),
    Star("Star"), Heart("Heart"), Burst("Burst"),
}

/**
 * The full, immutable edit session state. Undo/redo is a stack of these.
 * Geometry uses normalized (0..1) coordinates relative to the source
 * bitmap so preview and full-res save share the same math.
 */
data class EditState(
    /** Crop rect in normalized source coordinates. */
    val crop: Rect = Rect(0f, 0f, 1f, 1f),
    /** Quarter turns clockwise (0..3). */
    val rotate90: Int = 0,
    val flipHorizontal: Boolean = false,
    /** Fine straighten angle in degrees (-45..45). */
    val straighten: Float = 0f,
    val tone: Map<ToneKey, Float> = emptyMap(),
    /** Selected filter preset id + 0..100 intensity. */
    val filterId: String? = null,
    val filterIntensity: Float = 100f,
    val strokes: List<DrawStroke> = emptyList(),
    val texts: List<TextOverlay> = emptyList(),
    val stickers: List<StickerOverlay> = emptyList(),
) {
    val isIdentity: Boolean
        get() = crop == Rect(0f, 0f, 1f, 1f) && rotate90 == 0 && !flipHorizontal &&
            straighten == 0f && tone.values.all { it == 0f } && filterId == null &&
            strokes.isEmpty() && texts.isEmpty() && stickers.isEmpty()

    fun tone(key: ToneKey): Float = tone[key] ?: 0f
}

/** Filter presets: original color recipes expressed as tone offsets + fixed matrices. */
data class FilterPreset(
    val id: String,
    val label: String,
    /** Builds the preset's color matrix at full strength. */
    val matrix: () -> ColorMatrix,
)

object Filters {
    val presets: List<FilterPreset> = listOf(
        FilterPreset("mono", "Mono") {
            ColorMatrix().apply { setSaturation(0f) }
        },
        FilterPreset("noir", "Noir") {
            ColorMatrix().apply {
                setSaturation(0f)
                postConcat(contrast(1.25f))
            }
        },
        FilterPreset("sepia", "Sepia") {
            ColorMatrix().apply {
                setSaturation(0f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.10f, 0f, 0f, 0f, 12f,
                            0f, 0.95f, 0f, 0f, 2f,
                            0f, 0f, 0.75f, 0f, -12f,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                )
            }
        },
        FilterPreset("vivid", "Vivid") {
            ColorMatrix().apply {
                setSaturation(1.35f)
                postConcat(contrast(1.12f))
            }
        },
        FilterPreset("cool", "Cool") {
            ColorMatrix(
                floatArrayOf(
                    0.95f, 0f, 0f, 0f, -6f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1.08f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        },
        FilterPreset("warm", "Warm") {
            ColorMatrix(
                floatArrayOf(
                    1.08f, 0f, 0f, 0f, 10f,
                    0f, 1.02f, 0f, 0f, 2f,
                    0f, 0f, 0.92f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        },
        FilterPreset("fade", "Fade") {
            ColorMatrix().apply {
                setSaturation(0.8f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.9f, 0f, 0f, 0f, 24f,
                            0f, 0.9f, 0f, 0f, 24f,
                            0f, 0f, 0.9f, 0f, 24f,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                )
            }
        },
        FilterPreset("forest", "Forest") {
            ColorMatrix(
                floatArrayOf(
                    0.95f, 0f, 0f, 0f, 0f,
                    0f, 1.1f, 0f, 0f, 6f,
                    0f, 0f, 0.95f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        },
        FilterPreset("rose", "Rose") {
            ColorMatrix(
                floatArrayOf(
                    1.06f, 0f, 0f, 0f, 8f,
                    0f, 0.98f, 0f, 0f, 0f,
                    0f, 0f, 1.02f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        },
        FilterPreset("dusk", "Dusk") {
            ColorMatrix().apply {
                setSaturation(0.85f)
                postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.92f, 0f, 0f, 0f, -6f,
                            0f, 0.92f, 0f, 0f, -4f,
                            0f, 0f, 1.05f, 0f, 8f,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                )
            }
        },
    )

    fun byId(id: String?): FilterPreset? = presets.firstOrNull { it.id == id }

    fun contrast(factor: Float): ColorMatrix {
        val translate = (1f - factor) * 127.5f
        return ColorMatrix(
            floatArrayOf(
                factor, 0f, 0f, 0f, translate,
                0f, factor, 0f, 0f, translate,
                0f, 0f, factor, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }
}

/** Builds the combined live-preview color matrix for a state. */
object ToneMatrix {

    fun build(state: EditState): ColorMatrix {
        val m = ColorMatrix()

        val brightness = state.tone(ToneKey.Brightness) * 0.8f
        val exposure = 1f + state.tone(ToneKey.Exposure) / 150f
        val contrastF = 1f + state.tone(ToneKey.Contrast) / 180f
        val saturation = 1f + state.tone(ToneKey.Saturation) / 130f
        val warmth = state.tone(ToneKey.Warmth)
        val tint = state.tone(ToneKey.Tint)
        // Coarse live approximation of highlights/shadows; exact curves are
        // applied per-pixel at save time.
        val highlights = state.tone(ToneKey.Highlights)
        val shadows = state.tone(ToneKey.Shadows)

        // Exposure (multiplicative) + brightness (additive).
        m.postConcat(
            ColorMatrix(
                floatArrayOf(
                    exposure, 0f, 0f, 0f, brightness + shadows * 0.25f,
                    0f, exposure, 0f, 0f, brightness + shadows * 0.25f,
                    0f, 0f, exposure, 0f, brightness + shadows * 0.25f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
        if (highlights != 0f) {
            val f = 1f - highlights / 400f
            m.postConcat(Filters.contrast(f.coerceIn(0.75f, 1.25f)))
        }
        m.postConcat(Filters.contrast(contrastF))
        m.postConcat(ColorMatrix().apply { setSaturation(saturation.coerceAtLeast(0f)) })
        if (warmth != 0f || tint != 0f) {
            m.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f + warmth / 250f, 0f, 0f, 0f, 0f,
                        0f, 1f + tint / 400f, 0f, 0f, 0f,
                        0f, 0f, 1f - warmth / 250f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )
        }

        // Filter preset, faded by intensity.
        Filters.byId(state.filterId)?.let { preset ->
            m.postConcat(lerpToIdentity(preset.matrix(), state.filterIntensity / 100f))
        }
        return m
    }

    /** Linear blend of a matrix toward identity by [t] (0 = identity). */
    private fun lerpToIdentity(matrix: ColorMatrix, t: Float): ColorMatrix {
        val identity = ColorMatrix().array
        val src = matrix.array
        val out = FloatArray(20)
        for (i in 0 until 20) {
            out[i] = identity[i] + (src[i] - identity[i]) * t.coerceIn(0f, 1f)
        }
        return ColorMatrix(out)
    }
}
