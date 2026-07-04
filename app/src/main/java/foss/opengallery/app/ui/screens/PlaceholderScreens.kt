package foss.opengallery.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

// Stories gets its real implementation in M9.

@Composable
fun StoriesScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Stories", style = OgType.HeroTitle, color = OgColors.TitleBlue)
    }
}
