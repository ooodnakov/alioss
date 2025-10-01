package com.example.alioss.data.download

import com.example.alioss.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * HTTP downloader for user-requested pack files. Enforces HTTPS + allow-listed hosts,
 * disables redirects, applies size cap, and optionally verifies SHA-256.
 */
class PackDownloader(
    private val client: OkHttpClient,
    private val settings: SettingsRepository,
) {
    companion object {
        // 40 MB cap to avoid memory bombs; tune as needed
        internal const val MAX_BYTES: Long = 40L * 1024 * 1024
        internal const val CHUNK_SIZE: Int = 8 * 1024
        private const val USER_AGENT = "AliossLocal/dev"
    }

    /**
     * Download the given [url] if it passes policy checks. Optionally verify [expectedSha256] (hex).
     */
    suspend fun download(
        url: String,
        expectedSha256: String? = null,
        maxBytes: Long = MAX_BYTES,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): ByteArray {
        val out = ByteArrayOutputStream()
        return executeDownload(
            url = url,
            expectedSha256 = expectedSha256,
            maxBytes = maxBytes,
            onProgress = onProgress,
            chunkWriter = { chunk, read -> out.write(chunk, 0, read) },
            finalize = { out.toByteArray() },
        )
    }

    /**
     * Download the given [url] into [destination] if it passes policy checks. Optionally verify [expectedSha256] (hex).
     * The destination file will be deleted if the download fails validation.
     */
    suspend fun downloadToFile(
        url: String,
        destination: File,
        expectedSha256: String? = null,
        maxBytes: Long = MAX_BYTES,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ) {
        require(!destination.isDirectory) { "Destination must be a file" }
        destination.parentFile?.let { parent ->
            if (!parent.exists()) {
                require(parent.mkdirs()) { "Failed to create parent directories" }
            } else {
                require(parent.isDirectory) { "Destination parent must be a directory" }
            }
        }
        var success = false
        try {
            FileOutputStream(destination, /* append = */ false).use { output ->
                executeDownload(
                    url = url,
                    expectedSha256 = expectedSha256,
                    maxBytes = maxBytes,
                    onProgress = onProgress,
                    chunkWriter = { chunk, read -> output.write(chunk, 0, read) },
                    finalize = {
                        output.fd.sync()
                    },
                )
            }
            success = true
        } finally {
            if (!success) {
                destination.delete()
            }
        }
    }

    private suspend fun <T> executeDownload(
        url: String,
        expectedSha256: String?,
        maxBytes: Long,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit,
        chunkWriter: (chunk: ByteArray, read: Int) -> Unit,
        finalize: () -> T,
    ): T {
        val httpUrl = url.toHttpUrlOrNull() ?: error("Invalid URL")
        require(httpUrl.isHttps) { "TLS required" }

        val allowed = allowedHost(httpUrl)
        require(allowed) { "Host not allow-listed" }

        val req = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        return client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
            .newCall(req)
            .execute().use { resp ->
                require(resp.isSuccessful) { "HTTP ${'$'}{resp.code()}" }
                val body = resp.body ?: error("Empty body")
                val contentLength = body.contentLength()
                val totalBytes = contentLength.takeUnless { it == -1L }
                if (contentLength != -1L) require(contentLength <= maxBytes) { "File too large" }
                val source = body.source()
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                val chunk = ByteArray(CHUNK_SIZE)
                onProgress(0L, totalBytes)
                while (true) {
                    val read = source.read(chunk)
                    if (read == -1) break
                    total += read
                    require(total <= maxBytes) { "File too large" }
                    chunkWriter(chunk, read)
                    digest.update(chunk, 0, read)
                    onProgress(total, totalBytes)
                }
                val digestBytes = digest.digest()
                if (expectedSha256 != null) {
                    val got = digestBytes.toHex()
                    require(got.equals(expectedSha256, ignoreCase = true)) { "Checksum mismatch" }
                }
                finalize()
            }
    }

    private suspend fun allowedHost(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        val ports = listOf(url.port, 443)
        val allowed = settings.settings.first().trustedSources
        if (allowed.isEmpty()) return false
        val allowedNormalized = allowed
            .asSequence()
            .map { it.trimEnd('/').lowercase() }
            .toSet()
        // Accept either host-only entries or origin (scheme://host[:port]) entries
        val origins = buildSet {
            add(host)
            add("https://$host")
            for (port in ports.distinct()) {
                add("https://$host:$port")
            }
        }
        return origins.any { it in allowedNormalized }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
