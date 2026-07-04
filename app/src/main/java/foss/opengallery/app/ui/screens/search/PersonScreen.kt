package foss.opengallery.app.ui.screens.search

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import foss.opengallery.app.data.model.MediaItem
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.ogViewModel
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/** One person's photos, with inline naming ("Add name"). */
@Composable
fun PersonScreen(
    personId: Long,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val vm = ogViewModel { c ->
        SearchViewModel(context.applicationContext as Application, c.database)
    }
    val items by produceState<List<MediaItem>>(emptyList(), personId) {
        value = vm.personItems(personId)
    }
    var editingName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(OgColors.Background)) {
        CompactHeaderBar(
            title = "People",
            visible = true,
            subtitle = "${items.size} pictures",
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (editingName) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            placeholder = { Text("Name") },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            vm.renamePerson(personId, name)
                            editingName = false
                        }) { Text("Save") }
                    }
                } else {
                    Text(
                        "Add name",
                        style = OgType.ItemLabel,
                        color = OgColors.AccentBlue,
                        modifier = Modifier
                            .clickable { editingName = true }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
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
