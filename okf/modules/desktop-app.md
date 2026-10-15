---
type: Module
title: "desktopApp"
description: The JVM/Desktop entry point for Windows, Linux, and macOS using Compose Desktop.
resource: repo:desktopApp
tags: [module, desktop, jvm]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# desktopApp

## Responsibility

The Desktop (JVM) entry point. A thin shell that launches the Compose
Multiplatform UI from [core](/modules/core.md) in a desktop window.

## Targets

| Platform | Packaging              | Build command / tool                    |
|----------|------------------------|-----------------------------------------|
| Linux    | AppImage               | `desktopApp/linux/AppImage/make.sh`     |
| Windows  | MSI installer          | `./gradlew desktopApp:packageDistributionForCurrentOS` |
| macOS    | DMG (arm64 + x86_64)   | `./gradlew desktopApp:packageDistributionForCurrentOS` |

## Platform-specific notes

The `jvmMain` source set in `core` provides:
- `sqldelight-jvm-sqlite-driver` for database access
- `ktor-server-netty` (used for local sync server or asset serving)
- Compose Desktop runtime (`compose.desktop.currentOs`)
- Wav audio format for kana voice (vs. opus on Android)
