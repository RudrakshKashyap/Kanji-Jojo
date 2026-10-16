---
type: Feature
title: Owned accounts and snapshot sync
description: Shared browser approval, Firebase REST credentials, private backups, and migration boundaries.
resource: repo:core/src/commonMain/kotlin/ua/syt0r/kanji/core/JojoService.kt
tags: [accounts, sync, migration, fdroid]
---

# Ownership and rollout

New client sources use `https://kanji-jojo.com` and Firebase project
`kanji-jojo-mhmy`. `JojoService` owns the origin, API base, public Auth API key,
and 4,000,000-byte backup limit. The companion Next.js repository is
`Kanji-Jojo-Web`; its backend must be deployed before releasing this client.
Old released app versions still use the original service. No old accounts or
cloud backups are imported automatically.

The Google Play Android Firebase configuration already belongs to the owned
project. Its debug variant additionally needs a Firebase Android registration
for `com.rudraksh.kanjijojo.dev`; do not change package IDs or invent a Firebase
mobile app ID to bypass the Google Services plugin check.

# Browser sign-in on every platform

The app creates a random flow ID, state, and private verifier. Only flow ID,
state, and the verifier's SHA-256 base64url challenge enter the browser URL:
`/account?appAuth=1&flowId=...&state=...&challenge=...&platform=mobile|desktop`.
Android/JVM use SecureRandom and MessageDigest; iOS uses Security/CoreCrypto.
Neither verifier nor Firebase credentials enter a URL or app log.

The user signs in on the owned website and explicitly approves app access.
On mobile, Continue approves the grant and attempts to open the fixed app
return link in one action; a Return link remains if the browser blocks opening.
Returning only resumes the app and does not authorize anything. Android may
pause background polling while the browser is open.
The website records a short-lived grant. The app polls
`POST /api/auth/device/token` with its verifier/state every two seconds, for up
to five minutes. Temporary connection failures, HTTP 408/429, and server errors
are retried within that same deadline; permanent errors still require restarting
sign-in. Unauthenticated calls have a 30-second request timeout; authenticated
sync calls have a 60-second timeout, with 10-second connection and 30-second
socket timeouts. Pending approval is HTTP 202. A valid grant can be consumed
only once, within two minutes of approval. The backend returns a Firebase custom
token; the app exchanges it at Firebase Auth REST's `signInWithCustomToken`
endpoint for its own ID/refresh tokens. Refresh uses the owned project's Secure
Token endpoint, form-encoded requests, and persists rotated refresh tokens.
Changes to either the ID or refresh token invalidate the client's cached bearer
credentials, including a re-sign-in that reuses the refresh token.
Account sign-in and sign-out also clear the bearer cache explicitly, so an
immediate profile lookup cannot race the asynchronous preference observers.
New credentials clear the previous user's cached profile before that lookup.

This is an open app-specific approval/polling protocol, not a claim to implement
the standard OAuth device grant. It replaces the inherited refresh-token-in-URL
handoff and desktop Netty callback server. Desktop needs no listening port,
loopback CORS, or browser POST to localhost. Mobile's fixed `kanji-jojo://signin`
link only resumes the account UI; it carries no credentials and cannot complete
sign-in without the pending app process's verifier. Process termination loses the
pending proof, so restart sign-in rather than replaying an old browser tab.

The shared AccountManager owns the pending job; sign-out/new sign-in cancels it.
Browser opening happens on the main dispatcher. F-Droid embeds no Firebase Auth
or Play Services SDK; Google/Firebase remains an optional network dependency.
Local/offline study and export remain available without an account.

# Identity migration

The public migration path is local export/import, followed by sync: in Kanji
Dojo create a backup under Settings → Backup & Restore; restore that ZIP in
Kanji Jojo, verify decks/progress, then sign in and sync the imported local data.
Restore replaces the current local dataset rather than merging it. Old accounts
and cloud archives do not transfer automatically. The app README, 3.0.0 release
notes, and companion website `/migration` explain this sequence; website sign-in
alone never imports/uploads the user's study data.

Owned-service tokens, account cache, and last-sync cache use `jojo_v1_*`
preference keys. Existing legacy credentials are not read or sent to the new
service. Local database, study IDs, review progress, and settings are preserved.
New sign-in clears the old sync cache for that session. Backend authorization
comes from the verified new-project Firebase UID, never a supplied owner/email.

`GET /api/account` returns the current account and `capabilities.sync`. The
inherited internal subscription flag temporarily maps to this free sync
capability; it is not proof of a paid subscription. Account subscription status
rows and Google Play purchase offers are hidden. Purchases, receipt submission,
feedback submission, donation history, and hosted text analysis have no owned
replacement yet; they fail locally with an explicit unavailable result and send
nothing to the old operator. Google Play cannot initiate these purchases while
their backend is absent.

# Snapshot contract and conflicts

`GET /api/v1/sync` returns nullable metadata: revision, study data ID, schema
version, optional original study timestamp, byte size, SHA-256, and server update
time. These two times have different meanings: preserve `dataTimestamp` for
existing local-vs-remote study comparisons; do not substitute upload time.

Upload is raw ZIP via PUT to the same endpoint, with quoted `If-Match` revision,
`X-Backup-Data-Id`, `X-Backup-Data-Version`, and optional
`X-Backup-Data-Timestamp`. Revision zero means no remote snapshot. Automatic
uploads and manual conflict choices carry the revision actually observed before
the decision. Never fetch a new revision just before uploading and silently
overwrite a newer device's work. HTTP 409 requires refreshing sync/reconsidering
the conflict. Study timestamp/equality comparisons exclude the transport revision.

Download fetches metadata and `/api/v1/sync/download?revision=N` for the expected
revision. The app rejects changed revisions, excessive sizes, mismatched lengths,
and invalid SHA-256 before restoring a temporary ZIP. Legacy binary JSON framing
is gone. API bearer tokens are sent preemptively only to the owned HTTPS host.

Cloud archives are capped at 4 MB; larger local exports remain possible. No
E2EE or old-service migration is implemented. Integration testing requires the
companion backend, private Storage, rules/IAM, real archives, and disposable
accounts. Kotlin JVM protocol tests use Ktor MockEngine. iOS needs a macOS/Xcode
compile and device sign-in check; Linux cannot validate that target.

# Sync operation lifecycle and recovery

Each enabled account owns an intent worker. An operation and its live data
trackers share a child scope: replacing/cancelling it stops old trackers and
settles any waiting result. A thrown refresh, backup, cleanup, or tracker error
is reported as a retryable sync error without cancelling the worker. Queued
requests are cancelled when their account scope ends; no result is silently
dropped. The UI blocks repeated taps during refresh/upload/download, and ignores
a tap that races with an account-state transition. From the home screen, a sync
error or conflict opens the sync screen to expose retry and resolution controls.

JVM regression tests cover thrown refresh/upload errors, cancellation and retry,
tracker cleanup/failure, network polling retries, permanent rejection, and the
overall sign-in deadline, and cached bearer replacement on sign-in/sign-out.
Real device/browser and cloud validation remain
necessary; tests do not authenticate a live account or restore real study data.
