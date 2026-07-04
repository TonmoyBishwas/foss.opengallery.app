package foss.opengallery.app.ui.screens.locations

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import foss.opengallery.app.data.db.MediaIndexEntity
import foss.opengallery.app.data.db.OgDatabase
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** City group derived from the on-device location index. */
data class CityGroup(
    val city: String,
    val country: String,
    val count: Int,
    val coverId: Long,
    val lat: Double,
    val lon: Double,
)

class LocationsViewModel(
    application: Application,
    private val db: OgDatabase,
) : AndroidViewModel(application) {

    private val _groups = MutableStateFlow<List<CityGroup>>(emptyList())
    val groups: StateFlow<List<CityGroup>> = _groups.asStateFlow()

    private val _pins = MutableStateFlow<List<MediaIndexEntity>>(emptyList())
    val pins: StateFlow<List<MediaIndexEntity>> = _pins.asStateFlow()

    init {
        viewModelScope.launch {
            val geotagged = withContext(Dispatchers.IO) { db.indexDao().geotagged() }
            _pins.value = geotagged.take(200)
            _groups.value = geotagged
                .filter { it.city != null }
                .groupBy { it.city!! }
                .map { (city, list) ->
                    val first = list.first()
                    CityGroup(
                        city = city,
                        country = first.country ?: "",
                        count = list.size,
                        coverId = first.mediaId,
                        lat = first.latitude ?: 0.0,
                        lon = first.longitude ?: 0.0,
                    )
                }
                .sortedByDescending { it.count }
        }
    }

    suspend fun cover(id: Long): MediaItem? = withContext(Dispatchers.IO) {
        foss.opengallery.app.data.MediaQuery.queryPage(
            getApplication<Application>().contentResolver,
            offset = 0, limit = 1,
            selection = "${android.provider.MediaStore.MediaColumns._ID} = ?",
            selectionArgs = arrayOf(id.toString()),
        ).firstOrNull()
    }
}

/**
 * Locations: an OpenStreetMap card with photo pins (no API keys) above
 * country/city groups built from on-device geocoding.
 */
@Composable
fun LocationsScreen(
    onBack: () -> Unit,
    onOpenCity: (String) -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel { c ->
        LocationsViewModel(context.applicationContext as Application, c.database)
    }
    val groups by vm.groups.collectAsState()
    val pins by vm.pins.collectAsState()
    val cityCount = groups.size

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "Locations",
            visible = true,
            subtitle = if (cityCount == 1) "1 city" else "$cityCount cities",
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            if (pins.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(14.dp)
                            .clip(OgShapes.Card),
                    ) {
                        OsmMap(pins)
                    }
                }
            }
            if (groups.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No located pictures yet. Locations appear as your pictures " +
                            "are indexed (EXIF location access required).",
                        style = OgType.Body,
                        color = OgColors.TextSecondary,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            groups.groupBy { it.country }.forEach { (country, cities) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "country:$country") {
                    Text(
                        country.ifBlank { "Unknown" },
                        style = OgType.SectionHeader,
                        color = OgColors.TitleBlue,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
                items(cities, key = { "city:${it.city}" }) { group ->
                    CityCard(vm, group) { onOpenCity(group.city) }
                }
            }
        }
    }
}

@Composable
private fun CityCard(vm: LocationsViewModel, group: CityGroup, onClick: () -> Unit) {
    val cover by produceState<MediaItem?>(null, group.coverId) {
        value = vm.cover(group.coverId)
    }
    Column(Modifier.padding(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(OgShapes.AlbumCover)
                .background(OgColors.SurfaceCard)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = cover?.uri,
                contentDescription = group.city,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            group.city,
            style = OgType.ItemLabel,
            color = OgColors.TextPrimary,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        Text(
            group.count.toString(),
            style = OgType.ItemSecondary,
            color = OgColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun OsmMap(pins: List<MediaIndexEntity>) {
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(5.0)
                val first = pins.firstOrNull()
                if (first?.latitude != null && first.longitude != null) {
                    controller.setCenter(GeoPoint(first.latitude, first.longitude))
                }
                pins.forEach { pin ->
                    if (pin.latitude != null && pin.longitude != null) {
                        overlays.add(
                            Marker(this).apply {
                                position = GeoPoint(pin.latitude, pin.longitude)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
