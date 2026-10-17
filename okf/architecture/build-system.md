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

Release 3.0.0 uses `AppVersion.versionName = "3.0.0"` and Android version code
`3001` (the initial Play upload used `3000`); desktop packages use that same
version name. Xcode keeps independent
marketing/build values (`3.0.0` / `12`). See repo-root `RELEASING.md` for the
release procedure and its platform-validation limits.

## Android product flavors

The `:app` module has two product flavors on a `version` dimension:

| Flavor       | Application ID suffix | Notes                                       |
|--------------|-----------------------|-----------------------------------------------|
| `googlePlay` | (none)                | Includes Firebase Analytics/Crashlytics, Play Billing, In-App Review |
| `fdroid`     | `.fdroid`             | No Google services; Google/Firebase tasks are disabled at the task graph level |

Debug builds additionally get `.dev` appended to the application ID and
`-debug` to the version name.

The Google Play flavor uses Play Billing Library `8.0.0`, configured in
`gradle/libs.versions.toml`, to meet Play's minimum Billing 8 requirement.
Billing calls use the KTX `queryProductDetails` wrapper and the parameterized
`enablePendingPurchases` API. Purchase offers remain disabled where the owned
backend has no billing support; the library upgrade does not enable payments.

## Signing

The owned Google Play Firebase config must include the debug package
`com.rudraksh.kanjijojo.dev` to compile that variant. F-Droid does not run Google
Services processing. Account/sync tests in `core`'s JVM test source set use a
test-only Ktor MockEngine dependency; production dependencies are unchanged.

Debug builds always use the Android debug signing key. Release builds use a
`signedBuild` signing config that reads from `keystore.jks` at the project root.
Keystore credentials come from environment
variables: `KEYSTORE_PASS`, `SIGN_KEY`, `SIGN_PASS`. If `keystore.jks` does not
exist, release retains the inherited fallback to the `debug` signing config.

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

`build-release.yml` triggers on pushed `v*.*` tags and calls `build-all.yml`.
The reusable build also produces an Apple Silicon DMG on `macos-15`. After all
builds pass, the release job checks out the tagged source and publishes the
downloaded assets with `release-notes/<tag>.md` as its body. The workflow defaults
to read-only contents access; only the publishing job gets `contents: write`.
It does not publish to app stores or produce an iOS binary. Coordinated website
deployment remains separate from this GitHub release workflow.

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
