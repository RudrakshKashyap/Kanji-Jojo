package ua.syt0r.kanji.core

import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.hex
import io.ktor.util.network.UnresolvedAddressException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ua.syt0r.kanji.core.user_data.preferences.PreferencesSyncDataInfo

interface NetworkApi {

    suspend fun getUserInfo(): Result<ApiUserInfo>
    suspend fun getUserId(): Result<String>

    suspend fun getSyncDataInfo(): Result<SubscriptionResponse<ApiSyncDataInfo>>
    suspend fun getSyncData(revision: Long): Result<SubscriptionResponse<RemoteSyncData>>
    suspend fun updateSyncData(
        syncDataInfo: ApiSyncDataInfo,
        baseRevision: Long,
        channelProvider: ChannelProvider
    ): Result<SubscriptionResponse<Unit>>

    suspend fun postFeedback(data: FeedbackApiData): Result<Unit>
    suspend fun postDonationPurchase(data: DonationPurchaseApiData): Result<Unit>
    suspend fun getDonations(): Result<List<ApiDonation>>
    suspend fun postSubscription(purchaseJson: String): Result<Unit>
    suspend fun postTextAnalysisRequest(
        request: ApiTextAnalysisRequest
    ): Result<SubscriptionResponse<ApiTextAnalysisResponse>>

}

data class HttpResponseException(
    val statusCode: HttpStatusCode,
    override val message: String?
) : Throwable()

data class SubscriptionResponse<T>(
    val value: T,
    val alert: String?
)

sealed interface ApiRequestIssue {

    data object NoConnection : ApiRequestIssue
    data object NotAuthenticated : ApiRequestIssue
    data object NoSubscription : ApiRequestIssue
    data class Other(val throwable: Throwable) : ApiRequestIssue

    companion object {
        fun classify(throwable: Throwable): ApiRequestIssue {
            return when (throwable) {
                is UnresolvedAddressException -> NoConnection
                is HttpResponseException -> when (throwable.statusCode) {
                    HttpStatusCode.Unauthorized -> NotAuthenticated
                    HttpStatusCode.PaymentRequired -> NoSubscription
                    else -> Other(throwable)
                }

                else -> {
                    Other(throwable)
                }
            }
        }
    }

}


@Serializable
data class ApiUserInfo(
    val email: String,
    val subscription: Boolean,
    val subscriptionDue: Long? = null
)

@Serializable
data class ApiSyncDataInfo(
    val dataId: String,
    val dataVersion: Long,
    val dataTimestamp: Long? = null,
    val revision: Long = 0
)

@Serializable
data class ApiTextAnalysisRequest(
    val text: String
)

@Serializable
data class ApiTextAnalysisResponse(
    val text: String,
    val translation: String,
    val elements: JsonArray
)

fun ApiSyncDataInfo.toPreferencesType() =
    PreferencesSyncDataInfo(dataId, dataVersion, dataTimestamp)

fun PreferencesSyncDataInfo.toApiType() = ApiSyncDataInfo(dataId, dataVersion, dataTimestamp)

data class FeedbackApiData(
    val topic: String,
    val message: String,
    val userData: JsonObject
)

data class DonationPurchaseApiData(
    val email: String,
    val message: String,
    val purchasesJson: List<String>
)

@Serializable
data class ApiDonation(
    val time: Long,
    val amountJpy: Float
)

data class RemoteSyncData(val info: ApiSyncDataInfo, val channel: ByteReadChannel)

@Serializable
private data class JojoAccount(val uid: String, val email: String? = null)
@Serializable
private data class JojoCapabilities(val sync: Boolean = false)
@Serializable
private data class JojoAccountResponse(val account: JojoAccount, val capabilities: JojoCapabilities)
@Serializable
private data class JojoBackup(
    val revision: Long,
    val dataId: String,
    val dataVersion: Long,
    val dataTimestamp: Long? = null,
    val byteSize: Int,
    val sha256: String
) {
    fun toApiType() = ApiSyncDataInfo(dataId, dataVersion, dataTimestamp, revision)
}
@Serializable
private data class JojoBackupResponse(val backup: JojoBackup?)

class DefaultNetworkApi(
    private val networkClients: NetworkClients,
    private val json: Json
) : NetworkApi {
    private suspend fun account(): JojoAccountResponse = json.decodeFromString(
        checked { networkClients.authenticatedClient.get("${JojoService.api}/account") }.bodyAsText()
    )

    override suspend fun getUserInfo(): Result<ApiUserInfo> = runCatching {
        val result = account()
        // The inherited subscription flag now represents free sync access,
        // not a paid subscription or access to the old operator's services.
        ApiUserInfo(result.account.email ?: "Kanji Jojo account", result.capabilities.sync)
    }

    override suspend fun getUserId(): Result<String> = runCatching { account().account.uid }

    private suspend fun backup(): JojoBackup = json.decodeFromString<JojoBackupResponse>(
        checked { networkClients.authenticatedClient.get("${JojoService.api}/v1/sync") }.bodyAsText()
    ).backup ?: throw HttpResponseException(HttpStatusCode.NoContent, "No cloud backup")

    override suspend fun getSyncDataInfo(): Result<SubscriptionResponse<ApiSyncDataInfo>> = runCatching {
        SubscriptionResponse(backup().toApiType(), null)
    }

    override suspend fun getSyncData(revision: Long): Result<SubscriptionResponse<RemoteSyncData>> = runCatching {
        val info = backup()
        if (info.revision != revision) throw HttpResponseException(HttpStatusCode.Conflict, "The remote backup changed. Refresh sync and try again.")
        val response = checked {
            networkClients.authenticatedClient.get("${JojoService.api}/v1/sync/download") {
                url { parameters.append("revision", revision.toString()) }
            }
        }
        val bytes = response.bodyAsChannel().readRemaining(JojoService.maxBackupBytes.toLong() + 1).readByteArray()
        check(bytes.size <= JojoService.maxBackupBytes && bytes.size == info.byteSize) { "Invalid backup size" }
        check(hex(sha256(bytes)) == info.sha256) { "Backup checksum verification failed" }
        SubscriptionResponse(RemoteSyncData(info.toApiType(), ByteReadChannel(bytes)), null)
    }

    override suspend fun updateSyncData(
        syncDataInfo: ApiSyncDataInfo,
        baseRevision: Long,
        channelProvider: ChannelProvider
    ): Result<SubscriptionResponse<Unit>> = runCatching {
        val bytes = channelProvider.block().readRemaining(JojoService.maxBackupBytes.toLong() + 1).readByteArray()
        check(bytes.size <= JojoService.maxBackupBytes) { "Cloud backups must be 4 MB or smaller. Use local export for larger backups." }
        checked {
            networkClients.authenticatedClient.put("${JojoService.api}/v1/sync") {
                contentType(ContentType.Application.Zip)
                header(HttpHeaders.IfMatch, "\"$baseRevision\"")
                header("X-Backup-Data-Id", syncDataInfo.dataId)
                header("X-Backup-Data-Version", syncDataInfo.dataVersion.toString())
                syncDataInfo.dataTimestamp?.let { header("X-Backup-Data-Timestamp", it.toString()) }
                setBody(bytes)
            }
        }
        SubscriptionResponse(Unit, null)
    }

    // These legacy services have no replacement implementation. Never send
    // feedback, purchases, or study text to the previous operator.
    private fun <T> unavailable(): Result<T> = Result.failure(HttpResponseException(
        HttpStatusCode.NotImplemented,
        "This feature is not available on the new Kanji Jojo service yet."
    ))
    override suspend fun postFeedback(data: FeedbackApiData): Result<Unit> = unavailable()
    override suspend fun postDonationPurchase(data: DonationPurchaseApiData): Result<Unit> = unavailable()
    override suspend fun getDonations(): Result<List<ApiDonation>> = unavailable()
    override suspend fun postSubscription(purchaseJson: String): Result<Unit> = unavailable()
    override suspend fun postTextAnalysisRequest(request: ApiTextAnalysisRequest): Result<SubscriptionResponse<ApiTextAnalysisResponse>> = unavailable()

    private suspend fun checked(block: suspend () -> HttpResponse): HttpResponse {
        val response = block()
        if (response.status != HttpStatusCode.OK) {
            val message = runCatching {
                json.parseToJsonElement(response.bodyAsText()).jsonObject["message"]?.jsonPrimitive?.content
            }.getOrNull() ?: "${response.status.value} ${response.status.description}"
            throw HttpResponseException(response.status, message)
        }
        return response
    }
}
