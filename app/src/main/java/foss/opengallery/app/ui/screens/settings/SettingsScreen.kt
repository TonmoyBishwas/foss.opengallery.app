package foss.opengallery.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import foss.opengallery.app.data.settings.SettingsRepository
import foss.opengallery.app.ui.components.CompactHeaderBar
import foss.opengallery.app.ui.components.HeaderAction
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType
import kotlinx.coroutines.launch

/** Gallery settings: One UI-style grouped rounded cards with switches. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as foss.opengallery.app.OpenGalleryApp).container
    val repo = container.settingsRepository
    val settings by repo.settings.collectAsState(initial = SettingsRepository.Settings())
    val scope = rememberCoroutineScope()

    fun toggle(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        scope.launch { repo.set(key, value) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(OgColors.Background)
    ) {
        CompactHeaderBar(
            title = "Gallery settings",
            visible = true,
            actions = listOf(HeaderAction.Back),
            onAction = { if (it == HeaderAction.Back) onBack() },
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            GroupLabel("Viewing options")
            SettingsCard {
                ToggleRow(
                    title = "Auto play motion photos",
                    checked = settings.autoPlayMotion,
                ) { toggle(SettingsRepository.Keys.AutoPlayMotion, it) }
            }

            GroupLabel("Stories")
            SettingsCard {
                ToggleRow(
                    title = "Auto-create stories",
                    subtitle = "Stories are generated on this device only. " +
                        "Turning this off really turns it off.",
                    checked = settings.autoCreateStories,
                ) { toggle(SettingsRepository.Keys.AutoCreateStories, it) }
            }

            GroupLabel("Search")
            SettingsCard {
                ToggleRow(
                    title = "On-device indexing",
                    subtitle = "Recognize text and objects in pictures and group " +
                        "people — all offline, nothing leaves your phone.",
                    checked = settings.indexingEnabled,
                ) { toggle(SettingsRepository.Keys.IndexingEnabled, it) }
            }

            GroupLabel("Privacy")
            SettingsCard {
                ToggleRow(
                    title = "Remove location data when sharing",
                    subtitle = "Share copies with GPS metadata stripped.",
                    checked = settings.stripLocationOnShare,
                ) { toggle(SettingsRepository.Keys.StripLocationOnShare, it) }
                Divider()
                LinkRow("Permissions") {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        )
                    )
                }
            }

            GroupLabel("About")
            SettingsCard {
                LinkRow(
                    "About Gallery",
                    subtitle = "OpenGallery ${versionName(context)} — free and open source, " +
                        "GPL-3.0. No ads, no telemetry, no cloud.",
                ) {}
                Divider()
                LinkRow("Source code on GitHub") {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/TonmoyBishwas/foss.opengallery.app"),
                        )
                    )
                }
                Divider()
                LinkRow("Open source licences") {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://github.com/TonmoyBishwas/foss.opengallery.app/blob/main/THIRD_PARTY_NOTICES.md"
                            ),
                        )
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(
                Modifier.padding(bottom = 30.dp)
            )
        }
    }
}

private fun versionName(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: ""

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = OgType.ItemSecondary,
        color = OgColors.TitleBlue,
        modifier = Modifier.padding(start = 14.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(OgColors.SurfaceCard, OgShapes.Card)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun Divider() {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .background(OgColors.Divider)
            .padding(top = 0.5.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = OgType.ItemLabel, color = OgColors.TextPrimary)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = OgType.Body,
                    color = OgColors.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = OgColors.SwitchTrackOn,
                uncheckedTrackColor = OgColors.SwitchTrackOff,
            ),
        )
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) {
        Text(title, style = OgType.ItemLabel, color = OgColors.TextPrimary)
        if (subtitle != null) {
            Text(
                subtitle,
                style = OgType.Body,
                color = OgColors.TextSecondary,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
