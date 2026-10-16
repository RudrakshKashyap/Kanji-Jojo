package ua.syt0r.kanji.presentation.common.character_notes

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.syt0r.kanji.core.user_data.database.CharacterNoteRepository
import ua.syt0r.kanji.core.user_data.database.limitCharacterNote

@OptIn(FlowPreview::class)
class CharacterNoteController(
    private val coroutineScope: CoroutineScope,
    private val characterNoteRepository: CharacterNoteRepository
) {
    private val notes = mutableStateMapOf<String, CharacterNoteState>()
    private val noteSaveMutex = Mutex()

    init {
        coroutineScope.launch {
            snapshotFlow { notes.values.map { it.text } }
                .debounce(500)
                .collect { savePendingNotes() }
        }
        coroutineScope.launch {
            characterNoteRepository.changesFlow.collect {
                notes.values.toList().forEach { refreshNote(it) }
            }
        }
    }

    suspend fun get(character: String): CharacterNoteState {
        return notes[character] ?: CharacterNoteState(character).also {
            loadNote(it)
            notes[character] = it
        }
    }

    fun update(character: String, text: String) {
        val note = notes[character] ?: return
        if (!note.isLoaded) return
        note.text = text.limitCharacterNote()
        note.hasError = false
    }

    fun retry(character: String) {
        val note = notes[character] ?: return
        if (note.isLoading || note.isSaving) return
        coroutineScope.launch {
            when {
                !note.isLoaded -> loadNote(note)
                note.text != note.savedText -> savePendingNotes()
                else -> refreshNote(note)
            }
        }
    }

    fun flush() {
        coroutineScope.launch { savePendingNotes() }
    }

    private suspend fun refreshNote(note: CharacterNoteState) {
        if (!note.isLoaded || note.isSaving || note.text != note.savedText) return
        val previousText = note.savedText
        try {
            val text = characterNoteRepository.get(note.character)
            if (!note.isSaving && note.savedText == previousText && note.text == previousText) {
                note.text = text
                note.savedText = text
                note.hasError = false
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            note.hasError = true
        }
    }

    private suspend fun loadNote(note: CharacterNoteState) {
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
    suspend fun savePendingNotes(): Boolean = noteSaveMutex.withLock {
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

}
