package foss.opengallery.app.ui

import android.net.Uri

/** Navigation routes. */
object Routes {
    const val HOME = "home"
    const val ALL_ALBUMS = "allAlbums"

    /** type: bucket | virtual | custom. id: bucketId, virtual key, or custom id. */
    const val ALBUM = "album/{type}/{id}?title={title}"

    fun album(type: String, id: String, title: String): String =
        "album/$type/$id?title=${Uri.encode(title)}"

    fun bucketAlbum(bucketId: Long, title: String) = album("bucket", bucketId.toString(), title)
    fun virtualAlbum(key: String, title: String) = album("virtual", key, title)
    fun customAlbum(id: Long, title: String) = album("custom", id.toString(), title)

    const val VIEWER = "viewer/{type}/{id}/{mediaId}?sort={sort}"

    fun viewer(type: String, id: String, mediaId: Long, sortEncoded: Int = 0): String =
        "viewer/$type/$id/$mediaId?sort=$sortEncoded"
}
