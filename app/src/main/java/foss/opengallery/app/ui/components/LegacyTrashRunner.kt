package foss.opengallery.app.ui.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import foss.opengallery.app.OpenGalleryApp
import foss.opengallery.app.data.trash.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drives the API 26–29 delete path: each uri is moved into the app recycle
 * bin (copy, then delete the original). On Android 10 a non-owned item
 * needs a per-item system consent dialog — the runner launches it and
 * resumes the queue when the user grants it.
 *
 * [onFinished] receives the uris that actually reached the bin.
 */
class LegacyTrashRunner internal constructor(
    private val trash: TrashRepository,
    private val scope: CoroutineScope,
) {
    internal lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    internal var onFinished: (List<Uri>) -> Unit = {}

    private val queue = ArrayDeque<Uri>()
    private val trashed = mutableListOf<Uri>()

    fun start(uris: List<Uri>) {
        queue.clear()
        trashed.clear()
        queue.addAll(uris)
        process()
    }

    internal fun onConsentResult(granted: Boolean) {
        if (granted) {
            process() // retry the same head uri — access is now granted
        } else {
            queue.clear()
            finish()
        }
    }

    private fun process() {
        scope.launch {
            while (queue.isNotEmpty()) {
                when (val outcome = trash.legacyTrashUri(queue.first())) {
                    is TrashRepository.LegacyTrashOutcome.Done -> {
                        trashed.add(queue.removeFirst())
                    }
                    is TrashRepository.LegacyTrashOutcome.Failed -> {
                        queue.removeFirst() // skip, keep going
                    }
                    is TrashRepository.LegacyTrashOutcome.NeedsConsent -> {
                        launcher.launch(
                            IntentSenderRequest.Builder(outcome.sender).build()
                        )
                        return@launch
                    }
                }
            }
            finish()
        }
    }

    private fun finish() {
        onFinished(trashed.toList())
        trashed.clear()
    }
}

/**
 * Drives Android 11+ system trash for arbitrarily large selections: one
 * createTrashRequest per [BATCH]-sized chunk (a whole-library selection in
 * a single request overflows the 1 MB binder transaction), continuing
 * through the queue as the user confirms each consent dialog.
 */
class SystemTrashRunner internal constructor(
    private val context: android.content.Context,
) {
    internal lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
    internal var onFinished: () -> Unit = {}

    private val queue = ArrayDeque<List<Uri>>()

    fun start(uris: List<Uri>) {
        if (android.os.Build.VERSION.SDK_INT < 30 || uris.isEmpty()) {
            onFinished()
            return
        }
        queue.clear()
        uris.chunked(BATCH).forEach(queue::add)
        next()
    }

    private fun next() {
        val batch = queue.firstOrNull() ?: run { onFinished(); return }
        launcher.launch(
            IntentSenderRequest.Builder(
                android.provider.MediaStore.createTrashRequest(
                    context.contentResolver, batch, true
                ).intentSender
            ).build()
        )
    }

    internal fun onResult(granted: Boolean) {
        if (!granted) {
            queue.clear()
            onFinished()
            return
        }
        queue.removeFirst()
        next()
    }

    private companion object {
        const val BATCH = 1000
    }
}

@Composable
fun rememberSystemTrashRunner(onFinished: () -> Unit): SystemTrashRunner {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val runner = remember { SystemTrashRunner(context.applicationContext) }
    runner.onFinished = { currentOnFinished() }
    runner.launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        runner.onResult(result.resultCode == Activity.RESULT_OK)
    }
    return runner
}

@Composable
fun rememberLegacyTrashRunner(onFinished: (trashed: List<Uri>) -> Unit): LegacyTrashRunner {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val runner = remember {
        val container = (context.applicationContext as OpenGalleryApp).container
        LegacyTrashRunner(container.trashRepository, scope)
    }
    runner.onFinished = { uris -> currentOnFinished(uris) }
    runner.launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        runner.onConsentResult(result.resultCode == Activity.RESULT_OK)
    }
    return runner
}
