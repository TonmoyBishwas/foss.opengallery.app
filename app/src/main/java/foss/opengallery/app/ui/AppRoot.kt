package foss.opengallery.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import foss.opengallery.app.ui.components.MainTab
import foss.opengallery.app.ui.components.OneUiBottomTabs
import foss.opengallery.app.ui.screens.AlbumsScreen
import foss.opengallery.app.ui.screens.StoriesScreen
import foss.opengallery.app.ui.screens.pictures.PicturesScreen
import foss.opengallery.app.ui.theme.OgColors

/**
 * App shell: the three main tabs plus the drawer sheet, mirroring the
 * reference design's Pictures · Albums · Stories · ☰ bottom bar.
 */
@Composable
fun AppRoot() {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Pictures) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OgColors.Background)
    ) {
        Box(Modifier.weight(1f)) {
            when (currentTab) {
                MainTab.Pictures -> PicturesScreen()
                MainTab.Albums -> AlbumsScreen()
                MainTab.Stories -> StoriesScreen()
            }
        }
        OneUiBottomTabs(
            current = currentTab,
            onSelect = { currentTab = it },
            onMenuClick = { drawerOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }

    if (drawerOpen) {
        DrawerSheet(onDismiss = { drawerOpen = false })
    }
}
