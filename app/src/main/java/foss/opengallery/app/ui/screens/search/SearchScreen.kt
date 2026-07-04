package foss.opengallery.app.ui.screens.search

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.db.PersonEntity
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.Routes
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

/**
 * Search: query field, People (on-device face clusters), shot-type chips,
 * and combined filename/folder/OCR/label results.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel { c ->
        SearchViewModel(context.applicationContext as Application, c.database)
    }
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val people by vm.people.collectAsState()
    val indexedCount by vm.indexedCount.collectAsState()

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Search",
            visible = true,
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )

        // Query field.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .background(OgColors.SurfaceChip, OgShapes.Chip)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (query.isEmpty()) {
                Text("Search your pictures", style = OgType.Body, color = OgColors.TextTertiary)
            }
            BasicTextField(
                value = query,
                onValueChange = vm::setQuery,
                singleLine = true,
                textStyle = OgType.Body.copy(color = OgColors.TextPrimary),
                cursorBrush = SolidColor(OgColors.AccentBlue),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (query.isBlank()) {
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()) {
                if (people.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "People",
                                style = OgType.SectionHeader,
                                color = OgColors.TitleBlue,
                            )
                            Text(
                                "  ${people.size}",
                                style = OgType.SectionHeader,
                                color = OgColors.TextTertiary,
                            )
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp),
                        ) {
                            people.take(20).forEach { person ->
                                PersonBubble(vm, person) {
                                    onNavigate("person/${person.id}")
                                }
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Shot types",
                        style = OgType.SectionHeader,
                        color = OgColors.TitleBlue,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp),
                    ) {
                        ShotTypeChip("Videos") {
                            onNavigate(Routes.virtualAlbum("videos", "Videos"))
                        }
                        ShotTypeChip("Favourites") {
                            onNavigate(Routes.virtualAlbum("favourites", "Favourites"))
                        }
                        ShotTypeChip("Screenshots") {
                            onNavigate(Routes.virtualAlbum("screenshots", "Screenshots"))
                        }
                        ShotTypeChip("Camera") {
                            onNavigate(Routes.virtualAlbum("camera", "Camera"))
                        }
                        ShotTypeChip("Downloads") {
                            onNavigate(Routes.virtualAlbum("download", "Download"))
                        }
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (indexedCount > 0)
                            "$indexedCount pictures indexed for text and object search."
                        else
                            "Pictures are indexed in the background so you can search " +
                                "text inside photos and objects — no cloud involved.",
                        style = OgType.Body,
                        color = OgColors.TextTertiary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "${results.size} results",
                        style = OgType.Subtitle,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                items(results, key = { it.id }) { item ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(0.75.dp)
                            .background(OgColors.SurfaceChip)
                            .clickable { onOpenItem(item) },
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonBubble(vm: SearchViewModel, person: PersonEntity, onClick: () -> Unit) {
    val cover by produceState<MediaItem?>(null, person.id) {
        value = vm.personCover(person)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(6.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(OgColors.SurfaceChip),
        ) {
            AsyncImage(
                model = cover?.uri,
                contentDescription = person.name ?: "Person",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = person.name ?: "",
            style = OgType.ItemSecondary,
            color = OgColors.TextPrimary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ShotTypeChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = OgType.ItemLabel,
        color = OgColors.TextPrimary,
        modifier = Modifier
            .padding(4.dp)
            .background(OgColors.SurfaceChip, OgShapes.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
