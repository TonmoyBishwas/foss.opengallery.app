package foss.opengallery.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Corner shapes used across the app (One UI's big rounded corners). */
object OgShapes {
    val AlbumCover = RoundedCornerShape(24.dp)
    val Card = RoundedCornerShape(26.dp)
    val Popup = RoundedCornerShape(26.dp)
    val Sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    val Chip = RoundedCornerShape(50)
    val Thumb = RoundedCornerShape(0.dp)
}

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = OgColors.AccentBlue,
    onPrimary = OgColors.TextPrimary,
    background = OgColors.Background,
    onBackground = OgColors.TextPrimary,
    surface = OgColors.Background,
    onSurface = OgColors.TextPrimary,
    surfaceVariant = OgColors.SurfaceCard,
    onSurfaceVariant = OgColors.TextSecondary,
    surfaceContainer = OgColors.SurfaceSheet,
    surfaceContainerHigh = OgColors.SurfacePopup,
    outline = OgColors.Divider,
)

@Composable
fun OpenGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Light theme lands in M10; dark is the reference design.
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
