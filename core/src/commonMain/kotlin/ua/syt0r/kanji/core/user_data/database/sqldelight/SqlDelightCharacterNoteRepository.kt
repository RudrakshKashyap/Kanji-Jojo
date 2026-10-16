package ua.syt0r.kanji.core.user_data.database.sqldelight

import ua.syt0r.kanji.core.user_data.database.CharacterNoteMaxLength
import ua.syt0r.kanji.core.user_data.database.CharacterNoteRepository
import ua.syt0r.kanji.core.user_data.database.UserDataDatabaseContract
import ua.syt0r.kanji.core.user_data.database.characterNoteLength

class SqlDelightCharacterNoteRepository(
    private val databaseManager: UserDataDatabaseContract.Manager
) : CharacterNoteRepository {

    override suspend fun get(character: String): String = databaseManager.readTransaction {
        getCharacterNote(character).executeAsOneOrNull().orEmpty()
    }

    override suspend fun save(character: String, note: String) {
        require(note.characterNoteLength() <= CharacterNoteMaxLength)
        databaseManager.writeTransaction {
            if (note.isBlank()) deleteCharacterNote(character)
            else upsertCharacterNote(character, note)
        }
    }
}
