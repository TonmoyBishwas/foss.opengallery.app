package foss.opengallery.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

// Temporary shells for the three tabs; each is replaced by its real
// implementation in later milestones (M3 Pictures, M4 Albums, M9 Stories).

@Composable
fun PicturesScreen() {
    HeroPlaceholder("Pictures")
}

@Composable
fun AlbumsScreen() {
    HeroPlaceholder("Albums")
}

@Composable
fun StoriesScreen() {
    HeroPlaceholder("Stories")
}

@Composable
private fun HeroPlaceholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = OgType.HeroTitle, color = OgColors.TitleBlue)
    }
}
