package foss.opengallery.app.data.index

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import foss.opengallery.app.OpenGalleryApp
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.db.FaceEntity
import foss.opengallery.app.data.db.MediaIndexEntity
import foss.opengallery.app.data.db.PersonEntity
import foss.opengallery.app.data.model.MediaItem
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Background indexing: OCR + labels + face embeddings for new photos,
 * then an incremental people-clustering pass. Everything on-device.
 *
 * Scheduled periodically with battery-friendly constraints; also safe to
 * run expedited after big media changes.
 */
class IndexWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as OpenGalleryApp
        val db = app.container.database
        val resolver = applicationContext.contentResolver

        val indexed = db.indexDao().indexedIds().toHashSet()

        // Collect un-indexed images (videos are searchable by name/type only).
        val pending = ArrayList<MediaItem>()
        var offset = 0
        while (true) {
            val page = MediaQuery.queryPage(
                resolver, offset = offset, limit = 500,
                selection = "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} = " +
                    "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}",
            )
            if (page.isEmpty()) break
            page.forEach { if (it.id !in indexed) pending.add(it) }
            offset += page.size
            if (pending.size >= MAX_PER_RUN) break
        }
        if (pending.isEmpty()) return Result.success()

        val embedder = FaceEmbedder(applicationContext)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )

        try {
            for (item in pending.take(MAX_PER_RUN)) {
                if (isStopped) break
                runCatching { indexOne(item, db, embedder, labeler, textRecognizer, faceDetector) }
            }
            clusterFaces(db)
        } finally {
            labeler.close()
            textRecognizer.close()
            faceDetector.close()
        }
        // More to do? Chain another run.
        return if (pending.size > MAX_PER_RUN) Result.retry() else Result.success()
    }

    private suspend fun indexOne(
        item: MediaItem,
        db: foss.opengallery.app.data.db.OgDatabase,
        embedder: FaceEmbedder,
        labeler: com.google.mlkit.vision.label.ImageLabeler,
        textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
        faceDetector: com.google.mlkit.vision.face.FaceDetector,
    ) {
        val bitmap = loadBitmap(item) ?: run {
            db.indexDao().upsert(
                MediaIndexEntity(item.id, "", "", 0, System.currentTimeMillis())
            )
            return
        }
        val input = InputImage.fromBitmap(bitmap, 0)

        val labels = runCatching {
            labeler.process(input).await()
                .filter { it.confidence > 0.65f }
                .joinToString(" ") { it.text }
        }.getOrDefault("")

        val ocr = runCatching {
            textRecognizer.process(input).await().text.replace('\n', ' ').take(4000)
        }.getOrDefault("")

        var faceCount = 0
        if (embedder.available) {
            runCatching {
                val faces = faceDetector.process(input).await()
                faceCount = faces.size
                faces.take(MAX_FACES_PER_IMAGE).forEach { face ->
                    cropFace(bitmap, face.boundingBox)?.let { crop ->
                        embedder.embed(crop)?.let { vector ->
                            db.faceDao().insert(
                                FaceEntity(
                                    mediaId = item.id,
                                    embedding = FaceEmbedder.toBytes(vector),
                                    personId = null,
                                )
                            )
                        }
                    }
                }
            }
        }

        val gps = readGps(item)
        val place = gps?.let { geocode(it.first, it.second) }

        db.indexDao().upsert(
            MediaIndexEntity(
                mediaId = item.id,
                ocrText = ocr,
                labels = labels,
                faceCount = faceCount,
                indexedAtMillis = System.currentTimeMillis(),
                latitude = gps?.first,
                longitude = gps?.second,
                city = place?.first,
                country = place?.second,
            )
        )
    }

    /** EXIF GPS; unredacted only when ACCESS_MEDIA_LOCATION is granted. */
    private fun readGps(item: MediaItem): Pair<Double, Double>? = runCatching {
        val resolver = applicationContext.contentResolver
        val uri = if (
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.ACCESS_MEDIA_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 29
        ) android.provider.MediaStore.setRequireOriginal(item.uri) else item.uri
        resolver.openInputStream(uri)?.use { stream ->
            androidx.exifinterface.media.ExifInterface(stream).latLong
        }?.let { it[0] to it[1] }
    }.getOrNull()

    /** Best-effort reverse geocode (city, country); null offline. */
    private fun geocode(lat: Double, lon: Double): Pair<String?, String?>? = runCatching {
        @Suppress("DEPRECATION")
        val addresses = android.location.Geocoder(applicationContext)
            .getFromLocation(lat, lon, 1)
        addresses?.firstOrNull()?.let { address ->
            (address.locality ?: address.subAdminArea) to address.countryName
        }
    }.getOrNull()

    /** Greedy incremental clustering by cosine similarity to centroids. */
    private suspend fun clusterFaces(db: foss.opengallery.app.data.db.OgDatabase) {
        val unassigned = db.faceDao().unassigned()
        if (unassigned.isEmpty()) return
        data class Cluster(val id: Long, var centroid: FloatArray, var count: Int)

        val clusters = db.faceDao().people().map {
            Cluster(it.id, FaceEmbedder.fromBytes(it.centroid), it.faceCount)
        }.toMutableList()

        for (face in unassigned) {
            val v = FaceEmbedder.fromBytes(face.embedding)
            val best = clusters.maxByOrNull { FaceEmbedder.cosine(it.centroid, v) }
            if (best != null && FaceEmbedder.cosine(best.centroid, v) >= SIMILARITY_THRESHOLD) {
                db.faceDao().assign(face.id, best.id)
                // Update running centroid.
                for (i in best.centroid.indices) {
                    best.centroid[i] =
                        (best.centroid[i] * best.count + v[i]) / (best.count + 1)
                }
                best.count++
                db.faceDao().updatePerson(
                    best.id, FaceEmbedder.toBytes(best.centroid), best.count
                )
            } else {
                val personId = db.faceDao().insertPerson(
                    PersonEntity(
                        name = null,
                        coverMediaId = face.mediaId,
                        centroid = face.embedding,
                        faceCount = 1,
                    )
                )
                db.faceDao().assign(face.id, personId)
                clusters.add(Cluster(personId, v, 1))
            }
        }
    }

    private fun loadBitmap(item: MediaItem): Bitmap? = runCatching {
        val resolver = applicationContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(item.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= ANALYZE_SIZE &&
            bounds.outHeight / (sample * 2) >= ANALYZE_SIZE
        ) sample *= 2
        resolver.openInputStream(item.uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
            })
        }
    }.getOrNull()

    private fun cropFace(bitmap: Bitmap, box: Rect): Bitmap? = runCatching {
        val left = box.left.coerceAtLeast(0)
        val top = box.top.coerceAtLeast(0)
        val w = box.width().coerceAtMost(bitmap.width - left)
        val h = box.height().coerceAtMost(bitmap.height - top)
        if (w <= 16 || h <= 16) null
        else Bitmap.createBitmap(bitmap, left, top, w, h)
    }.getOrNull()

    companion object {
        private const val MAX_PER_RUN = 300
        private const val MAX_FACES_PER_IMAGE = 10
        private const val ANALYZE_SIZE = 1024
        const val SIMILARITY_THRESHOLD = 0.62f

        /** Schedule periodic background indexing (idle + charging-friendly). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<IndexWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "media-indexing",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Kick a one-shot run now (e.g. first launch after permission). */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<IndexWorker>().build()
            )
        }
    }
}
