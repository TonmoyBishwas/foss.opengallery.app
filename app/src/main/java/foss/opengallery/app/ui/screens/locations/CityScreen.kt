package foss.opengallery.app.ui.screens.locations

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.screens.search.SearchViewModel
import foss.opengallery.app.ui.theme.OgColors

/** All pictures geocoded to one city. */
@Composable
fun CityScreen(
    city: String,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as foss.opengallery.app.OpenGalleryApp).container
    val loader = ogViewModel(key = "cityloader") { c ->
        SearchViewModel(context.applicationContext as Application, c.database)
    }
    val items by produceState<List<MediaItem>>(emptyList(), city) {
        val ids = container.database.indexDao().idsForCity(city)
        value = loader.queryByIds(ids)
    }

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = city,
            visible = true,
            subtitle = "${items.size} pictures",
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.id }) { item ->
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
