package foss.opengallery.app.data.thumbs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension

/**
 * Grid thumbnails via ContentResolver.loadThumbnail (API 29+): uses the
 * system's precomputed thumbnail cache instead of decoding full images,
 * which is what keeps huge-library scrolling smooth.
 *
 * Falls through (returns null factory) below API 29 or for large targets,
 * letting Coil's normal decode pipeline handle it.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val w = (options.size.width as? Dimension.Pixels)?.px ?: DEFAULT_SIZE
        val h = (options.size.height as? Dimension.Pixels)?.px ?: DEFAULT_SIZE
        val bitmap = context.contentResolver.loadThumbnail(data, Size(w, h), null)
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT < 29) return null
            if (data.scheme != ContentResolver.SCHEME_CONTENT) return null
            if (data.authority != "media") return null
            // Only intercept small (grid-sized) requests; the viewer wants originals.
            val w = (options.size.width as? Dimension.Pixels)?.px ?: return null
            if (w > MAX_THUMB_PX) return null
            return MediaStoreThumbnailFetcher(context, data, options)
        }
    }

    private companion object {
        const val DEFAULT_SIZE = 512
        const val MAX_THUMB_PX = 768
    }
}
