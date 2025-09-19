package com.example.alias.data.download

import com.example.alias.data.settings.Settings
import com.example.alias.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

private class FakeSettingsRepo(origins: Set<String>) : SettingsRepository {
    private val flow = MutableStateFlow(
        Settings(trustedSources = origins),
    )

    override val settings: Flow<Settings> = flow
    override suspend fun updateRoundSeconds(value: Int) = Unit
    override suspend fun updateTargetWords(value: Int) = Unit
    override suspend fun updateSkipPolicy(maxSkips: Int, penaltyPerSkip: Int) = Unit
    override suspend fun updatePunishSkips(value: Boolean) = Unit
    override suspend fun updateLanguagePreference(language: String) = Unit
    override suspend fun setEnabledDeckIds(ids: Set<String>) = Unit
    override suspend fun updateAllowNSFW(value: Boolean) = Unit
    override suspend fun updateStemmingEnabled(value: Boolean) = Unit
    override suspend fun updateHapticsEnabled(value: Boolean) = Unit
    override suspend fun updateSoundEnabled(value: Boolean) = Unit
    override suspend fun updateOneHandedLayout(value: Boolean) = Unit
    override suspend fun updateOrientation(value: String) = Unit
    override suspend fun updateUiLanguage(language: String) = Unit
    override suspend fun updateDifficultyFilter(min: Int, max: Int) = Unit
    override suspend fun setCategoriesFilter(categories: Set<String>) = Unit
    override suspend fun setWordClassesFilter(classes: Set<String>) = Unit
    override suspend fun setTeams(teams: List<String>) = Unit
    override suspend fun updateVerticalSwipes(value: Boolean) = Unit
    override suspend fun setTrustedSources(
        origins: Set<String>,
    ) {
        flow.value = flow.value.copy(trustedSources = origins)
    }
    override suspend fun readBundledDeckHashes(): Set<String> = emptySet()
    override suspend fun writeBundledDeckHashes(entries: Set<String>) = Unit
    override suspend fun updateSeenTutorial(value: Boolean) = Unit
    override suspend fun clearAll() = Unit
}

class PackDownloaderTest {

    @Test
    fun downloads_and_verifies_checksum() = runBlocking {
        val body = "hello world".toByteArray()
        withHttpsDownloader { server, downloader ->
            val buf1 = okio.Buffer().write(body)
            server.enqueue(MockResponse().setResponseCode(200).setBody(buf1))
            val url = server.url("/pack.json").toString().replace("http://", "https://")
            val expected = sha256Hex(body)
            val bytes = downloader.download(url, expected)
            assertContentEquals(body, bytes)
        }
    }

    @Test
    fun rejects_wrong_checksum() = runBlocking {
        val body = "content".toByteArray()
        withHttpsDownloader { server, downloader ->
            val buf2 = okio.Buffer().write(body)
            server.enqueue(MockResponse().setResponseCode(200).setBody(buf2))
            val url = server.url("/pack.json").toString().replace("http://", "https://")
            assertFailsWith<IllegalArgumentException> {
                downloader.download(url, "deadbeef")
            }
        }
    }

    @Test
    fun rejects_body_exceeding_max_bytes() = runBlocking {
        val maxBytes = PackDownloader.MAX_BYTES
        val buffer = okio.Buffer()
        val chunk = ByteArray(PackDownloader.CHUNK_SIZE)
        var remaining = maxBytes + 1
        while (remaining > 0) {
            val toWrite = minOf(remaining, chunk.size.toLong()).toInt()
            buffer.write(chunk, 0, toWrite)
            remaining -= toWrite
        }
        withHttpsDownloader { server, downloader ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(buffer))
            val url = server.url("/overflow.bin").toString().replace("http://", "https://")
            assertFailsWith<IllegalArgumentException> {
                downloader.download(url, null)
            }
        }
    }

    @Test
    fun rejects_non_https() {
        runBlocking {
            MockWebServer().use { server ->
                server.start()
                val client = OkHttpClient()
                val downloader = PackDownloader(client, FakeSettingsRepo(setOf("localhost")))
                val httpUrl = server.url("/x").toString() // http
                assertFailsWith<IllegalArgumentException> {
                    downloader.download(httpUrl, null)
                }
            }
        }
    }
}

private suspend fun withHttpsDownloader(
    trustedSources: (String) -> Set<String> = { host -> setOf("https://$host", "localhost") },
    block: suspend (server: MockWebServer, downloader: PackDownloader) -> Unit,
) {
    val localhostCert = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
    val serverCerts = HandshakeCertificates.Builder().heldCertificate(localhostCert).build()
    val clientCerts = HandshakeCertificates.Builder().addTrustedCertificate(localhostCert.certificate).build()
    val server = MockWebServer()
    try {
        server.useHttps(serverCerts.sslSocketFactory(), false)
        server.start()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCerts.sslSocketFactory(), clientCerts.trustManager)
            .build()
        val host = "localhost:${server.port}"
        val downloader = PackDownloader(client, FakeSettingsRepo(trustedSources(host)))
        block(server, downloader)
    } finally {
        server.shutdown()
    }
}

private fun sha256Hex(bytes: ByteArray): String {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    md.update(bytes)
    return bytesToHex(md.digest())
}

private fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") {
    val v = it.toInt() and 0xff
    "%02x".format(v)
}
