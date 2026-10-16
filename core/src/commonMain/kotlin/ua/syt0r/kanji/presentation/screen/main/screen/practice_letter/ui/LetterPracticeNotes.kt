package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.runtime.Composable
import ua.syt0r.kanji.presentation.common.character_notes.CharacterNoteCard
import ua.syt0r.kanji.presentation.common.character_notes.CharacterNoteState

@Composable
fun LetterPracticeNotes(
    state: CharacterNoteState,
    revealed: Boolean,
    onReveal: () -> Unit,
    onNoteChange: (String) -> Unit,
    onRetry: () -> Unit,
    onEditingFinished: () -> Unit
) {
    CharacterNoteCard(
        state = state,
        revealed = revealed,
        onReveal = onReveal,
        onNoteChange = onNoteChange,
        onRetry = onRetry,
        onEditingFinished = onEditingFinished
    )
}
