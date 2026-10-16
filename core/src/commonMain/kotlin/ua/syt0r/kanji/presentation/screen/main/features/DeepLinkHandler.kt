package ua.syt0r.kanji.presentation.screen.main.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import ua.syt0r.kanji.core.launchWhenHasSubscribers
import ua.syt0r.kanji.core.logger.Logger
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.account.AccountScreenContract

class DeepLinkHandler(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
) {

    private val _deepLinksFlow = MutableSharedFlow<String>()
    val deepLinksFlow: SharedFlow<String> = _deepLinksFlow

    fun notifyDeepLink(link: String) {
        _deepLinksFlow.launchWhenHasSubscribers(coroutineScope) { _deepLinksFlow.emit(link) }
    }

    @Composable
    fun HandleDeepLinksLaunchedEffect(navigationState: MainNavigationState) {
        LaunchedEffect(Unit) {
            deepLinksFlow.collectLatest {
                val destination = when {
                    it.startsWith(AccountScreenContract.DEEP_LINK_AUTH_REDIRECT_URL) -> {
                        val data = AccountScreenContract.parseDeepLink(it)
                        if (data != null) {
                            MainDestination.Account(data)
                        } else {
                            Logger.d("Unrecognized app return link")
                            null
                        }
                    }

                    else -> {
                        Logger.d("Unsupported deep link")
                        null
                    }
                }
                Logger.d("Handling app return navigation")
                destination?.let { navigationState.navigateToTop(it) }
            }
        }
    }

}
