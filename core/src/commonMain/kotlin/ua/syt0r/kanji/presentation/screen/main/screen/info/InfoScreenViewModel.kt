package ua.syt0r.kanji.presentation.screen.main.screen.info

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.isKanji
import ua.syt0r.kanji.core.user_data.database.CharacterNoteRepository
import ua.syt0r.kanji.presentation.common.character_notes.CharacterNoteController
import ua.syt0r.kanji.presentation.common.character_notes.CharacterNoteState
import ua.syt0r.kanji.presentation.screen.main.screen.info.InfoScreenContract.ScreenState

class InfoScreenViewModel(
    private val viewModelScope: CoroutineScope,
    screenData: InfoScreenData,
    private val loadLetterStateUseCase: InfoScreenContract.LoadLetterStateUseCase,
    private val loadVocabStateUseCase: InfoScreenContract.LoadVocabStateUseCase,
    private val analyticsManager: AnalyticsManager,
    characterNoteRepository: CharacterNoteRepository
) : InfoScreenContract.ViewModel {

    override val state = mutableStateOf<ScreenState>(ScreenState.Loading)
    override val note = mutableStateOf<CharacterNoteState?>(null)
    private val notes = CharacterNoteController(viewModelScope, characterNoteRepository)
    private var navigating = false

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { screenData.toState() }
            if (loaded is ScreenState.Loaded.Letter) {
                note.value = notes.get(loaded.data.character)
            }
            state.value = loaded
        }
    }

    override fun updateNote(text: String) {
        if (!navigating) note.value?.let { notes.update(it.character, text) }
    }

    override fun retryNote() {
        note.value?.let { notes.retry(it.character) }
    }

    override fun flushNotes() = notes.flush()

    override fun saveNotesAndThen(action: () -> Unit) {
        if (navigating) return
        navigating = true
        viewModelScope.launch {
            try {
                if (notes.savePendingNotes()) action()
            } finally {
                navigating = false
            }
        }
    }

    private suspend fun InfoScreenData.toState(): ScreenState {
        return when (this) {
            is InfoScreenData.Letter -> {
                if (letter.length == 1) {
                    reportLetter(letter)
                    loadLetterStateUseCase(letter, viewModelScope)
                } else {
                    loadVocabStateUseCase(asVocabData(), viewModelScope)
                }
            }

            is InfoScreenData.Vocab -> {
                loadVocabStateUseCase(this, viewModelScope)
            }
        }
    }

    private fun InfoScreenData.Letter.asVocabData(): InfoScreenData.Vocab {
        val kanjiReading: String?
        val kanaReading: String?

        when {
            letter.any { it.isKanji() } -> {
                kanjiReading = letter
                kanaReading = null
            }

            else -> {
                kanjiReading = null
                kanaReading = letter
            }
        }

        return InfoScreenData.Vocab(
            id = null,
            kanjiReading = kanjiReading,
            kanaReading = kanaReading
        )
    }

    private fun reportLetter(letter: String) {
        analyticsManager.sendEvent("kanji_info_open") { put("character", letter) }
    }

}
