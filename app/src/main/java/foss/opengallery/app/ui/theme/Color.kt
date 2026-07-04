package foss.opengallery.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OpenGallery color tokens — a dark-first, pure-black palette in the
 * One UI visual style (all values are our own approximations).
 */
object OgColors {
    // Backgrounds
    val Background = Color(0xFF000000)
    val SurfaceCard = Color(0xFF17171B)          // settings cards, sheets
    val SurfacePopup = Color(0xFF2E2F35)         // floating context menus
    val SurfaceSheet = Color(0xFF1C1C20)         // bottom sheets
    val SurfaceChip = Color(0xFF26262B)

    // Text
    val TextPrimary = Color(0xFFFAFAFA)
    val TextSecondary = Color(0xFF9E9EA3)
    val TextTertiary = Color(0xFF6F6F75)

    // The signature pale-blue used for big titles and section headers
    val TitleBlue = Color(0xFFB4C8E4)
    // Interactive/link blue
    val AccentBlue = Color(0xFF3E7EFF)
    // Switch active track
    val SwitchTrackOn = Color(0xFF3E7EFF)
    val SwitchTrackOff = Color(0xFF4A4A50)

    // Misc
    val NotificationDot = Color(0xFFFF7A00)
    val FavouriteHeart = Color(0xFFFF6C5C)
    val Divider = Color(0xFF2A2A2E)
    val ScrimVeil = Color(0x99000000)
    val SelectionCheck = Color(0xFF3E7EFF)
}
