package group.two.knockledge.ai

import android.content.Context
import group.two.knockledge.models.ModelDownloader
import group.two.knockledge.models.ModelStorage
import group.two.knockledge.settings.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Models live in no-backup app storage, never synced to the cloud.
 * scripts/models.sh copies files into this same folder over USB.
 */
class AndroidModelStorage(context: Context) : ModelStorage {
    /** Path of the no-backup "models" folder, created on first access. */
    override val directory: String =
        File(context.noBackupFilesDir, "models").apply { mkdirs() }.absolutePath

    /** Size of [path] in bytes, or -1 when it is not a file. */
    override fun sizeOf(path: String): Long = File(path).let { if (it.isFile) it.length() else -1L }

    /** Renames [from] to [to], replacing an existing file at the target; false if the move failed. */
    override fun move(from: String, to: String): Boolean {
        val target = File(to)
        if (target.exists()) target.delete()
        return File(from).renameTo(target)
    }

    /** Deletes [path]; true when it is gone afterwards, including when it never existed. */
    override fun delete(path: String): Boolean = File(path).let { !it.exists() || it.delete() }
}

/** Settings persisted in SharedPreferences. */
class PrefsStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("knockledge_settings", Context.MODE_PRIVATE)
    /** Stored value for [key], or null when it was never written. */
    override fun getString(key: String): String? = prefs.getString(key, null)
    /** Writes [key], or removes it when [value] is null. */
    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}

/** Resumable HTTP download. Hugging Face redirects to its CDN, which honors Range requests. */
class HttpModelDownloader : ModelDownloader {
    /**
     * Appends to [partPath] from [existingBytes] onward and reports progress about every 2 MB.
     * Runs on [Dispatchers.IO] and throws [IOException] when the server refuses the request or
     * the file ends shorter than its advertised length.
     */
    override suspend fun download(
        url: String,
        partPath: String,
        existingBytes: Long,
        onProgress: (writtenBytes: Long) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        val part = File(partPath)
        var existing = existingBytes
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "Knockledge-Android/0.2")
            if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
        }
        try {
            val code = conn.responseCode
            if (code == 416 && existing > 0L) return@withContext // already complete
            if (code == HttpURLConnection.HTTP_OK && existing > 0L) existing = 0L // server ignored Range
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("HTTP $code")
            }
            val length = conn.contentLengthLong.takeIf { it > 0L }?.plus(existing)
            conn.inputStream.use { input ->
                FileOutputStream(part, existing > 0L).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var written = existing
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        written += n
                        if (written - lastReport >= 2_000_000L) {
                            lastReport = written
                            onProgress(written)
                        }
                    }
                    if (length != null && written != length) {
                        throw IOException("incomplete file ($written of $length bytes)")
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
