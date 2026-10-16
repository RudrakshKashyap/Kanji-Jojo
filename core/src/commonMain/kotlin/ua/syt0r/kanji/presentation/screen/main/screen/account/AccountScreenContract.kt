package ua.syt0r.kanji.presentation.screen.main.screen.account

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

interface AccountScreenContract {

    companion object {

        const val ACCOUNT_WEB_PAGE_URL = "https://kanji-jojo.com/account"
        const val ACCOUNT_DELETE_URL = "$ACCOUNT_WEB_PAGE_URL?delete=true"
        const val DEEP_LINK_AUTH_REDIRECT_URL = "kanji-jojo://signin"

        // The redirect only resumes the UI. Authentication is exchanged by
        // the pending app process using its private proof over HTTPS.
        fun parseDeepLink(url: String): ScreenData? = if (
            url == DEEP_LINK_AUTH_REDIRECT_URL || url == "$DEEP_LINK_AUTH_REDIRECT_URL/"
        ) ScreenData() else null
    }

    @Serializable
    data class ScreenData(val browserReturn: Boolean = true)

    interface Content {

        @Composable
        operator fun invoke(
            state: MainNavigationState,
            data: ScreenData?
        )

    }

}