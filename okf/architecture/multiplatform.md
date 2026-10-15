---
type: Architecture
title: Multiplatform Overview
description: Kotlin Multiplatform structure, Gradle modules, and how the shared core maps to Android, iOS, and Desktop targets.
resource: repo:.
tags: [architecture, kmp, multiplatform]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# Overview

Kanji-Jojo is a **Kotlin Multiplatform** (KMP) project using **Compose
Multiplatform** for shared UI. Nearly all logic and UI lives in the shared
[core](/modules/core.md) module; platform entry points are thin shells.

## Module graph

```
settings.gradle.kts includes:
  :app           Android application (com.rudraksh.kanjijojo)
  :core          Shared KMP library (business logic + Compose UI + databases)
  :desktopApp    JVM/Desktop entry point
  :iosApp        iOS entry point (Xcode project, wraps core via Swift)
  :mediaGenerator  Asset generation tooling
```

All platform apps depend on `:core`. There are no other inter-module
dependencies.

## Source set mapping

The `core` module declares these Kotlin source sets:

| Source set     | Target              | Platform-specific concerns                   |
|----------------|---------------------|----------------------------------------------|
| `commonMain`   | All platforms        | 100% of business logic, Compose UI, DI, databases |
| `androidMain`  | Android              | SqlDelight Android driver, AndroidX lifecycle, WorkManager, ExoPlayer |
| `jvmMain`      | Desktop (JVM)        | SqlDelight JVM driver, Ktor server (Netty), Compose Desktop |
| `iosMain`      | iOS (x64, arm64, sim)| SqlDelight native driver, Ktor Darwin client |

## Dependency injection

Koin is used across all platforms (`koin-core`, `koin-compose`,
`koin-compose-viewmodel`). Android additionally uses `koin-android` and
`koin-androidx-compose`. DI modules are defined in `core/src/commonMain/kotlin/ua/syt0r/kanji/di/`.

## Key shared libraries

| Library                     | Purpose                              |
|-----------------------------|--------------------------------------|
| Compose Multiplatform       | Shared UI (Material 3)               |
| Koin                        | Dependency injection                 |
| SqlDelight                  | Type-safe SQL databases              |
| Ktor                        | HTTP client (sync feature)           |
| kotlinx-serialization       | JSON serialization                   |
| kotlinx-datetime            | Cross-platform date/time             |
| DataStore Preferences       | Key-value user preferences           |
| compose-reorderable         | Drag-to-reorder lists                |
| aboutlibraries              | OSS license display                  |

## Internal package naming

The codebase internally uses `ua.syt0r.kanji` as the base package, inherited
from the original Kanji-Dojo project. The Android `namespace` and
`applicationId` have been changed to `com.rudraksh.kanjijojo`, but Kotlin source
packages remain `ua.syt0r.kanji`. A full package rename is a potential future
task but carries risk due to SqlDelight generated code, Compose resources, and
BuildKonfig all referencing the original package.
