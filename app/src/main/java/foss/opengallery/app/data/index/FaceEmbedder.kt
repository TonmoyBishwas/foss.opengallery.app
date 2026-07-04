package foss.opengallery.app.data.index

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * MobileFaceNet embeddings: 112x112 face crop -> 192-dim L2-normalized
 * vector. Same-person faces land close in cosine similarity. Runs fully
 * on-device via the bundled TFLite model (see THIRD_PARTY_NOTICES.md).
 */
class FaceEmbedder(context: Context) {

    private val interpreter: Interpreter? = runCatching {
        Interpreter(loadModel(context), Interpreter.Options().apply { numThreads = 2 })
    }.getOrNull()

    val available: Boolean get() = interpreter != null

    private fun loadModel(context: Context): MappedByteBuffer {
        context.assets.openFd(MODEL_ASSET).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).use { stream ->
                return stream.channel.map(
                    FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
                )
            }
        }
    }

    /** Embeds a face crop; returns L2-normalized vector or null. */
    fun embed(face: Bitmap): FloatArray? {
        val model = interpreter ?: return null
        val scaled = Bitmap.createScaledBitmap(face, INPUT_SIZE, INPUT_SIZE, true)
        val input = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (p in pixels) {
            input.putFloat(((p shr 16 and 0xFF) - 127.5f) / 128f)
            input.putFloat(((p shr 8 and 0xFF) - 127.5f) / 128f)
            input.putFloat(((p and 0xFF) - 127.5f) / 128f)
        }
        input.rewind()
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        return runCatching {
            model.run(input, output)
            normalize(output[0])
        }.getOrNull()
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum)
        if (norm == 0f) return v
        for (i in v.indices) v[i] /= norm
        return v
    }

    companion object {
        const val MODEL_ASSET = "mobile_face_net.tflite"
        const val INPUT_SIZE = 112
        const val EMBEDDING_SIZE = 192

        fun cosine(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot // both are L2-normalized
        }

        fun toBytes(v: FloatArray): ByteArray {
            val buf = ByteBuffer.allocate(4 * v.size).order(ByteOrder.LITTLE_ENDIAN)
            v.forEach { buf.putFloat(it) }
            return buf.array()
        }

        fun fromBytes(bytes: ByteArray): FloatArray {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            return FloatArray(bytes.size / 4) { buf.float }
        }
    }
}
