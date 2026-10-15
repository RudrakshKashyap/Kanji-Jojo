---
type: Architecture
title: Build System
description: Gradle configuration, product flavors, signing, CI/CD pipelines, and how the project builds for all targets.
resource: repo:build.gradle.kts
tags: [architecture, build, gradle, ci]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# Build system

## Gradle setup

- **Kotlin:** 2.1.20
- **AGP:** 8.5.2
- **Compose Multiplatform:** 1.8.2 (with hot-reload plugin 1.0.0-alpha09)
- **JVM toolchain:** 17
- **Root project name:** `kanji-jojo`

## Android product flavors

The `:app` module has two product flavors on a `version` dimension:

| Flavor       | Application ID suffix | Notes                                       |
|--------------|-----------------------|-----------------------------------------------|
| `googlePlay` | (none)                | Includes Firebase Analytics/Crashlytics, Play Billing, In-App Review |
| `fdroid`     | `.fdroid`             | No Google services; Google/Firebase tasks are disabled at the task graph level |

Debug builds additionally get `.dev` appended to the application ID and
`-debug` to the version name.

## Signing

All build types use a `signedBuild` signing config that reads from
`keystore.jks` at the project root. Keystore credentials come from environment
variables: `KEYSTORE_PASS`, `SIGN_KEY`, `SIGN_PASS`. If `keystore.jks` does not
exist, falls back to the `debug` signing config.

The CI pipeline decodes the keystore from a base64 secret (`KEYSTORE`) before
building.

## CI/CD (GitHub Actions)

The "Build All" workflow (`build-all.yml`) runs on `workflow_call` and
`workflow_dispatch`:

| Job                           | Runner            | Output                                |
|-------------------------------|-------------------|---------------------------------------|
| `build-android-and-desktop-linux` | `ubuntu-latest`  | F-Droid APK + Linux AppImage          |
| `build-desktop-windows`       | `windows-latest`  | Windows MSI installer                 |
| `build-desktop-mac-intel`     | `macos-15-intel`  | macOS DMG (Intel)                     |

A separate release workflow likely triggers on tag push (TODO: verify).

## Desktop packaging

- **Linux:** Custom `desktopApp/linux/AppImage/make.sh` script
- **Windows:** `./gradlew desktopApp:packageDistributionForCurrentOS` (MSI)
- **macOS:** Separate builds for ARM and Intel architectures

## Proguard / R8

Release builds enable `isMinifyEnabled` and `isShrinkResources` with standard
Android proguard plus project-specific `proguard-rules.pro`. The core module
has its own `consumer-rules.pro`.

## `dependenciesInfo`

`includeInApk = false` for reproducible F-Droid builds (removes signing block
with encrypted data).
