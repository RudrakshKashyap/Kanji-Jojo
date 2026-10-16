package ua.syt0r.kanji.core

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.util.encodeBase64
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

expect fun secureAuthBytes(): ByteArray
expect fun sha256(bytes: ByteArray): ByteArray

fun base64Url(bytes: ByteArray) = bytes.encodeBase64().replace('+', '-').replace('/', '_').trimEnd('=')

data class FirebaseSessionTokens(val refreshToken: String, val idToken: String)

class BrowserAccountAuth(
    private val networkClients: NetworkClients,
    private val pollIntervalMillis: Long = 2_000,
    private val signInTimeoutMillis: Long = 300_000
) {
    suspend fun signIn(desktop: Boolean, openBrowser: suspend (String) -> Unit): FirebaseSessionTokens = withTimeout(signInTimeoutMillis) {
        val flowId = base64Url(secureAuthBytes())
        val state = base64Url(secureAuthBytes())
        val verifier = base64Url(secureAuthBytes())
        val challenge = base64Url(sha256(verifier.encodeToByteArray()))
        val url = URLBuilder("${JojoService.origin}/account").apply {
            parameters.append("appAuth", "1")
            parameters.append("flowId", flowId)
            parameters.append("state", state)
            parameters.append("challenge", challenge)
            parameters.append("platform", if (desktop) "desktop" else "mobile")
        }.buildString()
        openBrowser(url)
        while (true) {
            delay(pollIntervalMillis)
            val response = try {
                networkClients.unauthenticatedClient.post("${JojoService.api}/auth/device/token") {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject {
                        put("flowId", flowId)
                        put("state", state)
                        put("verifier", verifier)
                    }.toString())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                continue
            } catch (error: UnresolvedAddressException) {
                continue
            }
            if (response.status == HttpStatusCode.Accepted ||
                response.status == HttpStatusCode.RequestTimeout ||
                response.status == HttpStatusCode.TooManyRequests ||
                response.status.value in 500..599
            ) continue
            check(response.status == HttpStatusCode.OK) { "App sign-in failed. Please start again from the app." }
            val token = Json.parseToJsonElement(response.bodyAsText()).jsonObject.getValue("customToken").jsonPrimitive.content
            val firebaseResponse = networkClients.unauthenticatedClient.post(
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=${JojoService.firebaseApiKey}"
            ) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("token", token); put("returnSecureToken", true) }.toString())
            }
            check(firebaseResponse.status == HttpStatusCode.OK) { "Could not finish Firebase sign-in. Please try again." }
            val credentials = Json.parseToJsonElement(firebaseResponse.bodyAsText()).jsonObject
            return@withTimeout FirebaseSessionTokens(
                refreshToken = credentials.getValue("refreshToken").jsonPrimitive.content,
                idToken = credentials.getValue("idToken").jsonPrimitive.content
            )
        }
        @Suppress("UNREACHABLE_CODE")
        error("App sign-in did not finish")
    }
}
