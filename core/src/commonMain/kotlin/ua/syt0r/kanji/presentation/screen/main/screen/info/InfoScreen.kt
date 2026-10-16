package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import ua.syt0r.kanji.presentation.common.MultiplatformBackHandler
import ua.syt0r.kanji.presentation.getMultiplatformViewModel
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState


@Composable
fun InfoScreen(
    screenData: InfoScreenData,
    mainNavigationState: MainNavigationState,
    viewModel: InfoScreenContract.ViewModel = getMultiplatformViewModel(screenData)
) {

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val navigate: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus()
        keyboard?.hide()
        viewModel.saveNotesAndThen(action)
    }
    val navigateBack: () -> Unit = { navigate { mainNavigationState.navigateBack() } }
    MultiplatformBackHandler(navigateBack)

    InfoScreenUI(
        state = viewModel.state,
        note = viewModel.note.value,
        onNoteChange = { viewModel.updateNote(it) },
        onNoteRetry = { viewModel.retryNote() },
        onNoteEditingFinished = { viewModel.flushNotes() },
        onUpButtonClick = navigateBack,
        onLetterClick = {
            val nextScreenData = InfoScreenData.Letter(it)
            if (screenData != nextScreenData)
                navigate { mainNavigationState.navigate(MainDestination.Info(nextScreenData)) }
        },
        onWordClick = {
            val nextScreenData = it.toInfoScreenData()
            if (screenData != nextScreenData)
                navigate { mainNavigationState.navigate(MainDestination.Info(nextScreenData)) }
        }
    )

}
