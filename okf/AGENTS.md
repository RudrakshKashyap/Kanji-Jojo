---
type: Reference
title: OKF Bundle - Operating Manual
description: How agents read, write, and maintain this knowledge bundle. Read this first.
tags: [meta, schema, agents]
generated: { by: human:rudraksh, at: 2026-10-15T00:00:00Z }
---

This file is the **schema layer** for this knowledge bundle: the configuration
that turns an LLM agent into a disciplined wiki maintainer rather than a generic
chatbot. Any agent (Claude Code, Codex, Gemini, etc.) working in `okf/` should
read this first, then [the index](/index.md).

It combines two ideas:

- **OKF** - the [Open Knowledge Format](/okf-SPEC.md): a directory of markdown
  files with YAML frontmatter, one required `type` field, and `index.md` /
  `log.md` as reserved files. It defines the _format_.
- **The LLM Wiki pattern** - see [/llm-wiki.md](/llm-wiki.md): a persistent,
  compounding, agent-maintained knowledge base. It defines the _workflow_.

This bundle **dogfoods OKF**: the format's own spec and the wiki pattern live
inside the bundle as `Reference` concepts.

## Repository entry points

The repo-root [AGENTS.md](../AGENTS.md) is the shared guide for all agents.
Repo-root [CLAUDE.md](../CLAUDE.md) and [GEMINI.md](../GEMINI.md) point to it,
and the [README](../README.md) advertises the same reading order. That guide
requires agents to read this manual and [/index.md](/index.md) before project
questions or code work and summarizes the update triggers in section 6.1.
Keep repo-wide guidance in that root guide and detailed bundle rules here.

The `../` links above leave the bundle for repository files. Links beginning
with `/` elsewhere in this manual are bundle-relative: `/AGENTS.md` means
this operating manual, not the repo-root guide.

---

## 1. What this bundle is for

A living knowledge base for **Kanji-Jojo** - a Kotlin Multiplatform (Compose
Multiplatform) app for learning Japanese characters (kana and kanji), forked from
the now-archived [Kanji-Dojo](https://github.com/syt0r/Kanji-Dojo). The app
targets Android, iOS, and Desktop (Windows, Linux, macOS) from a shared
codebase. Its job is to capture the _knowledge around the code_: architecture,
per-module responsibilities, data model decisions, platform-specific quirks, and
playbooks - the context that a fresh agent or contributor needs and that git
history alone does not convey.

**Fork context:** The original Kanji-Dojo project was archived by its creator
(Yaroslav Shuliak). Kanji-Jojo is a community continuation under the same GNU
GPL v3.0 license. So far, only branding changes have been made (logo, package
name `com.rudraksh.kanjijojo`). The codebase is inherited as-is and carries the
original `ua.syt0r.kanji` internal package naming in many places.

It is **not** a mirror of the source tree. Do not document individual files or
paste code that the repo already holds. Document intent, structure, decisions,
and "how to" - the things that are expensive to re-derive.

## 2. The three layers (from the LLM-wiki pattern)

| Layer            | Where                                   | Who owns it    |
| ---------------- | --------------------------------------- | -------------- |
| **Raw sources**  | [/okf-SPEC.md](/okf-SPEC.md), [/llm-wiki.md](/llm-wiki.md), plus any material you ingest | Immutable. Read, never rewrite. |
| **The wiki**     | Every other concept in this tree        | Agent-authored & maintained. |
| **The schema**   | This file (`AGENTS.md`)                 | Co-evolved by you + the human. |

## 3. Directory map

```
okf/
├── AGENTS.md              # This file - the schema/operating manual.
├── index.md              # Root catalog (progressive disclosure). See spec section 8.
├── log.md                # Bundle-wide + cross-cutting change history (see section 7.1).
├── okf-SPEC.md           # Reference: the OKF format spec (raw source).
├── llm-wiki.md           # Reference: the LLM-wiki pattern (raw source).
├── architecture/         # System-level structure & cross-cutting design.
├── modules/              # One concept per Gradle module (app, core, desktopApp, iosApp, etc.).
├── features/             # Per-feature concepts - only features that clear the section 6.1 bar.
├── playbooks/            # Runbooks / how-to guides.
└── conventions/          # Coding/naming/structural standards.
```

New top-level groups are welcome as knowledge grows (e.g. `decisions/` for ADRs,
`data-model/` for database schema docs). Add a matching `index.md` when you
create one. A group may also carry its own scoped `log.md` once its local
history would crowd the root - see section 7.1.

## 4. Frontmatter contract

Every concept document (every `.md` that is **not** `index.md` or `log.md`) MUST
open with a YAML frontmatter block whose only required key is `type`.

```yaml
---
type: <one of the vocabulary below>   # REQUIRED
title: <human display name>
description: <one-sentence summary - used by index.md and previews>
resource: repo:<path-from-repo-root>  # for concepts bound to a repo asset
tags: [<tag>, ...]
generated: { by: <actor>, at: <ISO 8601> }
---
```

### Type vocabulary (this bundle's convention)

Types are free-form in OKF; we keep to this set for consistency. Add a new type
if none fits, and note it here.

| `type`         | Use for                                                    |
| -------------- | ---------------------------------------------------------- |
| `Architecture` | System-level structure, build graph, cross-cutting design. |
| `Module`       | A Gradle module (`app`, `core`, `desktopApp`, `iosApp`, `mediaGenerator`). |
| `Feature`      | A single app feature/domain slice that clears the section 6.1 bar (non-obvious implementation detail, platform quirk, or cross-cutting decision). NOT for ordinary features. |
| `Playbook`     | A step-by-step runbook ("how to add a new screen").        |
| `Decision`     | An architecture decision record (ADR): context, choice, consequences. |
| `Convention`   | A coding/naming/structural standard to follow.             |
| `Reference`    | External material mirrored in-bundle (specs, patterns).    |
| `Data Model`   | Database schema, SRS algorithm, or data flow documentation.|

### The `resource` convention

For a concept bound to something in this repo, set
`resource: repo:<path-from-repo-root>` (e.g. `repo:core`). The `repo:`
scheme is deliberately path-relative-to-repo-root, not an absolute filesystem
path, so it survives the repo moving on disk. **Repo paths still drift** - see
section 7.

## 5. Cross-linking - use bundle-relative absolute links

Link concepts with standard markdown links. **Always prefer the absolute,
bundle-relative form** (leading `/`, relative to `okf/`):

```markdown
See [the multiplatform architecture](/architecture/multiplatform.md).
```

This is the OKF-recommended form _because it stays valid when a document is moved
within the bundle_ - critical here, since this fork is still being restructured.
Avoid `../`-style relative links between subdirectories.

Broken links are **allowed** (OKF spec section 6): a link to a not-yet-written
concept (e.g. a feature that doesn't exist yet) is a valid marker of future
knowledge, not an error.

## 6. Operations (run these on request)

- **Ingest.** A new source (a decision thread, a design doc, a Slack export, a
  spec) - read it, discuss takeaways, write/update the relevant concept(s),
  refresh the affected `index.md`, and append a `log.md` entry. One source may
  touch several pages.
- **Query.** To answer a question: read [/index.md](/index.md) first to locate
  relevant concepts, drill in, synthesize with citations. **File good answers
  back** as new concepts so explorations compound.
- **Lint.** On request, health-check the bundle: contradictions, stale claims,
  orphan concepts (no inbound links), missing cross-references, concepts whose
  `resource:` path no longer exists (see section 7), and gaps worth filling.
  Report findings; fix with approval; log the pass.

### 6.1 When to update - trigger conditions

These are **not** run only on request. Whenever you are working in this repo and
one of the following happens, proactively ingest it into the bundle (or, if you
can't finish mid-task, explicitly tell the human it needs updating). These
triggers should also be mirrored in the repo-root `CLAUDE.md` / `AGENTS.md` so
agents see them repo-wide, not just inside `okf/`.

Update the bundle when you:

- **add / remove / rename / move a Gradle module** - update
  [/modules/](/modules/) + its `index.md`.
- **change the `core` module's public API** (new shared screen, DI module,
  database migration) - update that module's concept.
- **change the build/signing/flavoring setup** (build types, product flavors,
  signing configs) - update [/architecture/build-system.md](/architecture/build-system.md).
- **add/remove/swap an architecturally-significant dependency** (Compose version,
  Koin, SqlDelight, Ktor) - **not** routine version bumps.
- **change the database schema** (SqlDelight migrations for `AppDataDatabase` or
  `UserDataDatabase`) - update the data model concept.
- **change the SRS algorithm or parameters** - update
  [/architecture/srs.md](/architecture/srs.md) (or create it).
- **agree a new convention or decision** future contributors must follow - file a
  new `Convention` or `Decision` concept.
- **ship or materially change a feature that clears the bar below** - file/update a
  `Feature` concept under [/features/](/features/) + its `index.md`. **The bar is
  high, and it is the feature's *knowledge*, not its existence, that qualifies.** A
  feature earns a page ONLY if it carries something code + git history don't
  convey: a non-obvious **platform quirk** (Android vs. iOS vs. Desktop
  differences), a **data model subtlety**, or a **cross-cutting decision** made
  inside the feature. Document *only* that expensive-to-re-derive knowledge.
  If you can't name the specific non-obvious fact in one sentence, it does **not**
  qualify - do nothing.

**Do not** churn the bundle for: routine dependency bumps, bug fixes/refactors
with no structural change, **ordinary feature work** (a feature that's just
"the standard shape applied to a new screen" clears no bar), or cosmetic
changes (branding updates, icon tweaks).

When a trigger fires, the action is always the same three steps: update the
relevant concept, refresh the affected `index.md`, append a log entry to the
appropriate log (root, or a scoped `<group>/log.md` - see section 7.1).

## 7. Resilience to restructuring (this fork is a moving target)

The repo is a fresh fork with minimal changes so far. The `ua.syt0r.kanji`
internal package naming is inherited and may be migrated. Maintain the wiki so a
restructure never rots it:

1. **Describe responsibility, not file lists.** Say what a module is _for_
   and how it relates to others. Don't enumerate its files - that churns.
2. **Treat `resource:` repo paths as best-effort, not truth.** Before relying on
   one in an answer, verify it still exists. If a path moved, update the concept
   and log it; don't silently trust stale paths.
3. **Prefer bundle-relative absolute links** (section 5) so intra-bundle moves
   are safe.
4. **Forward-reference freely.** Concepts for planned-but-unbuilt features
   (e.g. new study modes) are encouraged; mark them as planned so no one
   mistakes intent for reality.
5. **After any significant repo restructure, run a Lint pass** focused on
   `resource:` drift and update the affected concepts + `log.md`.

### 7.1 Log scoping (grows with the bundle)

OKF (spec section 9) lets a `log.md` sit **at any level** of the tree, recording
history for **that scope**. We use this so one flat log doesn't become unreadable
as the bundle grows:

- **Root [/log.md](/log.md)** holds **bundle-wide and cross-cutting** changes:
  schema/convention/decision changes, anything touching multiple groups, and
  the top-level narrative.
- **A scoped `<group>/log.md`** (e.g. `/features/log.md`) holds changes
  **local to a single concept in that subtree**.
- **Spin up a scoped log lazily**, not upfront: create `<group>/log.md` only once
  a group's local entries would otherwise crowd the root (rule of thumb ~15-20).
  Until then, log everything in the root. Empty scaffolding is churn.
- **Scope is per-*group*, never per-*concept-file*.** One `features/log.md` for
  all features - do **not** create `features/srs.log`. Per-file history is what
  `git log --follow <concept>` already gives you, for free.
- **When in doubt, or when a change is cross-cutting, log it in the root.**
- Every log - root or scoped - stays date-grouped (`## YYYY-MM-DD`), newest
  first, and starts with a one-line note of what it scopes + a pointer to the
  root.

## 8. Conformance checklist (OKF v0.2)

A change keeps the bundle conformant if:

- [ ] Every non-reserved `.md` has parseable frontmatter with a non-empty `type`.
- [ ] `index.md` and `log.md` files carry no frontmatter - **except** the root
      [/index.md](/index.md), which may declare `okf_version`.
- [ ] Every `log.md` (root or scoped) is date-grouped (`## YYYY-MM-DD`), newest
      first; scoped logs are per-group, not per-concept-file (section 7.1).
- [ ] New cross-links use the bundle-relative absolute form.
- [ ] Any new `type` value or top-level group is recorded here.

When in doubt, the full rules are in [/okf-SPEC.md](/okf-SPEC.md).
