package foss.opengallery.app.ui.screens.locked

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import foss.opengallery.app.ui.theme.OgColors
import foss.opengallery.app.ui.theme.OgType

/**
 * Device-credential gate for the Locked Folder.
 * API 28+: framework BiometricPrompt with device-credential fallback.
 * API 26–27: KeyguardManager confirm-credential intent.
 */
@Composable
fun LockedAuthGate(
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (unlocked) {
        content()
        return
    }
    val context = LocalContext.current

    val keyguardLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onUnlocked()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Locked folder",
            style = OgType.SectionHeader,
            color = OgColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Verify it's you to view the pictures and videos in your Locked folder. " +
                "They are encrypted and never leave this device.",
            style = OgType.Body,
            color = OgColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 26.dp),
        )
        Button(
            onClick = { authenticate(context, onUnlocked, keyguardLauncher::launch) },
            colors = ButtonDefaults.buttonColors(containerColor = OgColors.AccentBlue),
        ) {
            Text("Unlock")
        }
    }
}

private fun authenticate(
    context: Context,
    onSuccess: () -> Unit,
    launchKeyguard: (Intent) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= 28) {
        val builder = BiometricPrompt.Builder(context)
            .setTitle("Unlock Locked folder")
        if (Build.VERSION.SDK_INT >= 30) {
            builder.setAllowedAuthenticators(
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
        builder.build().authenticate(
            CancellationSignal(),
            context.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult?
                ) {
                    onSuccess()
                }
            },
        )
    } else {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        @Suppress("DEPRECATION")
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock Locked folder", null
        )
        if (intent != null) launchKeyguard(intent) else onSuccess()
    }
}
