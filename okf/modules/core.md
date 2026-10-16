---
type: Module
title: "core"
description: The shared KMP library containing all business logic, Compose Multiplatform UI, databases, DI modules, and platform-abstracted services.
resource: repo:core
tags: [module, core, kmp, shared]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# core

## Responsibility

The **heart of the application**. This Kotlin Multiplatform library contains
everything that is shared across Android, iOS, and Desktop:

- All Compose Multiplatform UI (screens, components, theming)
- Business logic and domain models
- SqlDelight database definitions and access (AppData + UserData)
- DataStore preferences
- SRS/FSRS scheduling engine
- Koin DI module definitions
- Ktor-based sync client
- Compose resources (drawables, strings, localized values)
- BuildKonfig-generated constants

## Package structure

All code lives under `ua.syt0r.kanji` (inherited from Kanji-Dojo):

```
ua.syt0r.kanji/
├── core/                   # Business logic layer
│   ├── analytics/          # Analytics abstraction
│   ├── app_data/           # Read-only reference data (kanji dictionary)
│   │   └── data/           # Data classes
│   ├── backup/             # Import/export user data
│   ├── feedback/           # User feedback submission
│   ├── file/               # File system abstraction
│   ├── japanese/           # Japanese language utilities
│   ├── logger/             # Logging abstraction
│   ├── srs/                # Spaced repetition system
│   │   ├── fsrs/           # FSRS algorithm
│   │   └── use_case/       # SRS use cases
│   ├── stroke_evaluator/   # Handwriting stroke evaluation
│   ├── suspended_property/ # Async property utilities
│   ├── svg/                # SVG parsing (for stroke data)
│   ├── sync/               # Cloud sync
│   │   └── use_case/
│   ├── theme_manager/      # Theme management
│   ├── time/               # Time utilities
│   ├── tts/                # Text-to-speech
│   └── user_data/          # User progress persistence
│       ├── database/       # SqlDelight wrapper
│       └── preferences/    # DataStore wrapper
├── di/                     # Koin DI modules
└── presentation/           # UI layer (Compose Multiplatform)
    ├── common/             # Shared UI utilities
    │   ├── resources/      # Resource helpers
    │   ├── theme/          # Material 3 theming
    │   └── ui/             # Common composables
    ├── dialog/             # Shared dialogs
    └── screen/             # Feature screens
        └── main/
            ├── features/   # Feature module definitions
            └── screen/     # Individual screens (see below)
```

## Screens

Each screen has its own package under `presentation/screen/main/screen/`:

| Screen            | Purpose                                      |
|-------------------|----------------------------------------------|
| `about`           | About the app                                |
| `account`         | User account / sync settings                 |
| `backup`          | Data import/export                           |
| `credits`         | Open source licenses (aboutlibraries)        |
| `daily_limit`     | Configure daily review limits                |
| `deck_details`    | View/manage a study deck                     |
| `deck_edit`       | Create/edit a custom deck                    |
| `deck_picker`     | Browse and select decks (JLPT, grade, custom)|
| `feedback`        | Submit feedback                              |
| `home`            | Main dashboard (decks, progress overview)    |
| `info`            | Character detail view                        |
| `practice_common` | Shared practice/review logic                 |
| `practice_letter` | Letter writing practice (stroke input)       |
| `practice_vocab`  | Vocabulary review (flashcards, readings)     |
| `sponsor`         | Donation / support                           |
| `sync`            | Cloud sync UI                                |
| `text_analysis`   | Analyze Japanese text for unknown characters |
| `vocab_card`      | Vocabulary card detail                       |

## Platform source sets

Account and sync protocol ownership, browser approval, preference migration,
and unsupported legacy services are documented in
[owned accounts and snapshot sync](/features/accounts-and-sync.md).
The enabled sync worker isolates operation/tracker failures and settles cancelled
requests, preserving retries. Browser sign-in retries transient polling failures
within a fixed deadline; network calls have bounded timeouts. Shared
`InvertedButton` accepts an enabled state so in-flight sync cannot be repeatedly
interrupted by taps.
Account credential changes explicitly invalidate the network bearer cache before
loading the new profile; asynchronous preference observers alone are insufficient.

See [Multiplatform overview](/architecture/multiplatform.md) for how source sets
map to targets.

## Compose resources

Resources are generated with `packageOfResClass = "ua.syt0r.kanji"` and
`publicResClass = true`, so all platform apps can access them.

## Build plugins

- **BuildKonfig:** Generates `BuildConfig` with version info and asset
  filenames, with per-platform overrides for audio format (opus vs. wav).
- **SqlDelight:** Generates type-safe database code from `.sq` files.
- **Compose Resources:** Generates resource accessors for drawables, strings,
  and files.
