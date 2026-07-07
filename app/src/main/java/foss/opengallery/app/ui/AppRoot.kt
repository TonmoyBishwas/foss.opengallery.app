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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import foss.opengallery.app.ui.components.MainTab
import foss.opengallery.app.ui.components.OneUiBottomTabs
import foss.opengallery.app.ui.screens.albums.AlbumDetailScreen
import foss.opengallery.app.ui.screens.albums.AlbumsScreen
import foss.opengallery.app.ui.screens.albums.AllAlbumsScreen
import foss.opengallery.app.ui.screens.albums.HideAlbumsScreen
import foss.opengallery.app.ui.screens.pictures.PicturesScreen
import foss.opengallery.app.ui.theme.OgColors

/** App shell: NavHost + the Pictures · Albums · Stories · ☰ tab bar. */
@Composable
fun AppRoot() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .background(OgColors.Background),
    ) {
        composable(Routes.HOME) {
            HomeTabs(
                onNavigate = { route -> nav.navigate(route) },
            )
        }
        composable(Routes.ALL_ALBUMS) {
            AllAlbumsScreen(
                onBack = { nav.popBackStack() },
                onNavigate = { route -> nav.navigate(route) },
            )
        }
        composable("hideAlbums") {
            HideAlbumsScreen(onBack = { nav.popBackStack() })
        }
        composable("recycleBin") {
            foss.opengallery.app.ui.screens.recycle.RecycleBinScreen(
                onBack = { nav.popBackStack() },
            )
        }
        composable("lockedFolder") {
            foss.opengallery.app.ui.screens.locked.LockedFolderScreen(
                onBack = { nav.popBackStack() },
            )
        }
        composable("search") {
            foss.opengallery.app.ui.screens.search.SearchScreen(
                onBack = { nav.popBackStack() },
                onNavigate = { route -> nav.navigate(route) },
                onOpenItem = { item ->
                    nav.navigate(Routes.viewer("virtual", "recent", item.id))
                },
            )
        }
        composable("settings") {
            foss.opengallery.app.ui.screens.settings.SettingsScreen(
                onBack = { nav.popBackStack() },
            )
        }
        composable("suggestions") {
            foss.opengallery.app.ui.screens.suggestions.SuggestionsScreen(
                onBack = { nav.popBackStack() },
                onOpenItem = { item ->
                    nav.navigate(Routes.viewer("virtual", "recent", item.id))
                },
            )
        }
        composable("locations") {
            foss.opengallery.app.ui.screens.locations.LocationsScreen(
                onBack = { nav.popBackStack() },
                onOpenCity = { city -> nav.navigate("city/$city") },
            )
        }
        composable(
            route = "city/{city}",
            arguments = listOf(navArgument("city") { type = NavType.StringType }),
        ) { backStackEntry ->
            foss.opengallery.app.ui.screens.locations.CityScreen(
                city = backStackEntry.arguments?.getString("city") ?: "",
                onBack = { nav.popBackStack() },
                onOpenItem = { item ->
                    nav.navigate(Routes.viewer("virtual", "recent", item.id))
                },
            )
        }
        composable(
            route = "story/{from}/{to}",
            arguments = listOf(
                navArgument("from") { type = NavType.LongType },
                navArgument("to") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            foss.opengallery.app.ui.screens.stories.StoryViewerScreen(
                fromEpochDay = backStackEntry.arguments?.getLong("from") ?: 0L,
                toEpochDay = backStackEntry.arguments?.getLong("to") ?: 0L,
                onClose = { nav.popBackStack() },
            )
        }
        composable(
            route = "person/{personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType }),
        ) { backStackEntry ->
            foss.opengallery.app.ui.screens.search.PersonScreen(
                personId = backStackEntry.arguments?.getLong("personId") ?: 0L,
                onBack = { nav.popBackStack() },
                onOpenItem = { item ->
                    nav.navigate(Routes.viewer("virtual", "recent", item.id))
                },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
        ) { backStackEntry ->
            foss.opengallery.app.ui.screens.editor.EditorScreen(
                mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L,
                onClose = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("sort") {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            foss.opengallery.app.ui.screens.viewer.ViewerScreen(
                type = args?.getString("type") ?: "virtual",
                id = args?.getString("id") ?: "recent",
                startMediaId = args?.getLong("mediaId") ?: 0L,
                sortEncoded = args?.getInt("sort") ?: 0,
                onBack = { nav.popBackStack() },
                onEdit = { mediaId -> nav.navigate(Routes.editor(mediaId)) },
            )
        }
        composable(
            route = Routes.ALBUM,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val albumType = args?.getString("type") ?: "bucket"
            val albumId = args?.getString("id") ?: "0"
            AlbumDetailScreen(
                type = albumType,
                id = albumId,
                title = args?.getString("title") ?: "",
                onBack = { nav.popBackStack() },
                onOpenItem = { item ->
                    nav.navigate(Routes.viewer(albumType, albumId, item.id))
                },
            )
        }
    }
}

@Composable
private fun HomeTabs(onNavigate: (String) -> Unit) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Pictures) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var selectionActive by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OgColors.Background)
    ) {
        Box(Modifier.weight(1f)) {
            // Keep each tab's remembered state (grid scroll position etc.)
            // alive across tab switches instead of resetting every time.
            val tabStateHolder = rememberSaveableStateHolder()
            tabStateHolder.SaveableStateProvider(currentTab) {
                when (currentTab) {
                    MainTab.Pictures -> PicturesScreen(
                        onOpenItem = { item ->
                            onNavigate(Routes.viewer("virtual", "recent", item.id))
                        },
                        onOpenSearch = { onNavigate("search") },
                        onSelectionModeChange = { selectionActive = it },
                    )
                    MainTab.Albums -> AlbumsScreen(
                        onNavigate = onNavigate,
                    )
                    MainTab.Stories -> foss.opengallery.app.ui.screens.stories.StoriesScreen(
                        onOpenStory = { story ->
                            onNavigate("story/${story.fromEpochDay}/${story.toEpochDay}")
                        },
                    )
                }
            }
        }
        if (!selectionActive) {
            OneUiBottomTabs(
                current = currentTab,
                onSelect = { currentTab = it },
                onMenuClick = { drawerOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
        }
    }

    if (drawerOpen) {
        DrawerSheet(
            onDismiss = { drawerOpen = false },
            onNavigate = { route ->
                drawerOpen = false
                onNavigate(route)
            },
        )
    }
}
