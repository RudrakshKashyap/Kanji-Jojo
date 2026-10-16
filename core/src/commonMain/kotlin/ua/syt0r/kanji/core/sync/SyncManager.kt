package ua.syt0r.kanji.core.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import ua.syt0r.kanji.core.AccountManager
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.ApiRequestIssue
import ua.syt0r.kanji.core.SubscriptionInfo
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.core.sync.use_case.HandleSyncIntentUseCase

interface SyncManager {
    val state: StateFlow<SyncFeatureState>
}

class DefaultSyncManager(
    private val accountManager: AccountManager,
    private val handleSyncIntentUseCase: HandleSyncIntentUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncManager {

    private val workerScope = CoroutineScope(dispatcher)

    private val _state = MutableStateFlow<SyncFeatureState>(SyncFeatureState.Loading)
    override val state: StateFlow<SyncFeatureState> = _state

    init {
        workerScope.launch { handleStateUpdates() }
    }

    private suspend fun handleStateUpdates() {
        syncFeatureStateFlow().collectLatest { syncFeatureState ->
            Logger.d("syncFeatureState[$syncFeatureState]")
            _state.value = syncFeatureState
        }
    }

    private fun syncFeatureStateFlow(): Flow<SyncFeatureState> {
        return accountManager.state.transformLatest { accountState ->
            when (accountState) {
                AccountState.LoggedOut,
                is AccountState.Error -> {
                    emit(SyncFeatureState.Disabled)
                }

                AccountState.Loading -> {
                    emit(SyncFeatureState.Loading)
                }

                is AccountState.LoggedIn -> when (accountState.subscriptionInfo) {
                    SubscriptionInfo.Inactive,
                    is SubscriptionInfo.Expired -> {
                        emit(SyncFeatureState.Disabled)
                    }

                    is SubscriptionInfo.Active -> coroutineScope {
                        val enabledState = SyncEnabledState(
                            coroutineScope = this,
                            handleSyncIntentUseCase = handleSyncIntentUseCase,
                            accountManager = accountManager,
                            initialStateIssue = accountState.issue
                        )
                        emit(enabledState)
                    }
                }
            }
        }
    }


}

internal class SyncEnabledState(
    private val coroutineScope: CoroutineScope,
    private val handleSyncIntentUseCase: HandleSyncIntentUseCase,
    private val accountManager: AccountManager,
    initialStateIssue: ApiRequestIssue?
) : SyncFeatureState.Enabled {

    private data class IntentData(
        val intent: SyncIntent,
        val result: CompletableDeferred<SyncState>? = null
    )

    private val intentJobChannel = Channel<IntentData>(
        capacity = Channel.UNLIMITED,
        onUndeliveredElement = { it.result?.cancel() }
    )

    private val _state = MutableStateFlow<SyncState>(
        value = initialStateIssue?.let { SyncState.Error.Api(it) } ?: SyncState.Refreshing
    )
    override val state: StateFlow<SyncState> = _state

    init {
        coroutineScope.coroutineContext[Job]?.invokeOnCompletion {
            intentJobChannel.cancel()
        }
        coroutineScope.launch {
            intentJobChannel.consumeAsFlow().collectLatest { request ->
                try {
                    // Trackers belong to this operation. A failure is caught
                    // here instead of cancelling the long-lived intent worker.
                    coroutineScope {
                        val last = handleSyncIntentUseCase(this, request.intent)
                            .onEach { syncState ->
                                _state.value = syncState
                                syncState.updateAppState()
                            }
                            .last()
                        request.result?.complete(last)
                        // Keep current change trackers alive until replaced.
                        awaitCancellation()
                    }
                } catch (error: CancellationException) {
                    request.result?.cancel(error)
                    throw error
                } catch (error: Throwable) {
                    val failed = SyncState.Error.Api(ApiRequestIssue.classify(error))
                    _state.value = failed
                    request.result?.complete(failed)
                    failed.updateAppState()
                }
            }
        }
        if (initialStateIssue == null) {
            intentJobChannel.trySend(IntentData(SyncIntent.Refresh))
        }
    }

    override fun sync(): CompletableDeferred<SyncState> {
        return handleIntent(SyncIntent.Sync)
    }

    override fun resolveConflict(strategy: SyncConflictResolveStrategy) {
        val conflict = _state.value as? SyncState.Conflict ?: return
        handleIntent(SyncIntent.ResolveConflict(strategy, conflict.remoteRevision))
    }

    override fun cancel() {
        handleIntent(SyncIntent.Cancel)
    }

    private fun handleIntent(intent: SyncIntent): CompletableDeferred<SyncState> {
        val completableDeferred = CompletableDeferred<SyncState>()
        if (intentJobChannel.trySend(IntentData(intent, completableDeferred)).isFailure) {
            completableDeferred.cancel()
        }
        return completableDeferred
    }

    private fun SyncState.updateAppState() {
        when (this) {
            is SyncState.Error.Api -> {
                when (issue) {
                    ApiRequestIssue.NotAuthenticated -> accountManager.invalidateAuth()
                    ApiRequestIssue.NoSubscription -> accountManager.invalidateSubscription()
                    else -> Unit
                }
            }

            else -> Unit
        }
    }

}
