package foss.opengallery.app.data

import android.provider.MediaStore

/**
 * Per-album persistent sort. Encoded to a stable Int for Room storage.
 * "Date taken" is a first-class option — a top community request that
 * stock galleries often lack.
 */
enum class AlbumSort(val encoded: Int, val label: String) {
    DateAddedDesc(0, "Date added (newest first)"),
    DateAddedAsc(1, "Date added (oldest first)"),
    DateTakenDesc(2, "Date taken (newest first)"),
    DateTakenAsc(3, "Date taken (oldest first)"),
    NameAsc(4, "Name (A to Z)"),
    NameDesc(5, "Name (Z to A)"),
    SizeDesc(6, "Size (largest first)"),
    SizeAsc(7, "Size (smallest first)"),
    ;

    // Every order ends in _ID so it is total: LIMIT/OFFSET pages over a sort
    // with ties are otherwise nondeterministic, which duplicates or drops
    // rows at page boundaries (and duplicate ids crash the lazy grid).
    fun toSqlSort(): String = when (this) {
        DateAddedDesc -> "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"
        DateAddedAsc -> "${MediaStore.MediaColumns.DATE_ADDED} ASC, ${MediaStore.MediaColumns._ID} ASC"
        DateTakenDesc -> "datetaken DESC, ${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"
        DateTakenAsc -> "datetaken ASC, ${MediaStore.MediaColumns.DATE_ADDED} ASC, ${MediaStore.MediaColumns._ID} ASC"
        NameAsc -> "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE ASC, ${MediaStore.MediaColumns._ID} ASC"
        NameDesc -> "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE DESC, ${MediaStore.MediaColumns._ID} DESC"
        SizeDesc -> "${MediaStore.MediaColumns.SIZE} DESC, ${MediaStore.MediaColumns._ID} DESC"
        SizeAsc -> "${MediaStore.MediaColumns.SIZE} ASC, ${MediaStore.MediaColumns._ID} ASC"
    }

    companion object {
        fun fromEncoded(value: Int): AlbumSort =
            entries.firstOrNull { it.encoded == value } ?: DateAddedDesc
    }
}
