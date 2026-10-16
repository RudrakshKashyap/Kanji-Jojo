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
| `info`            | Character detail view and personal notes     |
| `practice_common` | Shared practice/review logic                 |
| `practice_letter` | Letter reading/writing practice and personal notes |
| `practice_vocab`  | Vocabulary review (flashcards, readings)     |
| `sponsor`         | Donation / support                           |
| `sync`            | Cloud sync UI                                |
| `text_analysis`   | Analyze Japanese text for unknown characters |
| `vocab_card`      | Vocabulary card detail                       |

## Platform source sets

Character info and letter practice share `CharacterNoteCard`, state, and a
`CharacterNoteController` under `presentation/common/character_notes`. The card
shows readable text with an edit action; editing exposes a multiline input,
character counter, save status, and Done action. Character info places it below
the character heading, before radicals/vocabulary. Practice shows a reveal
control on the question screen; revealing notes does not reveal the answer.
Answer reveal also opens notes, and manual visibility resets for each attempt.

The shared controller caches notes across repeated cards/decks, debounces
autosave by 500 ms, and serializes writes. Repository change notifications
refresh clean notes in other open screens, including after database replacement;
pending local drafts are preserved. Advancing/finishing practice and navigating
away from character info flush pending edits; failed saves keep the screen and
offer retry. Failed loads disable editing until retried. Notes allow line breaks
and up to 500 Unicode code points.

`NoteFocusDismissalBox` observes pointer-down events at the initial pass without
consuming them, compares window coordinates with the focused editor bounds,
and clears focus/hides the keyboard for outside presses. Child clicks and text
selection retain their normal handling. Losing editor focus flushes pending
changes and returns to the reading layout. During note entry with a software
keyboard, the writing canvas is hidden to leave room for the field. Persistence
and snapshot behavior are documented in [the data layer](/architecture/data-layer.md).

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
