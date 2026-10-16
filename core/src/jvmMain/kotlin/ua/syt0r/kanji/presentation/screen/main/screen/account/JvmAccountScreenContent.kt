package ua.syt0r.kanji.presentation.screen.main.screen.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ua.syt0r.kanji.core.AccountManager
import ua.syt0r.kanji.core.AccountState
import ua.syt0r.kanji.core.ApiRequestIssue
import ua.syt0r.kanji.core.SubscriptionInfo
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.account.JvmAccountScreenContract.ScreenState

object JvmAccountScreenContent : AccountScreenContract.Content {

    @Composable
    override fun invoke(
        state: MainNavigationState,
        data: AccountScreenContract.ScreenData?
    ) {

        val viewModel = getMultiplatformViewModel<JvmAccountScreenContract.ViewModel>()
        val uriHandler = LocalUriHandler.current


        AccountScreenUI(
            state = viewModel.state.collectAsState(),
            onUpClick = { state.navigateBack() },
            signIn = { viewModel.signIn(uriHandler::openUri) },
            signOut = { viewModel.signOut() },
            refresh = { viewModel.refresh() }
        )
    }

}

interface JvmAccountScreenContract {

    interface ViewModel {
        val state: StateFlow<ScreenState>
        fun signIn(openBrowser: (String) -> Unit)
        fun signOut()
        fun refresh()
    }

    sealed interface ScreenState {
        object SignedOut : ScreenState
        object LoadingUserData : ScreenState

        data class Loaded(
            val email: String,
            val subscriptionInfo: SubscriptionInfo,
            val issue: ApiRequestIssue?
        ) : ScreenState

        data class Error(
            val issue: ApiRequestIssue
        ) : ScreenState

    }

}

@Composable
fun AccountScreenUI(
    state: State<ScreenState>,
    onUpClick: () -> Unit,
    signIn: () -> Unit,
    signOut: () -> Unit,
    refresh: () -> Unit
) {

    AccountScreenContainer(
        state = state,
        onUpClick = onUpClick
    ) { screenState ->

        when (screenState) {
            ScreenState.SignedOut -> {
                AccountScreenSignedOut(
                    startSignIn = signIn
                )
            }

            ScreenState.LoadingUserData -> {
                AccountScreenLoading()
            }

            is ScreenState.Loaded -> {
                AccountScreenSignedIn(
                    email = screenState.email,
                    subscriptionInfo = screenState.subscriptionInfo,
                    issue = screenState.issue,
                    refresh = refresh,
                    signOut = signOut,
                    signIn = signIn
                )
            }

            is ScreenState.Error -> {
                AccountScreenError(
                    issue = screenState.issue,
                    startSignIn = signIn
                )
            }
        }

    }

}

class JvmAccountScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val accountManager: AccountManager,
) : JvmAccountScreenContract.ViewModel {

    private val _state = MutableStateFlow<ScreenState>(ScreenState.LoadingUserData)
    override val state: StateFlow<ScreenState> = _state

    init {

        accountManager.state
            .onEach {
                _state.value = when (it) {
                    AccountState.Loading -> ScreenState.LoadingUserData
                    AccountState.LoggedOut -> ScreenState.SignedOut
                    is AccountState.LoggedIn -> ScreenState.Loaded(
                        email = it.email,
                        subscriptionInfo = it.subscriptionInfo,
                        issue = it.issue
                    )

                    is AccountState.Error -> ScreenState.Error(it.issue)
                }
            }
            .launchIn(coroutineScope)

    }

    override fun signIn(openBrowser: (String) -> Unit) {
        accountManager.signInWithBrowser(openBrowser, desktop = true)
    }

    override fun signOut() {
        accountManager.signOut()
    }

    override fun refresh() {
        accountManager.refreshUserData()
    }

}
