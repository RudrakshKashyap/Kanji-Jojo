---
type: Module
title: "app (Android)"
description: The Android application shell - thin wrapper over core with platform-specific wiring, product flavors, and Firebase/billing integration.
resource: repo:app
tags: [module, android, app]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# app (Android)

## Responsibility

The Android entry point. This is a **thin shell** - nearly all logic and UI
lives in [core](/modules/core.md). The `app` module provides:

- Android `Application` and `Activity` classes
- Platform-specific DI wiring (Koin Android modules)
- Product flavor configuration (googlePlay vs. fdroid)
- Firebase, billing, and in-app review integration (googlePlay only)
- App icon resources and Android manifest

## Package

- **Namespace / Application ID:** `com.rudraksh.kanjijojo`
- **Min SDK:** 26
- **Target/Compile SDK:** 36

## Product flavors

See [Build system](/architecture/build-system.md) for details on the
`googlePlay` and `fdroid` flavors.

## Source sets

| Source set   | Contents                                        |
|--------------|-------------------------------------------------|
| `main`       | Shared Android code, manifest, resources, icons |
| `googlePlay` | Firebase, Play Billing, In-App Review wiring    |
| `fdroid`     | No-op stubs for Google-only features             |

## Dependencies

Depends on `:core` (the only module dependency). Flavor-specific dependencies:
- `googlePlay`: Firebase BOM, Analytics, Crashlytics, Play Billing, In-App Review
