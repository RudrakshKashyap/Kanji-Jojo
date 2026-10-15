# Kanji Jojo — repo guide for agents

This is the shared repository guide for all coding agents. Keep repo-wide
instructions here; `CLAUDE.md` and `GEMINI.md` are thin pointers to this file.
The detailed knowledge-bundle rules live in [okf/AGENTS.md](okf/AGENTS.md).

Kanji Jojo is an offline Japanese learning app and community continuation of
Kanji Dojo, built with Kotlin Multiplatform and Compose Multiplatform for
Android, iOS, and desktop. `core` holds shared logic and UI; `app`, `iosApp`,
and `desktopApp` provide platform entry points. `mediaGenerator` provides
screenshot and recording tooling. Internal `ua.syt0r.kanji` packages are
inherited from the original project.

## Start every session with the knowledge base

Before exploring source, answering project questions (including "what is this
project about?"), or making changes:

1. Read [okf/AGENTS.md](okf/AGENTS.md), the bundle's operating manual.
2. Read [okf/index.md](okf/index.md), the catalog of architecture, module,
   feature, convention, and playbook knowledge.
3. Follow the entries relevant to the task, then verify implementation details
   against the current source and build configuration.

Use the bundle to find context and decisions before re-deriving them from code.
Some concepts are drafts or contain TODOs; distinguish documented intent from
implemented behavior. Verify `resource: repo:...` paths before relying on them.
For project explanations, cite the relevant knowledge pages and source files.

Inside `okf/`, links beginning with `/` resolve from the **bundle root**, not
the repository root: `/AGENTS.md` there means `okf/AGENTS.md`.

## Keep the knowledge base current

Update the bundle when you:

- Add, remove, rename, or move a Gradle module: update its module concept and
  `okf/modules/index.md`.
- Change the `core` module's public API, shared screens, DI modules, or database
  migrations: update the relevant module and architecture concepts.
- Change build, signing, or flavor configuration: update
  [the build-system concept](okf/architecture/build-system.md).
- Add, remove, or swap an architecturally significant dependency; routine
  version bumps alone do not qualify.
- Change a database schema: update [the data-layer concept](okf/architecture/data-layer.md).
- Change the SRS algorithm or parameters: update [the SRS concept](okf/architecture/srs.md).
- Agree on a convention or decision future contributors need: record a
  `Convention` or `Decision` concept.
- Ship or materially change a feature with a non-obvious platform quirk, data
  model subtlety, or cross-cutting decision: record that knowledge under
  `okf/features/`. If you cannot name the non-obvious fact in one sentence,
  the feature does not need a page.

For each qualifying change, update the relevant concept, refresh the affected
`index.md`, and append a dated entry to the appropriate `log.md`. Use
[okf/log.md](okf/log.md) for bundle-wide or cross-cutting changes; follow the
operating manual's section 7.1 for scoped logs. If a required update cannot be
completed during the task, explicitly tell the user what remains.

Routine fixes, refactors without structural changes, ordinary feature work,
dependency bumps, and cosmetic changes do not require bundle churn. Preserve
the raw sources `okf/okf-SPEC.md` and `okf/llm-wiki.md`. Follow the operating
manual for frontmatter, links, indexing, and logging when editing the bundle.
