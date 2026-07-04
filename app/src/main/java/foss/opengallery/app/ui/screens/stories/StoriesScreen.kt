package foss.opengallery.app.ui.screens.stories

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import foss.opengallery.app.data.MediaQuery
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** An auto-generated story: a dense burst of photos in a time window. */
data class Story(
    val id: String,
    val title: String,
    val dateLabel: String,
    val cover: MediaItem,
    val itemIds: List<Long>,
)

class StoriesViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    init {
        viewModelScope.launch { _stories.value = buildStories() }
    }

    /**
     * Purely algorithmic story mining (no AI, works offline):
     *  - group camera-roll photos by day
     *  - a day with >= 10 photos becomes a "Shots of the day"
     *  - consecutive photo-heavy days merge into a trip-style story
     */
    private suspend fun buildStories(): List<Story> = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val items = MediaQuery.queryPage(
            resolver, offset = 0, limit = 4000,
            selection = "${MediaQuery.MEDIA_TYPE_SELECTION} AND bucket_display_name = ?",
            selectionArgs = arrayOf("Camera"),
        ).ifEmpty {
            MediaQuery.queryPage(resolver, offset = 0, limit = 4000)
        }
        if (items.isEmpty()) return@withContext emptyList()

        val zone = ZoneId.systemDefault()
        val byDay = items.groupBy {
            Instant.ofEpochMilli(it.takenAtMillis).atZone(zone).toLocalDate()
        }
        val denseDays = byDay.filterValues { it.size >= MIN_PHOTOS }.toSortedMap()
        if (denseDays.isEmpty()) return@withContext emptyList()

        // Merge consecutive dense days into ranges.
        val ranges = ArrayList<Pair<LocalDate, LocalDate>>()
        var start: LocalDate? = null
        var prev: LocalDate? = null
        for (day in denseDays.keys) {
            if (start == null) {
                start = day
            } else if (prev != null && day != prev.plusDays(1)) {
                ranges.add(start to prev)
                start = day
            }
            prev = day
        }
        if (start != null && prev != null) ranges.add(start to prev)

        val dayFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        ranges.sortedByDescending { it.second }.take(MAX_STORIES).map { (from, to) ->
            val storyItems = denseDays
                .filterKeys { it in from..to }
                .values.flatten()
                .sortedBy { it.takenAtMillis }
            Story(
                id = "$from..$to",
                title = if (from == to) "Shots of the day"
                else "Highlights: ${from.year}",
                dateLabel = if (from == to) dayFmt.format(from)
                else "${dayFmt.format(from)} – ${dayFmt.format(to)}",
                cover = storyItems[storyItems.size / 2],
                itemIds = storyItems.map { it.id },
            )
        }
    }

    private companion object {
        const val MIN_PHOTOS = 10
        const val MAX_STORIES = 30
    }
}

/** Stories tab: full-width story cards like the reference design. */
@Composable
fun StoriesScreen(onOpenStory: (Story) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as foss.opengallery.app.OpenGalleryApp).container
    val settings by container.settingsRepository.settings.collectAsState(
        initial = foss.opengallery.app.data.settings.SettingsRepository.Settings()
    )
    val vm = ogViewModel { _ ->
        StoriesViewModel(context.applicationContext as Application)
    }
    val allStories by vm.stories.collectAsState()
    val stories = if (settings.autoCreateStories) allStories else emptyList()

    LazyColumn(Modifier.fillMaxSize().background(OgColors.Background)) {
        item {
            Text(
                "Explore your stories",
                style = OgType.SectionHeader,
                color = OgColors.TitleBlue,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }
        if (stories.isEmpty()) {
            item {
                Text(
                    "Stories are created automatically from busy days in your " +
                        "camera roll — entirely on this device. They will appear " +
                        "here once you have enough pictures.",
                    style = OgType.Body,
                    color = OgColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        items(stories, key = { it.id }) { story ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(OgShapes.Card)
                    .clickable { onOpenStory(story) },
            ) {
                AsyncImage(
                    model = story.cover.uri,
                    contentDescription = story.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color(0xB3000000),
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        story.title,
                        style = OgType.SectionHeader,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        story.dateLabel,
                        style = OgType.ItemSecondary,
                        color = Color(0xCCFFFFFF),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
