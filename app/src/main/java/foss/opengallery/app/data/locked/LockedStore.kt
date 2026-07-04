package foss.opengallery.app.data.locked

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import foss.opengallery.app.data.db.LockedItemEntity
import foss.opengallery.app.data.db.OgDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The Locked Folder: files are AES-GCM encrypted with a non-exportable
 * Android Keystore key and stored in app-private storage, so they are
 * invisible to MediaStore, other apps, USB browsing, and (via backup
 * exclusion rules) cloud backups.
 *
 * Format per blob: [12-byte IV][ciphertext].
 */
class LockedStore(
    private val context: Context,
    private val db: OgDatabase,
) {
    private val dir: File by lazy {
        File(context.filesDir, "locked").apply { mkdirs() }
    }

    val items: Flow<List<LockedItemEntity>> = db.lockedDao().observeAll()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypts [source] into the vault; returns the new row id. */
    suspend fun add(
        source: Uri,
        displayName: String,
        mimeType: String,
        sizeBytes: Long,
        nowMillis: Long,
    ): Long = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.bin"
        val outFile = File(dir, fileName)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        context.contentResolver.openInputStream(source)?.use { input ->
            outFile.outputStream().use { raw ->
                raw.write(cipher.iv) // 12 bytes
                CipherOutputStream(raw, cipher).use { encrypted ->
                    input.copyTo(encrypted)
                }
            }
        } ?: error("Cannot open $source")
        db.lockedDao().insert(
            LockedItemEntity(
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                addedAtMillis = nowMillis,
                fileName = fileName,
            )
        )
    }

    /** Opens a decrypting stream for a vault item. */
    suspend fun openDecrypted(itemId: Long): Pair<LockedItemEntity, InputStream>? =
        withContext(Dispatchers.IO) {
            val item = db.lockedDao().get(itemId) ?: return@withContext null
            val file = File(dir, item.fileName)
            if (!file.exists()) return@withContext null
            val raw = file.inputStream()
            val iv = ByteArray(GCM_IV_LENGTH)
            var read = 0
            while (read < iv.size) {
                val r = raw.read(iv, read, iv.size - read)
                if (r <= 0) break
                read += r
            }
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            }
            item to CipherInputStream(raw, cipher)
        }

    /** Decrypts to a MediaStore file, restoring it to the gallery. */
    suspend fun restore(itemId: Long, writeTo: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            val (_, stream) = openDecrypted(itemId) ?: return@withContext false
            stream.use { it.copyTo(writeTo) }
            true
        }

    suspend fun delete(itemId: Long) = withContext(Dispatchers.IO) {
        db.lockedDao().get(itemId)?.let { item ->
            File(dir, item.fileName).delete()
            db.lockedDao().delete(itemId)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "opengallery_locked_folder"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
    }
}
