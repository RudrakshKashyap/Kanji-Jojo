# Bundle Update Log

Bundle-wide and cross-cutting changes. Newest first.

## 2026-10-15

- **Initialization**: Established the OKF bundle for the Kanji-Jojo project (a community
  fork of the archived Kanji-Dojo). Created the [Operating Manual](/AGENTS.md)
  (schema layer), the root [index](/index.md), and this log.
- **Creation**: Seeded architecture concepts - [Multiplatform overview](/architecture/multiplatform.md),
  [Build system](/architecture/build-system.md), [Data layer](/architecture/data-layer.md),
  and [SRS engine](/architecture/srs.md).
- **Creation**: Seeded module concepts - [app](/modules/app.md), [core](/modules/core.md),
  [desktopApp](/modules/desktop-app.md), [iosApp](/modules/ios-app.md), and
  [mediaGenerator](/modules/media-generator.md).
- **Adoption**: Adopted [/okf-SPEC.md](/okf-SPEC.md) and [/llm-wiki.md](/llm-wiki.md)
  as first-class `Reference` concepts (added OKF frontmatter) so the whole
  `okf/` tree is conformant.
- **Agent discovery**: Added a repo-root `AGENTS.md` with the required OKF
  reading order and maintenance triggers, plus thin `CLAUDE.md` and `GEMINI.md`
  pointers. Added a prominent README entry and updated the
  [Operating Manual](/AGENTS.md) and [index](/index.md) to distinguish the
  repository guide from the bundle manual and clarify bundle-relative links.
