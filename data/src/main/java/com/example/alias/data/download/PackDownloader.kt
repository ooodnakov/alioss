package com.example.alias.data.download

import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
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
        private const val USER_AGENT = "AliasLocal/dev"
    }

    /**
     * Download the given [url] if it passes policy checks. Optionally verify [expectedSha256] (hex).
     */
    suspend fun download(
        url: String,
        expectedSha256: String? = null,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): ByteArray {
        val httpUrl = url.toHttpUrlOrNull() ?: error("Invalid URL")
        require(httpUrl.isHttps) { "TLS required" }

        val allowed = allowedHost(httpUrl)
        require(allowed) { "Host not allow-listed" }

        val req = Request.Builder()
            .url(httpUrl)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()

        client.newBuilder()
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
                if (contentLength != -1L) require(contentLength <= MAX_BYTES) { "File too large" }
                val source = body.source()
                val out = ByteArrayOutputStream()
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                val chunk = ByteArray(CHUNK_SIZE)
                onProgress(0L, totalBytes)
                while (true) {
                    val read = source.read(chunk)
                    if (read == -1) break
                    total += read
                    require(total <= MAX_BYTES) { "File too large" }
                    out.write(chunk, 0, read)
                    digest.update(chunk, 0, read)
                    onProgress(total, totalBytes)
                }
                val bytes = out.toByteArray()
                if (expectedSha256 != null) {
                    val got = digest.digest().toHex()
                    require(got.equals(expectedSha256, ignoreCase = true)) { "Checksum mismatch" }
                }
                return bytes
            }
    }

    private suspend fun allowedHost(url: HttpUrl): Boolean {
        val host = url.host.lowercase()
        val ports = listOf(url.port, 443)
        val allowed = settings.settings.first().trustedSources
        if (allowed.isEmpty()) return false
        // Accept either host-only entries or origin (scheme://host[:port]) entries
        val origins = buildList {
            for (p in ports) add("https://${'$'}host:${'$'}p")
            add(host)
        }
        return origins.any { it in allowed }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { b -> "%02x".format(b) }
