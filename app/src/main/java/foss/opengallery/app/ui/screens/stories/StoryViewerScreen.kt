package foss.opengallery.app.ui.screens.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.components.HeaderIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Auto-advancing story slideshow with per-page progress segments.
 * Takes the story's day range and re-derives its items, mirroring
 * [StoriesViewModel.buildStories] membership (camera roll first, whole
 * library as fallback) — passing thousands of ids through the nav route
 * risked TransactionTooLargeException on process save.
 */
@Composable
fun StoryViewerScreen(
    fromEpochDay: Long,
    toEpochDay: Long,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val items by produceState<List<MediaItem>>(emptyList(), fromEpochDay, toEpochDay) {
        value = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val startMs = LocalDate.ofEpochDay(fromEpochDay)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val endMs = LocalDate.ofEpochDay(toEpochDay).plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            // Same taken-time fallback as MediaItem.takenAtMillis: datetaken
            // when present, otherwise date_added (seconds).
            val timeSel = "((datetaken > 0 AND datetaken >= ? AND datetaken < ?) OR " +
                "((datetaken IS NULL OR datetaken <= 0) AND " +
                "date_added >= ? AND date_added < ?))"
            val timeArgs = arrayOf(
                startMs.toString(), endMs.toString(),
                (startMs / 1000).toString(), (endMs / 1000).toString(),
            )
            val resolver = context.contentResolver
            MediaQuery.queryPage(
                resolver, offset = 0, limit = 4000,
                selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND " +
                    "bucket_display_name = ? AND $timeSel",
                selectionArgs = arrayOf("Camera") + timeArgs,
            ).ifEmpty {
                MediaQuery.queryPage(
                    resolver, offset = 0, limit = 4000,
                    selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND $timeSel",
                    selectionArgs = timeArgs,
                )
            }.sortedBy { it.takenAtMillis }
        }
    }
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(pageCount = { items.size })
    var paused by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, paused, items.size) {
        if (!paused) {
            delay(2600)
            val next = pagerState.currentPage + 1
            if (next < items.size) {
                pagerState.animateScrollToPage(next)
            } else {
                onClose()
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { paused = !paused },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = items[page].uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Progress segments.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            repeat(items.size) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 1.5.dp)
                        .height(3.dp)
                        .background(
                            if (i <= pagerState.currentPage) Color.White
                            else Color(0x55FFFFFF)
                        )
                )
            }
        }
        Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 14.dp)) {
            HeaderIconButton(HeaderAction.Back) { onClose() }
        }
    }
}
