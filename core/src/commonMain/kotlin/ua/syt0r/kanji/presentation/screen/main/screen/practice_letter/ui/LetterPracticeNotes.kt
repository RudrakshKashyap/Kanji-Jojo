package ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.CharacterNoteMaxLength
import ua.syt0r.kanji.core.user_data.database.characterNoteLength
import ua.syt0r.kanji.practice_note_counter
import ua.syt0r.kanji.practice_note_load_error
import ua.syt0r.kanji.practice_note_loading
import ua.syt0r.kanji.practice_note_placeholder
import ua.syt0r.kanji.practice_note_retry
import ua.syt0r.kanji.practice_note_save_error
import ua.syt0r.kanji.practice_note_saved
import ua.syt0r.kanji.practice_note_saving
import ua.syt0r.kanji.practice_note_title
import ua.syt0r.kanji.presentation.screen.main.screen.practice_letter.data.LetterPracticeNoteState

@Composable
fun LetterPracticeNotes(
    state: LetterPracticeNoteState,
    onNoteChange: (String) -> Unit,
    onRetry: () -> Unit
) = key(state.character) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.text,
            onValueChange = onNoteChange,
            enabled = state.isLoaded,
            label = { Text(stringResource(Res.string.practice_note_title)) },
            placeholder = { Text(stringResource(Res.string.practice_note_placeholder)) },
            minLines = 2,
            maxLines = 5,
            isError = state.hasError,
            supportingText = {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(
                            when {
                                state.hasError && !state.isLoaded -> Res.string.practice_note_load_error
                                state.hasError -> Res.string.practice_note_save_error
                                state.isLoading -> Res.string.practice_note_loading
                                state.isSaving || state.text != state.savedText -> Res.string.practice_note_saving
                                else -> Res.string.practice_note_saved
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(
                            Res.string.practice_note_counter,
                            state.text.characterNoteLength(),
                            CharacterNoteMaxLength
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (state.hasError) {
            TextButton(onClick = onRetry, enabled = !state.isLoading && !state.isSaving) {
                Text(
                    text = stringResource(Res.string.practice_note_retry),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
