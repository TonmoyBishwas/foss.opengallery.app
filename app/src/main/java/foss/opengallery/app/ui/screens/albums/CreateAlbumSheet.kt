package foss.opengallery.app.ui.screens.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgShapes
import foss.opengallery.app.ui.theme.OgType

/**
 * "Choose what to create" sheet: Album / Auto-updating album / Group.
 * Auto-updating albums unlock once People arrives (M8) — shown disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAlbumSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, isGroup: Boolean) -> Unit,
) {
    var namingMode by rememberSaveable { mutableStateOf<String?>(null) } // null | album | group
    var name by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = OgShapes.Sheet,
        containerColor = OgColors.SurfaceSheet,
    ) {
        Column(Modifier.padding(horizontal = 26.dp).padding(bottom = 30.dp)) {
            if (namingMode == null) {
                Text(
                    "Choose what to create",
                    style = OgType.SectionHeader,
                    color = OgColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                CreateEntry(
                    title = "Album",
                    description = "Create a new album and add pictures and videos manually.",
                ) { namingMode = "album" }
                CreateEntry(
                    title = "Auto-updating album",
                    description = "Create an album that automatically updates to include " +
                        "pictures of people you select. Available once People search is set up.",
                    enabled = false,
                ) {}
                CreateEntry(
                    title = "Group",
                    description = "Create a group of related albums.",
                ) { namingMode = "group" }
            } else {
                Text(
                    if (namingMode == "group") "Create group" else "Create album",
                    style = OgType.SectionHeader,
                    color = OgColors.TextPrimary,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(if (namingMode == "group") "Group name" else "Album name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = { onCreate(name, namingMode == "group") },
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
private fun CreateEntry(
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(
            title,
            style = OgType.ItemLabel,
            color = if (enabled) OgColors.TextPrimary else OgColors.TextTertiary,
        )
        Text(
            description,
            style = OgType.Body,
            color = if (enabled) OgColors.TextSecondary else OgColors.TextTertiary,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
