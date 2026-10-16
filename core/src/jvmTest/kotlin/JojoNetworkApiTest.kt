import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.util.hex
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class JojoNetworkApiTest {
    private fun clients(engine: MockEngine) = object : NetworkClients {
        override val authenticatedClient = HttpClient(engine)
        override val unauthenticatedClient = authenticatedClient
        override fun invalidateTokens() = Unit
    }

    @Test
    fun pkceMatchesRfc7636Vector() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", base64Url(sha256(verifier.encodeToByteArray())))
        assertEquals(43, base64Url(secureAuthBytes()).length)
    }

    @Test
    fun uploadsRawZipWithObservedRevisionAndStudyTimestamp() = runBlocking {
        val bytes = byteArrayOf(0x50, 0x4b, 3, 4, 1)
        val engine = MockEngine { request ->
            assertEquals("https://kanji-jojo.com/api/v1/sync", request.url.toString())
            assertEquals("PUT", request.method.value)
            assertEquals("\"7\"", request.headers["If-Match"])
            assertEquals("deck-id", request.headers["X-Backup-Data-Id"])
            assertEquals("123", request.headers["X-Backup-Data-Timestamp"])
            assertTrue((request.body as OutgoingContent.ByteArrayContent).bytes().contentEquals(bytes))
            respond("{}", HttpStatusCode.OK)
        }
        val api = DefaultNetworkApi(clients(engine), Json { ignoreUnknownKeys = true })
        assertTrue(api.updateSyncData(ApiSyncDataInfo("deck-id", 1, 123), 7, ChannelProvider { ByteReadChannel(bytes) }).isSuccess)
    }

    @Test
    fun validatesDownloadedBackupChecksumAndKeepsSnapshotMetadata() = runBlocking {
        val bytes = byteArrayOf(0x50, 0x4b, 3, 4, 1)
        val checksum = hex(sha256(bytes))
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("download")) {
                assertEquals("2", request.url.parameters["revision"])
                respond(bytes, HttpStatusCode.OK)
            } else respond("""{"backup":{"revision":2,"dataId":"deck-id","dataVersion":1,"dataTimestamp":123,"byteSize":5,"sha256":"$checksum"}}""", HttpStatusCode.OK)
        }
        val api = DefaultNetworkApi(clients(engine), Json { ignoreUnknownKeys = true })
        val result = api.getSyncData(2).getOrThrow().value
        assertEquals(123L, result.info.dataTimestamp)
        assertEquals(2L, result.info.revision)
    }

    @Test
    fun rejectsChangedRevisionWithoutDownloadingOrOverwriting() = runBlocking {
        var downloads = 0
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("download")) downloads++
            respond("""{"backup":{"revision":3,"dataId":"new","dataVersion":1,"byteSize":5,"sha256":"unused"}}""", HttpStatusCode.OK)
        }
        val result = DefaultNetworkApi(clients(engine), Json).getSyncData(2)
        assertEquals(HttpStatusCode.Conflict, (result.exceptionOrNull() as HttpResponseException).statusCode)
        assertEquals(0, downloads)
    }

    @Test
    fun rejectsCorruptBackupAndOversizedUploads() = runBlocking {
        val engine = MockEngine {
            if (it.url.encodedPath.endsWith("download")) respond(byteArrayOf(1, 2, 3, 4, 5), HttpStatusCode.OK)
            else respond("""{"backup":{"revision":1,"dataId":"data","dataVersion":1,"byteSize":5,"sha256":"wrong"}}""", HttpStatusCode.OK)
        }
        val api = DefaultNetworkApi(clients(engine), Json)
        assertTrue(api.getSyncData(1).isFailure)
        assertTrue(api.updateSyncData(ApiSyncDataInfo("data", 1), 1, ChannelProvider { ByteReadChannel(ByteArray(JojoService.maxBackupBytes + 1)) }).isFailure)
    }

    @Test
    fun emptyBackupUsesExistingNoRemoteDataPathAndLegacyFeaturesMakeNoRequests() = runBlocking {
        var calls = 0
        val engine = MockEngine { calls++; respond("""{"backup":null}""", HttpStatusCode.OK) }
        val api = DefaultNetworkApi(clients(engine), Json)
        assertEquals(HttpStatusCode.NoContent, (api.getSyncDataInfo().exceptionOrNull() as HttpResponseException).statusCode)
        assertTrue(api.getDonations().isFailure)
        assertTrue(api.postSubscription("purchase").isFailure)
        assertEquals(1, calls)
    }

    @Test
    fun browserApprovalExchangesProofOverHttpsAndNeverInTheBrowserUrl() = runBlocking {
        var browserUrl: io.ktor.http.Url? = null
        val engine = MockEngine { request ->
            val body = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
            if (request.url.host == "kanji-jojo.com") {
                val verifier = body.getValue("verifier").jsonPrimitive.content
                assertEquals(browserUrl!!.parameters["challenge"], base64Url(sha256(verifier.encodeToByteArray())))
                assertEquals(browserUrl!!.parameters["state"], body.getValue("state").jsonPrimitive.content)
                respond("""{"customToken":"test-custom-token"}""", HttpStatusCode.OK)
            } else {
                assertEquals("identitytoolkit.googleapis.com", request.url.host)
                assertEquals(JojoService.firebaseApiKey, request.url.parameters["key"])
                assertEquals("test-custom-token", body.getValue("token").jsonPrimitive.content)
                respond("""{"refreshToken":"test-refresh","idToken":"test-id"}""", HttpStatusCode.OK)
            }
        }
        val tokens = BrowserAccountAuth(clients(engine)).signIn(false) { url ->
            browserUrl = io.ktor.http.Url(url)
            assertFalse(browserUrl!!.parameters.contains("verifier"))
            assertFalse(url.contains("Token"))
            assertNotNull(browserUrl!!.parameters["challenge"])
        }
        assertEquals("test-refresh", tokens.refreshToken)
        assertEquals("test-id", tokens.idToken)
    }

    @Test
    fun browserApprovalSurvivesTemporaryNetworkAndServerFailures() = runBlocking {
        var polls = 0
        var verifier: String? = null
        val engine = MockEngine { request ->
            if (request.url.host == "kanji-jojo.com") {
                val body = Json.parseToJsonElement((request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()).jsonObject
                val current = body.getValue("verifier").jsonPrimitive.content
                if (verifier != null) assertEquals(verifier, current)
                verifier = current
                when (++polls) {
                    1 -> throw IOException("temporary connection loss")
                    2 -> respond("unavailable", HttpStatusCode.ServiceUnavailable)
                    3 -> respond("rate limited", HttpStatusCode.TooManyRequests)
                    4 -> respond("pending", HttpStatusCode.Accepted)
                    else -> respond("""{"customToken":"custom"}""", HttpStatusCode.OK)
                }
            } else respond("""{"refreshToken":"refresh","idToken":"id"}""", HttpStatusCode.OK)
        }
        val tokens = BrowserAccountAuth(clients(engine), pollIntervalMillis = 1).signIn(false) {}
        assertEquals("id", tokens.idToken)
        assertEquals(5, polls)
    }

    @Test
    fun browserApprovalStopsOnPermanentRejectionAndDoesNotContactFirebase() = runBlocking {
        var calls = 0
        val engine = MockEngine { request ->
            calls++
            assertEquals("kanji-jojo.com", request.url.host)
            respond("expired", HttpStatusCode.Gone)
        }
        assertFailsWith<IllegalStateException> {
            BrowserAccountAuth(clients(engine), pollIntervalMillis = 1).signIn(false) {}
        }
        assertEquals(1, calls)
    }

    @Test
    fun browserApprovalNetworkRetriesKeepTheOverallTimeout() = runBlocking<Unit> {
        val engine = MockEngine { throw IOException("offline") }
        assertFailsWith<TimeoutCancellationException> {
            BrowserAccountAuth(clients(engine), pollIntervalMillis = 1, signInTimeoutMillis = 50).signIn(false) {}
        }
    }
}
