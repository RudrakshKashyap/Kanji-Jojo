package ua.syt0r.kanji.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.koin.core.module.Module
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract

interface NetworkClients {

    val unauthenticatedClient: HttpClient

    val authenticatedClient: HttpClient
    fun invalidateTokens()

}

fun Module.addNetworkClientsDefinitions() {

    factory<HttpClientEngineFactory<*>> { CIO }

    single<NetworkClients> {
        DefaultNetworkClients(
            appPreferences = get(),
            engineFactory = get()
        )
    }

}

class DefaultNetworkClients(
    private val appPreferences: PreferencesContract.AppPreferences,
    engineFactory: HttpClientEngineFactory<*>
) : NetworkClients {

    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

    init {
        appPreferences.refreshToken.onModified
            .onEach { invalidateTokens() }
            .launchIn(coroutineScope)
    }

    override val unauthenticatedClient: HttpClient = HttpClient(engineFactory)
    override val authenticatedClient: HttpClient = HttpClient(engineFactory) {
        install(Auth) {
            bearer {
                sendWithoutRequest { it.url.host == "kanji-jojo.com" && it.url.protocol.name == "https" }
                loadTokens(::loadInitialTokens)
                refreshTokens(::tokenRefreshHandler)
            }
        }
    }

    override fun invalidateTokens() {
        authenticatedClient.authProvider<BearerAuthProvider>()!!.clearToken()
    }

    private suspend fun loadInitialTokens(): BearerTokens? {
        val (refreshToken, idToken) = appPreferences.run {
            refreshToken.get() to idToken.get()
        }
        return if (refreshToken != null && idToken != null) {
            BearerTokens(accessToken = idToken, refreshToken = refreshToken)
        } else {
            null
        }
    }

    private suspend fun tokenRefreshHandler(params: RefreshTokensParams): BearerTokens? {
        Logger.d("Session expired, refreshing token")

        val refreshToken = appPreferences.refreshToken.get()
        if (refreshToken == null) {
            Logger.d("No refresh token found")
            return null
        }

        val newTokens = runCatching {
            val response = unauthenticatedClient.submitForm(
                url = TOKEN_REFRESH_URL,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }
            )
            check(response.status == HttpStatusCode.OK) { "Session refresh failed. Sign in again." }

            Logger.d("response status[${response.status}]")
            val body = Json.decodeFromString<JsonObject>(response.bodyAsText())
            val idToken = body["id_token"]!!.jsonPrimitive.content
            Logger.d("Received new id token")

            val rotatedRefreshToken = body["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
            appPreferences.refreshToken.set(rotatedRefreshToken)
            appPreferences.idToken.set(idToken)
            BearerTokens(
                accessToken = idToken,
                refreshToken = rotatedRefreshToken
            )
        }.getOrElse {
            Logger.d("Session refresh failed")
            null
        }

        return newTokens
    }

    private companion object {
        const val PROJECT_KEY = JojoService.firebaseApiKey
        const val TOKEN_REFRESH_URL = "https://securetoken.googleapis.com/v1/token?key=$PROJECT_KEY"
    }

}
