# Architecture

System-level structure and cross-cutting design decisions.

- [Multiplatform overview](/architecture/multiplatform.md) - Kotlin Multiplatform structure, Gradle modules, and how the shared code maps to platform targets.
- [Build system](/architecture/build-system.md) - Gradle configuration, product flavors (googlePlay/fdroid), signing, and CI/CD.
- [Data layer](/architecture/data-layer.md) - SqlDelight databases, preferences, and schema v13 personal notes included in backup/snapshot sync.
- [Owned account/sync protocol](/features/accounts-and-sync.md) - browser approval, private backups, and rollout constraints.
- [SRS engine](/architecture/srs.md) - the FSRS-based spaced repetition system, scheduling logic, and review parameters.
