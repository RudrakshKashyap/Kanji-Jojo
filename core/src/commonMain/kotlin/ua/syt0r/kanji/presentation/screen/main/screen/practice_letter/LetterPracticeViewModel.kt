package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.syt0r.kanji.core.analytics.AnalyticsManager
import ua.syt0r.kanji.core.japanese.KanaReading
import ua.syt0r.kanji.core.tts.KanaTtsManager
import ua.syt0r.kanji.core.user_data.database.CharacterNoteRepository
import ua.syt0r.kanji.core.user_data.database.limitCharacterNote
import ua.syt0r.kanji.presentation.screen.main.screen.practice_common.PracticeAnswer
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.LetterPracticeScreenContract.ScreenState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeItemData
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeNoteState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeQueueState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeReviewState
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeScreenConfiguration
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeSummaryItem
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.use_case.GetLetterPracticeConfigurationUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.use_case.GetLetterPracticeQueueDataUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.use_case.GetLetterPracticeReviewStateUseCase
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.use_case.UpdateLetterPracticeConfigurationUseCase


@OptIn(FlowPreview::class)
class LetterPracticeViewModel(
    private val viewModelScope: CoroutineScope,
    private val getConfigurationUseCase: GetLetterPracticeConfigurationUseCase,
    private val updateConfigurationUseCase: UpdateLetterPracticeConfigurationUseCase,
    private val getQueueDataUseCase: GetLetterPracticeQueueDataUseCase,
    private val practiceQueue: LetterPracticeQueue,
    private val getReviewStateUseCase: GetLetterPracticeReviewStateUseCase,
    private val analyticsManager: AnalyticsManager,
    private val kanaTtsManager: KanaTtsManager,
    private val characterNoteRepository: CharacterNoteRepository
) : LetterPracticeScreenContract.ViewModel {

    private lateinit var configuration: LetterPracticeScreenConfiguration

    private val _state = mutableStateOf<ScreenState>(ScreenState.Loading)
    override val state: State<ScreenState> = _state

    private val notes = mutableStateMapOf<String, LetterPracticeNoteState>()
    private val noteSaveMutex = Mutex()
    private var advancing = false

    init {
        viewModelScope.launch {
            snapshotFlow { notes.values.map { it.text } }
                .debounce(500)
                .collect { savePendingNotes() }
        }
    }

    override fun initialize(configuration: LetterPracticeScreenConfiguration) {
        if (this::configuration.isInitialized) return
        this.configuration = configuration

        viewModelScope.launch {
            _state.value = ScreenState.Configuring(
                configuration = getConfigurationUseCase(configuration)
            )
        }
    }

    override fun configure() {
        val configurationState = _state.value as ScreenState.Configuring
        _state.value = ScreenState.Loading

        viewModelScope.launch {

            updateConfigurationUseCase(configurationState.configuration)

            practiceQueue.initialize(
                items = getQueueDataUseCase(configurationState.configuration)
            )

            practiceQueue.state
                .onEach {
                    when (it) {
                        LetterPracticeQueueState.Loading -> {
                            _state.value = ScreenState.Loading
                        }

                        is LetterPracticeQueueState.Review -> {
                            val reviewState = it.toScreenState()
                            _state.value = reviewState

                            reviewState.kanaAutoReadFlow()
                                .onEach { speakKana(it) }
                                .launchIn(viewModelScope)
                        }

                        is LetterPracticeQueueState.Summary -> {
                            _state.value = it.toScreenState()
                        }
                    }
                }
                .launchIn(this)
        }

        reportConfiguration(configurationState)
    }

    override fun submitAnswer(answer: PracticeAnswer) {
        if (advancing) return
        advancing = true
        viewModelScope.launch {
            try {
                if (savePendingNotes()) practiceQueue.submitAnswer(answer)
            } finally {
                advancing = false
            }
        }
    }

    override fun speakKana(reading: KanaReading) {
        viewModelScope.launch { kanaTtsManager.speak(reading) }
    }

    override fun finishPractice() {
        if (advancing) return
        advancing = true
        viewModelScope.launch {
            try {
                if (savePendingNotes()) practiceQueue.immediateFinish()
            } finally {
                advancing = false
            }
        }
    }

    override fun updateNote(character: String, text: String) {
        val note = notes[character] ?: return
        if (!note.isLoaded || advancing) return
        note.text = text.limitCharacterNote()
        note.hasError = false
    }

    override fun retryNote(character: String) {
        val note = notes[character] ?: return
        if (note.isLoading || note.isSaving) return
        viewModelScope.launch {
            if (!note.isLoaded) loadNote(note)
            else savePendingNotes()
        }
    }

    private suspend fun loadNote(note: LetterPracticeNoteState) {
        note.isLoading = true
        note.hasError = false
        try {
            val text = characterNoteRepository.get(note.character)
            note.text = text
            note.savedText = text
            note.isLoaded = true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            note.hasError = true
        } finally {
            note.isLoading = false
        }
    }

    // Sequential saves preserve edit order. Advancing also flushes the debounce window.
    private suspend fun savePendingNotes(): Boolean = noteSaveMutex.withLock {
        var success = true
        notes.values.toList().forEach { note ->
            if (!note.isLoaded || note.text == note.savedText) return@forEach
            note.isSaving = true
            try {
                while (note.text != note.savedText) {
                    val text = note.text
                    characterNoteRepository.save(note.character, text)
                    note.savedText = text
                }
                note.hasError = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // A write may have committed before timestamp tracking failed.
                // Retry the current draft even if the user reverted their edit.
                note.savedText = null
                note.hasError = true
                success = false
            } finally {
                note.isSaving = false
            }
        }
        success
    }

    private fun reportConfiguration(state: ScreenState.Configuring) {
        analyticsManager.sendEvent("letter_practice_configuration") {
            put("practice_type", configuration.practiceType.dataType.srsPracticeType.value)
            put("list_size", state.configuration.selectorState.selectedCountIntState.value)
        }
    }

    private suspend fun LetterPracticeQueueState.Review.toScreenState(): ScreenState.Review {
        val character = data.character
        val note = notes[character] ?: LetterPracticeNoteState(character).also {
            loadNote(it)
            notes[character] = it
        }
        return ScreenState.Review(
            practiceProgress = progress,
            reviewState = getReviewStateUseCase(this),
            note = note
        )
    }

    private fun LetterPracticeQueueState.Summary.toScreenState(): ScreenState.Summary {
        val accuracy: Float? = items.filterIsInstance<LetterPracticeSummaryItem.Writing>()
            .takeIf { it.isNotEmpty() }
            ?.let {
                val totalStrokeCount = it.fold(0) { sum, item -> sum + item.strokeCount }
                val totalMistakeCount = it.fold(0) { sum, item -> sum + item.mistakes }
                val correctStrokes = (totalStrokeCount - totalMistakeCount)
                    .coerceAtLeast(0)
                correctStrokes.toFloat() * 100 / totalStrokeCount
            }
        return ScreenState.Summary(
            duration = duration,
            accuracy = accuracy,
            items = items
        )
    }

    private fun ScreenState.Review.kanaAutoReadFlow(): Flow<KanaReading> = callbackFlow {
        when {
            reviewState is LetterPracticeReviewState.Reading &&
                    reviewState.itemData is LetterPracticeItemData.KanaReadingData -> {

                snapshotFlow { reviewState.revealed.value }
                    .filter { it && reviewState.layout.kanaAutoPlay.value }
                    .take(1)
                    .onEach { send(reviewState.itemData.reading) }
                    .collect()

            }

            reviewState is LetterPracticeReviewState.Writing &&
                    reviewState.itemData is LetterPracticeItemData.KanaWritingData -> {

                // Plays when writer state is switched (study/review)
                snapshotFlow { reviewState.writerState.value }
                    .filter { reviewState.layout.kanaAutoPlay.value }
                    .onEach {
                        delay(200)
                        send(reviewState.itemData.reading)
                    }
                    .collect()

            }
        }
        awaitClose()
    }

}
