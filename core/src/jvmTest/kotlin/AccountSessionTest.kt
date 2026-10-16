import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.BrowserAccountAuth
import ua.syt0r.kanji.core.DefaultAccountManager
import ua.syt0r.kanji.core.DefaultNetworkApi
import ua.syt0r.kanji.core.DefaultNetworkClients
import ua.syt0r.kanji.core.suspended_property.SuspendedProperty
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesSyncDataInfo
import ua.syt0r.kanji.core.user_data.preferences.PreferencesUserInfo
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountSessionTest {
    // No modification events: model observers that have not processed writes yet.
    private class Property<T>(private var value: T) : SuspendedProperty<T> {
        override val key = "test"
        override val onModified = MutableSharedFlow<T>()
        override suspend fun get() = value
        override suspend fun set(value: T) { this.value = value }
        override suspend fun isModified() = false
        override suspend fun backup(): JsonPrimitive? = error("unused")
        override suspend fun restore(value: JsonPrimitive) = error("unused")
    }

    @Test
    fun signInAndSignOutDoNotReuseCachedBearerCredentials() = runBlocking {
        val refresh = Property<String?>("same-refresh")
        val id = Property<String?>("old-id")
        val userInfo = Property<PreferencesUserInfo?>(PreferencesUserInfo("old@example.com", true, null))
        val properties = mapOf(
            "getRefreshToken" to refresh,
            "getIdToken" to id,
            "getUserInfo" to userInfo,
            "getLastSyncedDataInfo" to Property<PreferencesSyncDataInfo?>(null)
        )
        val preferences = Proxy.newProxyInstance(
            PreferencesContract.AppPreferences::class.java.classLoader,
            arrayOf(PreferencesContract.AppPreferences::class.java)
        ) { _, method, _ -> properties[method.name] ?: error("Unexpected preference ${method.name}") }
            as PreferencesContract.AppPreferences
        val authorizationHeaders = mutableListOf<String?>()
        val factory = object : HttpClientEngineFactory<MockEngineConfig> {
            override fun create(block: MockEngineConfig.() -> Unit) = MockEngine(
                MockEngineConfig().apply(block).apply {
                    addHandler { request ->
                        val authorization = request.headers["Authorization"]
                        authorizationHeaders.add(authorization)
                        val email = if (authorization == "Bearer new-id") "new@example.com" else "old@example.com"
                        respond("""{"account":{"uid":"test","email":"$email"},"capabilities":{"sync":true}}""", HttpStatusCode.OK)
                    }
                }
            )
        }
        val clients = DefaultNetworkClients(preferences, factory)
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val time = object : TimeUtils {
            override fun getCurrentDate(): LocalDate = error("unused")
            override fun getCurrentTime(): LocalDateTime = error("unused")
            override fun now(): Instant = error("unused")
        }
        try {
            // Prime Ktor's cache with the previous session.
            clients.authenticatedClient.get("https://kanji-jojo.com/api/account")
            assertEquals("Bearer old-id", authorizationHeaders.last())
            val manager = DefaultAccountManager(
                scope, preferences, DefaultNetworkApi(clients, Json), clients,
                BrowserAccountAuth(clients), time
            )
            manager.signIn("same-refresh", "new-id")
            withTimeout(2_000) {
                manager.state.first { it is AccountState.LoggedIn && it.email == "new@example.com" }
            }
            assertEquals("Bearer new-id", authorizationHeaders.last())
            assertEquals("new@example.com", userInfo.get()?.email)

            manager.signOut()
            withTimeout(2_000) { clients.authenticatedClient.get("https://kanji-jojo.com/api/account") }
            assertNull(authorizationHeaders.last())
            assertNull(userInfo.get())
        } finally {
            scope.cancel()
            clients.authenticatedClient.close()
            clients.unauthenticatedClient.close()
        }
    }
}
