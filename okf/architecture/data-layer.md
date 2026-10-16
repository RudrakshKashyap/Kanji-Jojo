---
type: Architecture
title: Data Layer
description: SqlDelight databases (AppData, UserData), DataStore preferences, and how data flows through the app.
resource: repo:core/src/commonMain
tags: [architecture, data, sqldelight, database]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# Data layer

## Databases (SqlDelight)

The app uses two separate SqlDelight databases, both configured in
`core/build.gradle.kts`:

| Database           | Package                          | Source dir                             | Purpose                  |
|--------------------|----------------------------------|----------------------------------------|--------------------------|
| `AppDataDatabase`  | `ua.syt0r.kanji.core.app_data.db`| `src/commonMain/sqldelight_app_data`   | Static reference data (kanji, readings, meanings, stroke data, JLPT/grade classifications) |
| `UserDataDatabase` | `ua.syt0r.kanji.core.user_data.db`| `src/commonMain/sqldelight_user_data` | User progress, SRS state, custom decks, study history |

**AppData** is shipped as a pre-built asset (the filename is configured via
BuildKonfig's `appDataAssetName`). It is read-only at runtime.

**UserData** is created/migrated on device. Migrations live in
`sqldelight_user_data/migrations/`.

Schema version 13 adds `character_note`, keyed by character rather than deck or
practice type. Its optional plain-text note is shared by reading/writing practice
and every deck containing that character. Migration `12.sqm` preserves existing
study data. Notes are limited to 500 Unicode code points in the repository and
UI, with a matching SQLite length constraint; blank notes delete the row.

`CharacterNoteRepository` uses the database manager's write transactions, which
update `localDataTimestamp`. Notes therefore participate in existing backup and
snapshot sync without a separate endpoint. Whole-snapshot conflict handling
applies to notes too; they are not merged independently between devices.
The note repository also exposes the standard user-data change notifications
for note writes and database replacement. Shared note controllers refresh clean
local state from those notifications so edits on character info pages appear in
an already-open practice screen without overwriting a pending draft.

### Platform drivers

| Platform | Driver                        |
|----------|-------------------------------|
| Android  | `sqldelight-android-driver`   |
| JVM      | `sqldelight-jvm-sqlite-driver`|
| iOS      | `sqldelight-native-sqlite-driver` |

## Preferences (DataStore)

User preferences (settings, theme, daily limits, etc.) use
`datastore-preferences-core` (common) and `datastore-preferences` (Android).

## Data access pattern

Business logic code in `core/` accesses data through repository/use-case layers
in `ua.syt0r.kanji.core`:

```
core/
  app_data/       # Read-only reference data access
    data/         # Data classes and query wrappers
  user_data/      # Mutable user progress
    database/     # SqlDelight wrapper
    preferences/  # DataStore wrapper
  srs/            # SRS scheduling (reads user_data, writes review results)
    fsrs/         # FSRS algorithm implementation
    use_case/     # SRS-related use cases
  backup/         # Import/export user data
  sync/           # Cloud sync via Ktor
    use_case/
```

## Backup and sync

- **Backup:** The `backup` package handles import/export of user data (database
  file operations).
- **Sync:** The `sync` package uses Ktor HTTP client for cloud synchronization.
  Authentication is handled via `ktor-client-auth`.

Owned-service sync is a private versioned ZIP snapshot protocol with explicit
revision preconditions and checksum checks. Auth/cache preferences use new
`jojo_v1_*` keys so legacy credentials are not reused. Local study data remains
unchanged. See [accounts and sync](/features/accounts-and-sync.md).
