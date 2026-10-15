---
type: Module
title: "iosApp"
description: The iOS entry point - an Xcode project wrapping the shared core via Kotlin/Native.
resource: repo:iosApp
tags: [module, ios, swift]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# iosApp

## Responsibility

The iOS entry point. An Xcode project that wraps the shared
[core](/modules/core.md) module via Kotlin/Native. The iOS-specific Kotlin code
lives in `core/src/iosMain/`.

## Targets

- `iosX64` (simulator on Intel Mac)
- `iosArm64` (physical device)
- `iosSimulatorArm64` (simulator on Apple Silicon)

## Platform-specific notes

The `iosMain` source set in `core` provides:
- `sqldelight-native-sqlite-driver` for database access
- `ktor-client-darwin` for network requests
- Wav audio format for kana voice
