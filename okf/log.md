# Bundle Update Log

Bundle-wide and cross-cutting changes. Newest first.

## 2026-10-16

- Added personal notes to character info pages and replaced the practice input
  with a shared notes card offering reading/editing layouts, reveal, and Done.
  Shared note controllers observe repository changes across screens, preserve
  pending drafts, and flush edits on focus loss or navigation. Outside presses
  dismiss editor focus and the keyboard without consuming other controls' taps.
  Verified `:app:assembleGooglePlayDebug`; visual/touch behavior still needs a
  device check.
- Made personal notes available behind a variants-style reveal control on the
  question screen. Opening notes does not reveal the answer; visibility resets
  for each attempt, and answer reveal still opens notes automatically. Kept the
  notes field available with the software keyboard in both writing layouts.
  Builds and tests remain deferred at the user's request.
- Added editable personal notes after reveal in reading/writing practice, with
  a 500-code-point limit, autosave, retry, and a flush before advancing/finishing.
  Schema v13 stores notes per character across decks and includes them in the
  existing backup/snapshot sync path. Updated core and data-layer knowledge.
  Builds and tests were deferred at the user's request.
- Isolated sync operation and tracker errors so later requests remain usable;
  settled interrupted results, removed dropped intents, and disabled overlapping
  sync taps. Added bounded network timeouts, transient app-sign-in polling
  retries, and bearer-cache invalidation for either changed credential. Added
  JVM regression coverage for recovery, cancellation, trackers, and polling.
  Account changes clear cached bearer credentials before profile requests; home
  sync errors open the detailed controls instead of silently retrying.
  The companion website now approves and attempts mobile return with one click.
- Corrected debug APK signing to use the Android debug key even when a release
  keystore exists, so local builds do not require release signing credentials.
- Migrated active website/account/sync requests to the owned Kanji Jojo origin
  and Firebase project. Added browser approval with a private PKCE verifier,
  one-time HTTPS token exchange, and polling across Android/iOS/Desktop. Removed
  credential-bearing redirects and the desktop callback server.
- Moved credentials/account/sync caches to new preference keys, preserving local
  study data. Replaced legacy framed downloads with bounded, verified ZIP
  snapshots and carried observed revisions through automatic/manual sync.
- Disabled calls to unimplemented legacy purchases, feedback, donation history,
  and text analysis; hid purchase offers and subscription status. Added JVM
  protocol tests and documented backend deployment and platform validation needs.

## 2026-10-15

- **Initialization**: Established the OKF bundle for the Kanji-Jojo project (a community
  fork of the archived Kanji-Dojo). Created the [Operating Manual](/AGENTS.md)
  (schema layer), the root [index](/index.md), and this log.
- **Creation**: Seeded architecture concepts - [Multiplatform overview](/architecture/multiplatform.md),
  [Build system](/architecture/build-system.md), [Data layer](/architecture/data-layer.md),
  and [SRS engine](/architecture/srs.md).
- **Creation**: Seeded module concepts - [app](/modules/app.md), [core](/modules/core.md),
  [desktopApp](/modules/desktop-app.md), [iosApp](/modules/ios-app.md), and
  [mediaGenerator](/modules/media-generator.md).
- **Adoption**: Adopted [/okf-SPEC.md](/okf-SPEC.md) and [/llm-wiki.md](/llm-wiki.md)
  as first-class `Reference` concepts (added OKF frontmatter) so the whole
  `okf/` tree is conformant.
- **Agent discovery**: Added a repo-root `AGENTS.md` with the required OKF
  reading order and maintenance triggers, plus thin `CLAUDE.md` and `GEMINI.md`
  pointers. Added a prominent README entry and updated the
  [Operating Manual](/AGENTS.md) and [index](/index.md) to distinguish the
  repository guide from the bundle manual and clarify bundle-relative links.
