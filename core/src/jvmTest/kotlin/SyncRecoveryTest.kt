package ua.syt0r.kanji.core.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import ua.syt0r.kanji.core.AccountManager
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.sync.use_case.HandleSyncIntentUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyncRecoveryTest {
    private val account = object : AccountManager {
        override val state = MutableStateFlow<AccountState>(AccountState.LoggedOut)
        override val subscriptionExpirationEvents = MutableSharedFlow<Unit>()
        override fun signIn(refreshToken: String, idToken: String) = Unit
        override fun signInWithBrowser(openBrowser: (String) -> Unit, desktop: Boolean) = Unit
        override fun signOut() = Unit
        override fun refreshUserData() = Unit
        override fun invalidateAuth() = Unit
        override fun invalidateSubscription() = Unit
    }

    private fun enabled(
        scope: CoroutineScope,
        handler: suspend (CoroutineScope, SyncIntent) -> Flow<SyncState>
    ) = SyncEnabledState(scope, object : HandleSyncIntentUseCase {
        override suspend fun invoke(coroutineScope: CoroutineScope, intent: SyncIntent) =
            handler(coroutineScope, intent)
    }, account, null)

    @Test
    fun retriesAfterUnexpectedInitialRefreshFailure() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val sync = enabled(scope) { _, intent ->
                if (intent == SyncIntent.Refresh) error("local metadata failed")
                flowOf(SyncState.TrackingChanges.NoRemoteData)
            }
            withTimeout(2_000) { sync.state.first { it is SyncState.Error.Api } }
            assertTrue(scope.isActive)
            assertEquals(SyncState.TrackingChanges.NoRemoteData, withTimeout(2_000) { sync.sync().await() })
        } finally { scope.cancel() }
    }

    @Test
    fun retriesAfterAnUploadFlowThrowsAndCompletesEveryCaller() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        var attempts = 0
        try {
            val sync = enabled(scope) { _, intent ->
                flow {
                    if (intent == SyncIntent.Sync && ++attempts == 1) {
                        emit(SyncState.Uploading)
                        error("backup cleanup failed")
                    }
                    emit(SyncState.TrackingChanges.NoRemoteData)
                }
            }
            assertIs<SyncState.Error.Api>(withTimeout(2_000) { sync.sync().await() })
            assertTrue(scope.isActive)
            assertEquals(SyncState.TrackingChanges.NoRemoteData, withTimeout(2_000) { sync.sync().await() })
        } finally { scope.cancel() }
    }

    @Test
    fun cancellingAnUploadSettlesThePendingResultAndStillAllowsRetry() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val started = CompletableDeferred<Unit>()
        var attempts = 0
        try {
            val sync = enabled(scope) { _, intent ->
                flow {
                    when {
                        intent == SyncIntent.Cancel -> emit(SyncState.Canceled)
                        intent == SyncIntent.Sync && ++attempts == 1 -> {
                            emit(SyncState.Uploading)
                            started.complete(Unit)
                            awaitCancellation()
                        }
                        else -> emit(SyncState.TrackingChanges.NoRemoteData)
                    }
                }
            }
            val pending = sync.sync()
            withTimeout(2_000) { started.await() }
            sync.cancel()
            withTimeout(2_000) { sync.state.first { it == SyncState.Canceled } }
            assertTrue(pending.isCancelled)
            assertEquals(SyncState.TrackingChanges.NoRemoteData, withTimeout(2_000) { sync.sync().await() })
        } finally { scope.cancel() }
    }

    @Test
    fun keepsOnlyCurrentChangeTrackersAndSettlesRequestsAfterScopeEnds() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        var trackers = 0
        val sync = enabled(scope) { operation, _ ->
            operation.launch {
                trackers++
                try { awaitCancellation() } finally { trackers-- }
            }
            flowOf(SyncState.TrackingChanges.NoRemoteData)
        }
        try {
            repeat(3) {
                withTimeout(2_000) { sync.sync().await() }
                assertEquals(1, trackers)
            }
        } finally {
            scope.cancel()
            withTimeout(2_000) { scope.coroutineContext[Job]!!.join() }
        }
        assertEquals(0, trackers)
        assertTrue(sync.sync().isCancelled)
    }

    @Test
    fun aBackgroundTrackingFailureDoesNotKillTheWorker() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
        val fail = CompletableDeferred<Unit>()
        try {
            val sync = enabled(scope) { operation, intent ->
                if (intent == SyncIntent.Refresh) operation.launch {
                    fail.await()
                    error("tracking database failed")
                }
                flowOf(SyncState.TrackingChanges.NoRemoteData)
            }
            fail.complete(Unit)
            withTimeout(2_000) { sync.state.first { it is SyncState.Error.Api } }
            assertTrue(scope.isActive)
            assertEquals(SyncState.TrackingChanges.NoRemoteData, withTimeout(2_000) { sync.sync().await() })
        } finally { scope.cancel() }
    }
}
