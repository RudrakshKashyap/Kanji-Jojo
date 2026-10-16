package ua.syt0r.kanji.presentation.common.character_notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ua.syt0r.kanji.Res
import ua.syt0r.kanji.core.user_data.database.CharacterNoteMaxLength
import ua.syt0r.kanji.core.user_data.database.characterNoteLength
import ua.syt0r.kanji.practice_note_counter
import ua.syt0r.kanji.practice_note_done
import ua.syt0r.kanji.practice_note_edit
import ua.syt0r.kanji.practice_note_load_error
import ua.syt0r.kanji.practice_note_loading
import ua.syt0r.kanji.practice_note_placeholder
import ua.syt0r.kanji.practice_note_reveal
import ua.syt0r.kanji.practice_note_retry
import ua.syt0r.kanji.practice_note_save_error
import ua.syt0r.kanji.practice_note_saved
import ua.syt0r.kanji.practice_note_saving
import ua.syt0r.kanji.practice_note_title

@Composable
fun CharacterNoteCard(
    state: CharacterNoteState,
    onNoteChange: (String) -> Unit,
    onRetry: () -> Unit,
    onEditingFinished: () -> Unit,
    modifier: Modifier = Modifier,
    revealed: Boolean = true,
    onReveal: () -> Unit = {}
) {
    var editing by remember(state.character) { mutableStateOf(false) }
    var focused by remember(state.character) { mutableStateOf(false) }
    var editorBounds by remember(state.character) { mutableStateOf<Rect?>(null) }
    val focusBounds = LocalNoteFocusBounds.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    DisposableEffect(state.character) {
        onDispose {
            if (focusBounds?.editor == editorBounds) focusBounds?.editor = null
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(32.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    stringResource(Res.string.practice_note_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                when {
                    !revealed -> TextButton(onClick = onReveal) {
                        Icon(Icons.Outlined.Visibility, null, Modifier.size(16.dp))
                        Text(
                            stringResource(Res.string.practice_note_reveal),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    editing -> TextButton(onClick = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }) {
                        Text(stringResource(Res.string.practice_note_done))
                    }
                    else -> IconButton(onClick = { editing = true }, enabled = state.isLoaded) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(Res.string.practice_note_edit),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (revealed) {
                if (editing) {
                    BasicTextField(
                        value = state.text,
                        onValueChange = onNoteChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .focusRequester(focusRequester)
                            .onGloballyPositioned {
                                editorBounds = it.boundsInWindow()
                                if (focused) focusBounds?.editor = editorBounds
                            }
                            .onFocusChanged {
                                val wasFocused = focused
                                focused = it.isFocused
                                if (focused) {
                                    focusBounds?.editor = editorBounds
                                } else if (wasFocused) {
                                    focusBounds?.editor = null
                                    editing = false
                                    onEditingFinished()
                                }
                            },
                        decorationBox = { input ->
                            Box {
                                if (state.text.isEmpty()) {
                                    Text(
                                        stringResource(Res.string.practice_note_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                input()
                            }
                        }
                    )
                } else if (state.isLoaded) {
                    Text(
                        text = state.text.ifBlank { stringResource(Res.string.practice_note_placeholder) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.text.isBlank()) colors.onSurfaceVariant.copy(alpha = 0.7f)
                            else colors.onSurface,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { editing = true }
                            .padding(top = 8.dp, bottom = 16.dp)
                    )
                }

                if (editing || state.isLoading || state.isSaving || state.hasError || state.text != state.savedText) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(when {
                                state.hasError && !state.isLoaded -> Res.string.practice_note_load_error
                                state.hasError -> Res.string.practice_note_save_error
                                state.isLoading -> Res.string.practice_note_loading
                                state.isSaving || state.text != state.savedText -> Res.string.practice_note_saving
                                else -> Res.string.practice_note_saved
                            }),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.hasError) colors.error else colors.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.hasError) {
                            TextButton(onClick = onRetry, enabled = !state.isLoading && !state.isSaving) {
                                Text(stringResource(Res.string.practice_note_retry))
                            }
                        }
                        if (editing) {
                            Text(
                                stringResource(Res.string.practice_note_counter,
                                    state.text.characterNoteLength(), CharacterNoteMaxLength),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
