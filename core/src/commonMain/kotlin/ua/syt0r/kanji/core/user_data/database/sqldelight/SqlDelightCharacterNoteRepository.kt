package ua.syt0r.kanji.core.user_data.database.sqldelight

import ua.syt0r.kanji.core.user_data.database.CharacterNoteMaxLength
import ua.syt0r.kanji.core.user_data.database.CharacterNoteRepository
import ua.syt0r.kanji.core.user_data.database.ObservableUserDataRepository
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.user_data.database.characterNoteLength

class SqlDelightCharacterNoteRepository(
    databaseManager: UserDataDatabaseContract.Manager
) : ObservableUserDataRepository(databaseManager), CharacterNoteRepository {

    override suspend fun get(character: String): String = readTransaction {
        getCharacterNote(character).executeAsOneOrNull().orEmpty()
    }

    override suspend fun save(character: String, note: String) {
        require(note.characterNoteLength() <= CharacterNoteMaxLength)
        writeTransaction {
            if (note.isBlank()) deleteCharacterNote(character)
            else upsertCharacterNote(character, note)
        }
    }
}
