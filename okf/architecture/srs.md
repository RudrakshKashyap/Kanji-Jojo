---
type: Architecture
title: SRS Engine
description: The FSRS-based spaced repetition system that drives review scheduling and character retention.
resource: repo:core/src/commonMain/kotlin/ua/syt0r/kanji/core/srs
tags: [architecture, srs, fsrs, spaced-repetition]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
status: draft
---

# SRS engine

## Overview

Kanji-Jojo uses the **FSRS (Free Spaced Repetition Scheduler)** algorithm for
scheduling reviews. The implementation lives in
`core/src/commonMain/kotlin/ua/syt0r/kanji/core/srs/`:

- `fsrs/` - core FSRS algorithm implementation
- `use_case/` - application-level use cases that bridge the FSRS engine with
  the user data layer

## How it works

FSRS is a modern spaced repetition algorithm that computes optimal review
intervals based on:

- **Difficulty** of each card
- **Stability** (how well the memory has been consolidated)
- **Retrievability** (probability of successful recall at the current moment)
- **Review history** (past grades and intervals)

The scheduler determines when each character/card should next be reviewed to
maximize retention while minimizing review count.

## Integration

The SRS engine reads review history from and writes results to the
`UserDataDatabase` (see [Data layer](/architecture/data-layer.md)). Daily review
limits are configurable through the daily-limit screen
(`presentation/screen/main/screen/daily_limit/`).

## TODO

- Document the specific FSRS parameters used
- Document the grading scale (how user input maps to FSRS grades)
- Document the daily limit logic and how it interacts with scheduling
