package foss.opengallery.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/** Media read access state, including Android 14+ partial ("selected photos") access. */
enum class MediaAccess { Full, Partial, Denied }

fun currentMediaAccess(context: Context): MediaAccess {
    fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return when {
        Build.VERSION.SDK_INT >= 34 -> when {
            granted(Manifest.permission.READ_MEDIA_IMAGES) &&
                granted(Manifest.permission.READ_MEDIA_VIDEO) -> MediaAccess.Full
            granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> MediaAccess.Partial
            else -> MediaAccess.Denied
        }
        Build.VERSION.SDK_INT >= 33 ->
            if (granted(Manifest.permission.READ_MEDIA_IMAGES) &&
                granted(Manifest.permission.READ_MEDIA_VIDEO)
            ) MediaAccess.Full else MediaAccess.Denied
        else ->
            @Suppress("DEPRECATION")
            if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) MediaAccess.Full
            else MediaAccess.Denied
    }
}

fun mediaPermissionsToRequest(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    else -> arrayOf(@Suppress("DEPRECATION") Manifest.permission.READ_EXTERNAL_STORAGE)
}

/**
 * Gate that shows [content] once media access is granted (full or partial),
 * and a One UI-style request screen otherwise. Re-checks on every resume so
 * changes made in system settings are picked up.
 */
@Composable
fun MediaAccessGate(content: @Composable (MediaAccess) -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(currentMediaAccess(context)) }
    var requestedOnce by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = currentMediaAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestedOnce = true
        access = currentMediaAccess(context)
    }

    if (access != MediaAccess.Denied) {
        // Media is readable: make sure background indexing is scheduled.
        androidx.compose.runtime.LaunchedEffect(Unit) {
            foss.opengallery.app.data.index.IndexWorker.schedule(context)
            foss.opengallery.app.data.index.IndexWorker.runNow(context)
        }
        content(access)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Allow access to your photos and videos",
                style = OgType.SectionHeader,
                color = OgColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Gallery needs permission to show the pictures and videos on this device. " +
                    "Nothing ever leaves your phone.",
                style = OgType.Body,
                color = OgColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )
            Button(
                onClick = {
                    if (requestedOnce) {
                        // Likely permanently denied; send to app settings.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        launcher.launch(mediaPermissionsToRequest())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OgColors.AccentBlue),
            ) {
                Text(if (requestedOnce) "Open settings" else "Continue")
            }
        }
    }
}
