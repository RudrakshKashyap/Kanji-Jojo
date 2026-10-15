---
type: Module
title: "mediaGenerator"
description: Asset generation tooling for building app data and media files.
resource: repo:mediaGenerator
tags: [module, tooling, assets]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# mediaGenerator

## Responsibility

A build-time / development-time tooling module for generating app assets.
Used to prepare the pre-built `AppDataDatabase` asset and other media files
that ship with the app.

See [Kanji-Jojo-Data](https://github.com/RudrakshKashyap/Kanji-Jojo-Data) for
the upstream dictionary data and parsers.

## TODO

- Document what assets this module generates
- Document how to run it (is it a Gradle task? a standalone main?)
- Document its relationship to the Kanji-Jojo-Data repository
