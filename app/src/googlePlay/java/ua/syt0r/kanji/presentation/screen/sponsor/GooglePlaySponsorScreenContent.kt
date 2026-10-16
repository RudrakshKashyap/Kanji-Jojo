package ua.syt0r.kanji.presentation.screen.sponsor

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import ua.syt0r.kanji.presentation.common.asActivity
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorScreenContract
import ua.syt0r.kanji.presentation.screen.main.screen.sponsor.SponsorScreenUI

object GooglePlaySponsorScreenContent : SponsorScreenContract.Content {

    @Composable
    override fun invoke(
        state: MainNavigationState
    ) {

        SponsorScreenUI(onUpClick = { state.navigateBack() }) {
            Text("In-app purchases are not available on the new Kanji Jojo service yet.")
        }

    }

}
