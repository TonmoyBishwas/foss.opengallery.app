package foss.opengallery.app.data

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-shot actions on media items (share, trash, delete, favourite).
 * Batch operations on 30+ return a [PendingIntent] whose IntentSender must
 * be launched so the system can ask the user for consent.
 */
object MediaActions {

    /** Sharing more uris than this overflows the 1 MB binder transaction. */
    const val MAX_SHARE_ITEMS = 500

    fun shareIntent(uris: List<Uri>, mimeHint: String = "*/*"): Intent {
        val bounded = uris.take(MAX_SHARE_ITEMS)
        val intent = if (bounded.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, bounded.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(bounded))
            }
        }
        return intent.apply {
            type = mimeHint
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, null) }
    }

    @RequiresApi(30)
    fun trashRequest(resolver: ContentResolver, uris: List<Uri>, trash: Boolean = true): PendingIntent =
        MediaStore.createTrashRequest(resolver, uris, trash)

    @RequiresApi(30)
    fun deleteRequest(resolver: ContentResolver, uris: List<Uri>): PendingIntent =
        MediaStore.createDeleteRequest(resolver, uris)

    @RequiresApi(30)
    fun favoriteRequest(resolver: ContentResolver, uris: List<Uri>, favorite: Boolean): PendingIntent =
        MediaStore.createFavoriteRequest(resolver, uris, favorite)

    /**
     * Direct delete for API 26–29. On 29, deleting an item we don't own
     * throws RecoverableSecurityException — the returned IntentSender must
     * be launched so the user can consent, then the delete retried. Returns
     * null when everything was deleted (or skipped) without needing consent.
     */
    suspend fun deleteDirect(context: Context, uris: List<Uri>): IntentSender? =
        withContext(Dispatchers.IO) {
            for (uri in uris) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= 29 && e is RecoverableSecurityException) {
                        return@withContext e.userAction.actionIntent.intentSender
                    }
                    // 26–28 without write access: skip instead of crashing.
                }
            }
            null
        }

    fun editIntent(uri: Uri, mimeType: String): Intent =
        Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

    fun canUseSystemTrash(): Boolean = Build.VERSION.SDK_INT >= 30
}
