package foss.opengallery.app.ui.screens.editor

import android.app.Application
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import foss.opengallery.app.util.SaveEdited
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Save progress for the editor UI. */
enum class SaveStatus { Idle, Saving, Done, Failed }

class EditorViewModel(
    application: Application,
    private val mediaId: Long,
) : AndroidViewModel(application) {

    private val resolver = application.contentResolver

    val sourceUri: Uri = ContentUris.withAppendedId(
        if (android.os.Build.VERSION.SDK_INT >= 29)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            @Suppress("DEPRECATION") MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        mediaId,
    )

    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<EditState>()
    private val redoStack = ArrayDeque<EditState>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private var sourceName: String = "image"

    init {
        viewModelScope.launch {
            _preview.value = loadPreviewBitmap()
        }
    }

    /** Preview at bounded resolution, orientation baked in. */
    private suspend fun loadPreviewBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            resolver.query(
                sourceUri,
                arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null,
            )?.use { c -> if (c.moveToFirst()) sourceName = c.getString(0) ?: "image" }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_PREVIEW &&
                bounds.outHeight / (sample * 2) >= MAX_PREVIEW
            ) sample *= 2

            val decoded = resolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(
                    it, null, BitmapFactory.Options().apply { inSampleSize = sample }
                )
            } ?: return@runCatching null
            applyExifOrientation(decoded)
        }.getOrNull()
    }

    private fun applyExifOrientation(bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            resolver.openInputStream(sourceUri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postRotate(180f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(-90f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private var gestureBase: EditState? = null

    /** Applies a state mutation, recording undo history. */
    fun update(transform: (EditState) -> EditState) {
        gestureBase = null
        undoStack.addLast(_state.value)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        _state.value = transform(_state.value)
        refreshHistoryFlags()
    }

    /**
     * Mid-gesture mutation (slider drag, straighten ruler): applies without
     * recording history so one gesture doesn't flood the undo stack.
     */
    fun updateLive(transform: (EditState) -> EditState) {
        if (gestureBase == null) gestureBase = _state.value
        _state.value = transform(_state.value)
    }

    /** Commits the gesture started by [updateLive] as a single undo entry. */
    fun commitGesture() {
        val base = gestureBase ?: return
        gestureBase = null
        if (base == _state.value) return
        undoStack.addLast(base)
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
        refreshHistoryFlags()
    }

    fun undo() {
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value)
        _state.value = prev
        refreshHistoryFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value)
        _state.value = next
        refreshHistoryFlags()
    }

    fun revert() {
        undoStack.addLast(_state.value)
        redoStack.clear()
        _state.value = EditState()
        refreshHistoryFlags()
    }

    private fun refreshHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    /** Renders full-res and saves a copy (never overwrites the original). */
    fun save() {
        if (_saveStatus.value == SaveStatus.Saving) return
        _saveStatus.value = SaveStatus.Saving
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // Bounded decode: a 108 MP source at ARGB_8888 is ~430 MB,
                    // and the render pipeline keeps 2–3 geometry copies alive
                    // at once. Budget from the actual heap instead of a fixed
                    // edge so big-heap devices keep more resolution (a 512 MB
                    // heap preserves ~20 MP; the old fixed 4096 cap forced
                    // every 50 MP photo down to ~12 MP).
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(sourceUri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    val budgetBytes = (Runtime.getRuntime().maxMemory() / 6)
                        .coerceAtMost(256L * 1024 * 1024)
                    var sample = 1
                    while (
                        (bounds.outWidth.toLong() / sample) *
                        (bounds.outHeight.toLong() / sample) * 4L > budgetBytes
                    ) {
                        sample *= 2
                    }
                    val full = resolver.openInputStream(sourceUri)?.use {
                        BitmapFactory.decodeStream(
                            it, null, BitmapFactory.Options().apply { inSampleSize = sample }
                        )
                    } ?: return@runCatching null
                    val oriented = applyExifOrientation(full)
                    val rendered = SaveEdited.render(oriented, _state.value)
                    SaveEdited.saveAsCopy(
                        getApplication(), sourceUri, sourceName, rendered,
                    )
                }.getOrNull()
            }
            _saveStatus.value = if (result != null) SaveStatus.Done else SaveStatus.Failed
        }
    }

    private companion object {
        const val MAX_PREVIEW = 2048
        const val MAX_HISTORY = 40
    }
}
