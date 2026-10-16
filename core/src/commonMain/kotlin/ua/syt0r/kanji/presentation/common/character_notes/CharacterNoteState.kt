package ua.syt0r.kanji.presentation.common.character_notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CharacterNoteState(val character: String) {
    var text by mutableStateOf("")
        internal set
    var isLoaded by mutableStateOf(false)
        internal set
    var isLoading by mutableStateOf(true)
        internal set
    var isSaving by mutableStateOf(false)
        internal set
    var hasError by mutableStateOf(false)
        internal set
    internal var savedText by mutableStateOf<String?>(null)
}
