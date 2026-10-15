---
okf_version: "0.2"
---

# Start here

- [Repository agent guide](../AGENTS.md) - repo-wide entry point, also reached through repo-root `CLAUDE.md` and `GEMINI.md`. Read before this bundle's manual and index.
- [Operating Manual (AGENTS.md)](/AGENTS.md) - how to read, write, and maintain this bundle. Read after the repository guide, before the concepts below.
- [Change log](/log.md) - chronological history of the bundle.

# Architecture

- [Multiplatform overview](/architecture/multiplatform.md) - Kotlin Multiplatform structure, Gradle modules, and how the shared code maps to platform targets.
- [Build system](/architecture/build-system.md) - Gradle configuration, product flavors (googlePlay/fdroid), signing, and CI/CD.
- [Data layer](/architecture/data-layer.md) - SqlDelight databases (AppData, UserData), DataStore preferences, and how data flows through the app.
- [SRS engine](/architecture/srs.md) - the FSRS-based spaced repetition system, scheduling logic, and review parameters.

# Modules

- [modules/](/modules/) - one concept per Gradle module.
- [app (Android)](/modules/app.md) - the Android application shell (`com.rudraksh.kanjijojo`), flavors, and platform-specific wiring.
- [core](/modules/core.md) - the shared KMP library: all business logic, UI (Compose Multiplatform), databases, and DI.
- [desktopApp](/modules/desktop-app.md) - the JVM/Desktop entry point.
- [iosApp](/modules/ios-app.md) - the iOS entry point (Swift/Xcode wrapper).
- [mediaGenerator](/modules/media-generator.md) - asset generation tooling.

# Features

- [features/](/features/) - per-feature concepts, only for features carrying non-obvious knowledge (high bar - see [Operating Manual section 6.1](/AGENTS.md)).

# Playbooks

- [playbooks/](/playbooks/) - runbooks and how-to guides.

# Conventions

- [conventions/](/conventions/) - standards and rules contributors must follow.

# Sources

Raw, immutable material this bundle is built on (the LLM-wiki "raw sources" layer):

- [Open Knowledge Format spec](/okf-SPEC.md) - the format this bundle conforms to.
- [The LLM Wiki pattern](/llm-wiki.md) - the maintenance workflow this bundle follows.
