package foss.opengallery.app.ui.screens.pictures

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import foss.opengallery.app.data.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One cell of the Pictures timeline: a full-width date header or a thumb. */
sealed interface TimelineCell {
    data class Header(val label: String, val key: String) : TimelineCell
    data class Media(val item: MediaItem) : TimelineCell
}

/** Day / Month / Year grouping — cycled by pinching past the density limits. */
enum class TimelineGrouping { Day, Month, Year }

object TimelineFormat {

    private val dayThisYear = DateTimeFormatter.ofPattern("EEE, d MMMM", Locale.getDefault())
    private val dayOtherYear = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
    private val monthThisYear = DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())
    private val monthOtherYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val scrubberFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())

    fun localDate(item: MediaItem): LocalDate =
        Instant.ofEpochMilli(item.takenAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun groupKey(item: MediaItem, grouping: TimelineGrouping): String {
        val d = localDate(item)
        return when (grouping) {
            TimelineGrouping.Day -> "${d.year}-${d.monthValue}-${d.dayOfMonth}"
            TimelineGrouping.Month -> "${d.year}-${d.monthValue}"
            TimelineGrouping.Year -> "${d.year}"
        }
    }

    fun headerLabel(item: MediaItem, grouping: TimelineGrouping): String {
        val d = localDate(item)
        val today = LocalDate.now()
        return when (grouping) {
            TimelineGrouping.Day -> when (d) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> if (d.year == today.year) dayThisYear.format(d) else dayOtherYear.format(d)
            }
            TimelineGrouping.Month ->
                if (d.year == today.year) monthThisYear.format(d) else monthOtherYear.format(d)
            TimelineGrouping.Year -> d.year.toString()
        }
    }

    fun scrubberLabel(item: MediaItem): String = scrubberFormat.format(localDate(item))
}

/** Injects date headers between items whenever the group key changes. */
fun Flow<PagingData<MediaItem>>.withDateHeaders(
    grouping: TimelineGrouping,
): Flow<PagingData<TimelineCell>> = map { paging ->
    paging
        .map { item -> TimelineCell.Media(item) as TimelineCell }
        .insertSeparators { before, after ->
            val afterMedia = (after as? TimelineCell.Media)?.item ?: return@insertSeparators null
            val beforeMedia = (before as? TimelineCell.Media)?.item
            val afterKey = TimelineFormat.groupKey(afterMedia, grouping)
            if (beforeMedia == null ||
                TimelineFormat.groupKey(beforeMedia, grouping) != afterKey
            ) {
                TimelineCell.Header(
                    label = TimelineFormat.headerLabel(afterMedia, grouping),
                    key = afterKey,
                )
            } else null
        }
}
