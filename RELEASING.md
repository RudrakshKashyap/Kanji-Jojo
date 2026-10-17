# Releasing Kanji Jojo

## Prepared release: 3.0.0

- Android: version name `3.0.0`, version code `3001` (the initial Play upload used `3000`).
- Desktop packages: version `3.0.0`, using the shared `AppVersion`.
- iOS source: marketing version `3.0.0`, build number `12`. The GitHub workflow does not build or publish iOS.
- GitHub release body: [release-notes/v3.0.0.md](release-notes/v3.0.0.md).
- Android listing changelog: [3001.txt](fastlane/metadata/android/en-US/changelog/3001.txt).

## Review before publishing

1. Review and commit the app changes, including the version, in-app changelog, README, release notes, and metadata.
2. Review and publish the companion website changes from `Kanji-Jojo-Web`, including `/migration`. The website/backend and app must support the same sign-in and sync protocol. Check real sign-in, backup restore, and sync against the deployed backend before announcing sync as available.
3. Confirm repository Actions secrets `KEYSTORE`, `KEYSTORE_PASS`, `SIGN_KEY`, and `SIGN_PASS` contain the intended release signing configuration. `KEYSTORE` is the base64-encoded Android keystore. Keep the release key consistent with previously published Kanji Jojo builds.
4. Validate the platforms you intend to publish. An Android debug build does not validate release signing, R8, Windows/macOS packaging, or iOS.

## Tag and build

Once the reviewed app commit has been pushed, create and push the release tag from that commit:

```sh
git tag -a v3.0.0 -m "Kanji Jojo 3.0.0"
git push origin v3.0.0
```

Pushing a `v*.*` tag triggers `.github/workflows/build-release.yml`. It calls the build workflow for the F-Droid-flavor Android APK, Linux AppImage, Windows MSI, and Intel/Apple Silicon macOS DMGs. After all builds succeed, it publishes their assets with the matching `release-notes/<tag>.md` as the release description. The release job has the `contents: write` permission needed to publish.

Check the workflow result and attached assets before sharing the release. These workflows publish to GitHub, not Google Play, F-Droid's repository, or the App Store.

## Future versions

Update `buildSrc/src/main/kotlin/AppVersion.kt`, the Xcode marketing/build versions, the README version badge, the in-app version dialog, and Android metadata. Add `release-notes/v<version>.md` before pushing the matching tag. Increment Android's version code and iOS's build number independently, without reusing a released value.
