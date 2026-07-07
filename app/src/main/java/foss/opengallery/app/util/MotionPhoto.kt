package foss.opengallery.app.util

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Motion-photo detection and extraction.
 *
 * A motion photo is a still JPEG/HEIC with an MP4 appended to the same
 * file. Vendors mark the video offset differently:
 *  - Samsung: a raw "MotionPhoto_Data" marker; video follows immediately.
 *  - Older Google (MVIMG): XMP "MicroVideoOffset" = bytes from EOF.
 *  - Google Motion Photo 1.0: XMP Container directory with Item:Length.
 *
 * We scan the file once, prefer explicit XMP offsets, and fall back to the
 * Samsung marker. Extraction copies the trailing MP4 slice into cache so
 * Media3 can play it without re-encoding.
 */
object MotionPhoto {

    private val SAMSUNG_MARKER = "MotionPhoto_Data".toByteArray(Charsets.US_ASCII)
    private val MICRO_VIDEO_OFFSET = Regex("MicroVideoOffset\\s*=\\s*\"(\\d+)\"")
    private val MOTION_PHOTO_FLAG = Regex("MotionPhoto\\s*=\\s*\"1\"|MicroVideo\\s*=\\s*\"1\"")
    private val ITEM_LENGTH = Regex("Item:Length\\s*=\\s*\"(\\d+)\"")
    private val ITEM_MIME_VIDEO = "Item:Mime=\"video/mp4\""

    data class VideoSlice(val offset: Long, val length: Long)

    /** Cheap detection: only sniffs the head (XMP) for motion markers. */
    fun isLikelyMotionPhoto(resolver: ContentResolver, uri: Uri, mimeType: String): Boolean {
        if (!mimeType.startsWith("image/")) return false
        val head = readHead(resolver, uri, 128 * 1024) ?: return false
        val text = String(head, Charsets.ISO_8859_1)
        return MOTION_PHOTO_FLAG.containsMatchIn(text)
    }

    /** Locates the embedded MP4 without loading the whole file into heap. */
    fun findVideo(resolver: ContentResolver, uri: Uri): VideoSlice? {
        val size = fileSize(resolver, uri) ?: return null
        val head = readHead(resolver, uri, 160 * 1024) ?: return null
        val headText = String(head, Charsets.ISO_8859_1)

        // Google MVIMG: offset counted back from EOF.
        MICRO_VIDEO_OFFSET.find(headText)?.let { m ->
            val fromEnd = m.groupValues[1].toLongOrNull() ?: return@let
            if (fromEnd in 1 until size) return VideoSlice(size - fromEnd, fromEnd)
        }
        // Google Motion Photo 1.0: the video item is last; its Length counts
        // back from EOF too (plus padding we can ignore for playback).
        if (headText.contains(ITEM_MIME_VIDEO)) {
            val lengths = ITEM_LENGTH.findAll(headText)
                .mapNotNull { it.groupValues[1].toLongOrNull() }
                .toList()
            val videoLen = lengths.lastOrNull()
            if (videoLen != null && videoLen in 1 until size) {
                return VideoSlice(size - videoLen, videoLen)
            }
        }
        // Samsung marker: MP4 begins right after the marker bytes. Streamed
        // windowed scan — motion photos can be 100 MB+.
        val markerAt = findMarker(resolver, uri, SAMSUNG_MARKER)
        if (markerAt >= 0) {
            val start = markerAt + SAMSUNG_MARKER.size
            if (start < size) return VideoSlice(start, size - start)
        }
        return null
    }

    /** Copies the MP4 slice to a cache file Media3 can open. */
    fun extractToCache(
        resolver: ContentResolver,
        uri: Uri,
        slice: VideoSlice,
        cacheDir: File,
        key: String,
    ): File? {
        val out = File(cacheDir, "motion_$key.mp4")
        if (out.exists() && out.length() == slice.length) return out
        resolver.openInputStream(uri)?.use { input ->
            var toSkip = slice.offset
            while (toSkip > 0) {
                val skipped = input.skip(toSkip)
                if (skipped <= 0) return null
                toSkip -= skipped
            }
            FileOutputStream(out).use { output ->
                val buf = ByteArray(64 * 1024)
                var remaining = slice.length
                while (remaining > 0) {
                    val read = input.read(buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    remaining -= read
                }
            }
        } ?: return null
        return out
    }

    private fun readHead(resolver: ContentResolver, uri: Uri, limit: Int): ByteArray? =
        runCatching {
            resolver.openInputStream(uri)?.use { it.readNBytesCompat(limit) }
        }.getOrNull()

    private fun fileSize(resolver: ContentResolver, uri: Uri): Long? =
        runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize.takeIf { it > 0 }
            }
        }.getOrNull()

    /**
     * Absolute offset of [needle] in the stream, or -1. Scans in 1 MB
     * windows with a needle-sized carry so matches across chunk borders
     * are found; memory stays O(window).
     */
    private fun findMarker(resolver: ContentResolver, uri: Uri, needle: ByteArray): Long =
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val buf = ByteArray(1 shl 20)
                val keep = needle.size - 1
                var carry = ByteArray(0)
                var base = 0L // absolute offset of carry[0]
                while (true) {
                    var read = 0
                    while (read < buf.size) {
                        val r = input.read(buf, read, buf.size - read)
                        if (r <= 0) break
                        read += r
                    }
                    if (read <= 0) break
                    val hay = carry + buf.copyOfRange(0, read)
                    val idx = indexOf(hay, needle)
                    if (idx >= 0) return@use base + idx
                    base += (hay.size - keep).coerceAtLeast(0)
                    carry = hay.copyOfRange((hay.size - keep).coerceAtLeast(0), hay.size)
                    if (read < buf.size) break
                }
                -1L
            } ?: -1L
        }.getOrDefault(-1L)

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val buf = ByteArray(limit)
        var total = 0
        while (total < limit) {
            val read = read(buf, total, limit - total)
            if (read <= 0) break
            total += read
        }
        return buf.copyOf(total)
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
