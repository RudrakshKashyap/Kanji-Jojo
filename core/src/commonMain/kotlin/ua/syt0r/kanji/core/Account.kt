package ua.syt0r.kanji.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import org.koin.core.module.Module
import ua.syt0r.kanji.core.time.TimeUtils
import ua.syt0r.kanji.core.user_data.preferences.PreferencesContract
import ua.syt0r.kanji.core.user_data.preferences.PreferencesUserInfo

interface AccountManager {
    val subscriptionExpirationEvents: SharedFlow<Unit>
    val state: StateFlow<AccountState>
    fun signIn(refreshToken: String, idToken: String)
    fun signInWithBrowser(openBrowser: (String) -> Unit, desktop: Boolean = false)
    fun signOut()
    fun refreshUserData()
    fun invalidateAuth()
    fun invalidateSubscription()
}

sealed interface AccountState {

    data object Loading : AccountState

    data object LoggedOut : AccountState

    data class LoggedIn(
        val email: String,
        val subscriptionInfo: SubscriptionInfo,
        val issue: ApiRequestIssue?
    ) : AccountState

    data class Error(
        val issue: ApiRequestIssue
    ) : AccountState

}

sealed interface SubscriptionInfo {
    data object Inactive : SubscriptionInfo
    data class Active(val due: LocalDateTime?) : SubscriptionInfo
    data class Expired(val due: LocalDateTime?) : SubscriptionInfo
}

fun Module.addAccountDefinitions() {

    single<AccountManager> {
        DefaultAccountManager(
            coroutineScope = CoroutineScope(Dispatchers.IO),
            appPreferences = get(),
            networkApi = get(),
            networkClients = get(),
            browserAccountAuth = BrowserAccountAuth(get()),
            timeUtils = get()
        )
    }

}

class DefaultAccountManager(
    private val coroutineScope: CoroutineScope,
    private val appPreferences: PreferencesContract.AppPreferences,
    private val networkApi: NetworkApi,
    private val networkClients: NetworkClients,
    private val browserAccountAuth: BrowserAccountAuth,
    private val timeUtils: TimeUtils
) : AccountManager {

    private val _subscriptionExpirationEvents = MutableSharedFlow<Unit>()
    override val subscriptionExpirationEvents: SharedFlow<Unit> = _subscriptionExpirationEvents

    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    override val state: StateFlow<AccountState> = _state
    private var signInJob: Job? = null

    init {
        _state.launchWhenHasSubscribers(coroutineScope) { updateStateFromLocal() }
    }

    override fun signIn(refreshToken: String, idToken: String) {
        _state.value = AccountState.Loading
        coroutineScope.launch {
            storeSession(refreshToken, idToken)
            updateStateFromRemote()
        }
    }

    override fun signInWithBrowser(openBrowser: (String) -> Unit, desktop: Boolean) {
        signInJob?.cancel()
        _state.value = AccountState.Loading
        signInJob = coroutineScope.launch {
            try {
                val tokens = browserAccountAuth.signIn(desktop) { url ->
                    withContext(Dispatchers.Main) { openBrowser(url) }
                }
                storeSession(tokens.refreshToken, tokens.idToken)
                updateStateFromRemote()
            } catch (error: TimeoutCancellationException) {
                _state.value = AccountState.Error(ApiRequestIssue.Other(
                    IllegalStateException("App sign-in timed out. Please start again.")
                ))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = AccountState.Error(ApiRequestIssue.classify(error))
            }
        }
    }

    override fun signOut() {
        signInJob?.cancel()
        _state.value = AccountState.LoggedOut
        coroutineScope.launch { clearUserData() }
    }

    override fun refreshUserData() {
        _state.value = AccountState.Loading
        coroutineScope.launch { updateStateFromRemote() }
    }

    override fun invalidateAuth() {
        coroutineScope.launch { updateStateFromLocal(ApiRequestIssue.NotAuthenticated) }
    }

    override fun invalidateSubscription() {
        coroutineScope.launch { _subscriptionExpirationEvents.emitWhenWithSubscribers(Unit) }
        coroutineScope.launch { updateStateFromRemote() }
    }

    private suspend fun updateStateFromRemote() {
        val apiUserInfo = networkApi.getUserInfo().getOrElse {
            updateStateFromLocal(ApiRequestIssue.classify(it))
            return
        }

        val userInfo = apiUserInfo.toPreferencesType()
        appPreferences.userInfo.set(userInfo)

        updateStateFromLocal()
    }

    private suspend fun updateStateFromLocal(issue: ApiRequestIssue? = null) {
        val userInfo = appPreferences.userInfo.get()

        _state.value = when {
            userInfo != null -> AccountState.LoggedIn(
                email = userInfo.email,
                subscriptionInfo = userInfo.getSubscriptionInfo(),
                issue = issue
            )

            issue != null -> AccountState.Error(issue)

            else -> AccountState.LoggedOut
        }
    }

    private fun PreferencesUserInfo.getSubscriptionInfo(): SubscriptionInfo {
        val dueInstant = subscriptionDue?.let { Instant.fromEpochMilliseconds(it) }
        return when {
            !subscriptionEnabled && dueInstant == null -> {
                SubscriptionInfo.Inactive
            }

            subscriptionEnabled -> {
                when {
                    dueInstant == null || dueInstant >= timeUtils.now() -> {
                        SubscriptionInfo.Active(dueInstant?.toLocalDateTime())
                    }

                    else -> {
                        SubscriptionInfo.Expired(dueInstant.toLocalDateTime())
                    }
                }
            }

            else -> {
                SubscriptionInfo.Expired(dueInstant?.toLocalDateTime())
            }
        }
    }

    private fun ApiUserInfo.toPreferencesType(): PreferencesUserInfo = PreferencesUserInfo(
        email = email,
        subscriptionEnabled = subscription,
        subscriptionDue = subscriptionDue
    )

    private suspend fun storeSession(refreshToken: String, idToken: String) {
        appPreferences.refreshToken.set(refreshToken)
        appPreferences.idToken.set(idToken)
        // Preference observers run asynchronously. The immediate profile lookup
        // must use this session even if those observers have not run yet.
        networkClients.invalidateTokens()
        appPreferences.userInfo.set(null)
        appPreferences.lastSyncedDataInfo.set(null)
    }

    private suspend fun clearUserData() {
        appPreferences.apply {
            refreshToken.set(null)
            idToken.set(null)
            userInfo.set(null)
        }
        networkClients.invalidateTokens()
    }

}
