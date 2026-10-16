package ua.syt0r.kanji.core.user_data.database

const val CharacterNoteMaxLength = 500

interface CharacterNoteRepository : ObservableRepository {
    suspend fun get(character: String): String
    suspend fun save(character: String, note: String)
}

// Count Unicode code points so supplementary kanji and emoji count as one character.
fun String.characterNoteLength(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        index += if (this[index].isHighSurrogate() &&
            index + 1 < length && this[index + 1].isLowSurrogate()
        ) 2 else 1
        count++
    }
    return count
}

fun String.limitCharacterNote(): String {
    var count = 0
    var index = 0
    while (index < length && count < CharacterNoteMaxLength) {
        index += if (this[index].isHighSurrogate() &&
            index + 1 < length && this[index + 1].isLowSurrogate()
        ) 2 else 1
        count++
    }
    return take(index)
}
