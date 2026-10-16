<div align="center">

  <img src="preview_assets/inkscape_icon.svg" height=120 style="border-radius: 20px;">

# Kanji Jojo

![Version Badge](https://img.shields.io/badge/version-v3.0.0-blue?style=for-the-badge&labelColor=ffffff&color=4A90E2)

</div>

**Kanji Jojo is the community continuation of [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo), the original app by Yaroslav Shuliak.** After the original repository was archived, this independent fork became a new home for continued development under the same GNU GPL v3.0 license.

Version **3.0.0** brings **My notes**, personal notes for each character, alongside Kanji Jojo's own account and sync service. More features and improvements are planned, and community ideas and contributions are welcome.

[Get Kanji Jojo](https://github.com/RudrakshKashyap/Kanji-Jojo/releases) · [Website](https://kanji-jojo.com) · [Moving from Kanji Dojo?](#migrate-from-kanji-dojo) · [Discord](https://discord.gg/ptAJzGnHYk)

## Table of Contents

- [About this app](#about-this-app)
- [Migrate from Kanji Dojo](#migrate-from-kanji-dojo)
- [What is new in 3.0.0](#what-is-new-in-300)
- [Screenshots](#screenshots)
- [Downloads and Installation](#downloads)
  - [Android](#android)
  - [Desktop (Windows, Linux, MacOS)](#desktop)
- [Version Comparison](#version-comparison)
- [Contributions](#contributions)
- [For contributors and AI coding agents](#for-contributors-and-ai-coding-agents)
- [Technical Details](#technical-details)
- [Credits](#credits)

## About this app

Practice writing Japanese letters, learn their meanings and related words

### Features

- Memorize how to write and read kana and kanji
- Follow JLPT levels or school grades
- Create your own decks to study, more than 6000 characters in total are available
- Use SRS reviewing system to avoid forgetting learned information
- Search letters and words with built-in dictionary
- Study words with flashcards
- Available modes to write words and pick correct letter readings
- Write personal notes for each character, shared across decks and available in practice and character info
- Back up study data locally and sync through Kanji Jojo
- Works offline

Repository with dictionary data and parsers here: [Kanji-Dojo-Data](https://github.com/RudrakshKashyap/Kanji-Jojo-Data)

## Migrate from Kanji Dojo

**Export from Kanji Dojo → import into Kanji Jojo → sync from Kanji Jojo.**

1. In **Kanji Dojo**, open **Settings → Backup & Restore → Create backup**. Save the exported ZIP somewhere you can access outside the app, and keep it until migration is complete.
2. Install **Kanji Jojo 3.0.0 or newer** from [this project's releases](https://github.com/RudrakshKashyap/Kanji-Jojo/releases).
3. In **Kanji Jojo**, open **Settings → Backup & Restore → Restore from backup**, select the exported ZIP, and confirm **Restore**. Restoring replaces any study data already in Kanji Jojo; export that data first if you need to keep it.
4. Check that your decks and review progress are present in Kanji Jojo.
5. **Only after importing**, sign in from Kanji Jojo's account screen and sync the imported data. If a sync conflict appears, choose the imported **local data** to upload.

Kanji Dojo accounts and cloud backups do **not** transfer automatically. Signing in to the website alone does not import or upload study data. Local study and backup/restore work without an account; sync is optional.

The [website migration guide](https://kanji-jojo.com/migration) walks through the same steps.

## What is new in 3.0.0

- **My notes:** add up to 500 characters of personal notes to each kana or kanji. Notes are shared across decks and editable on the character info page and during practice.
- **Reveal when you need it:** notes stay hidden on the practice question until you choose Reveal, and appear automatically after revealing the answer.
- **Autosave, backup, and sync:** notes save locally and travel with your study data through existing backups and sync.
- **A new home for accounts:** updated builds connect to Kanji Jojo's own sign-in and sync service, separately from the original service.

This is the next step in continuing Kanji Dojo, with more improvements to come. See the [3.0.0 release notes](release-notes/v3.0.0.md) for details and share ideas through [GitHub issues](https://github.com/RudrakshKashyap/Kanji-Jojo/issues).

## Screenshots

<p float="left">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="400"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="400"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="400"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" height="400"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" height="400"/>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" height="400"/>
</p>

## Downloads

### Android

Download an APK from [Kanji Jojo's GitHub releases](https://github.com/RudrakshKashyap/Kanji-Jojo/releases). The release workflow provides the F-Droid-flavor APK.

The original Kanji Dojo listings on Google Play and F-Droid belong to the original app. They are not download links for this continuation; use this repository's releases for Kanji Jojo.

### iOS

The iOS source is included in this repository. Check the release assets for published builds; the existing Kanji Dojo App Store listing belongs to the original app.

### Desktop

#### Windows

- Download `.msi` installer from [Github Releases](https://github.com/RudrakshKashyap/Kanji-Jojo/releases)

#### Mac

1. Download `.dmg` installer from [Github Releases](https://github.com/RudrakshKashyap/Kanji-Jojo/releases) for your platform
   - `Kanji Jojo-arm-*.dmg` for Apple Silicon
   - `Kanji Jojo-intel-*.dmg` for devices with older Intel CPU
2. Install the app
   - Go to `Privacy & Security` screen in system settings and click on `Open Anyway` button under security section for `Kanji Jojo.app`

#### Linux

- Download `.AppImage` executable from [Github Releases](https://github.com/RudrakshKashyap/Kanji-Jojo/releases)

## Version comparison

| Google Play                                                             | F-Droid | Desktop |
| ----------------------------------------------------------------------- | ------- | ------- |
| Firebase Analytics and Crashlytics </br>Play services for in app review | -       | -       |

## Contributions

- Pull-Requests are welcome!
- Before making PR create and discuss your issue
- Use proper code formatting

## For contributors and AI coding agents

Start with **[AGENTS.md](AGENTS.md)**, the shared repository guide, then read
the **[OKF operating manual](okf/AGENTS.md)** and **[knowledge index](okf/index.md)**
before exploring code or answering project questions. The [`okf/`](okf/)
knowledge base records architecture, module responsibilities, the data layer,
build setup, and the SRS engine, with space for feature knowledge, conventions,
and playbooks. The guide explains when and how to keep it current.

Agent entry files are `AGENTS.md` for Codex, `CLAUDE.md` for Claude, and
`GEMINI.md` for Gemini. The latter two point to the shared guide so instructions
stay in one place. For other agents, or a session opened outside this repository,
explicitly ask the agent to read this repository's `AGENTS.md` first.

### Technical details

Used Libraries: `Compose Multiplatform`, `Kotlin Coroutines`, `Koin`, `SqlDelight`

### Credits

- **KanjiVG**</br>
  Provides writing strokes, radicals information </br>
  License: Creative Commons Attribution-Share Alike 3.0</br>
  Link: https://kanjivg.tagaini.net/
- **Kanji Dic**</br>
  Provides characters info, such as meanings, readings and classifications </br>
  License: Creative Commons Attribution-Share Alike 3.0</br>
  Link: http://www.edrdg.org/wiki/index.php/KANJIDIC_Project
- **Tanos by Jonathan Waller**</br>
  Provides JLPT classification for kanji </br>
  License: Creative Commons BY</br>
  Link: http://www.tanos.co.uk/jlpt/
- **JMDict**</br>
  Japanese-Multilingual dictionary, provides expressions </br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://www.edrdg.org/jmdict/j_jmdict.html
- **JmdictFurigana**</br>
  Open-source furigana resource to complement the EDICT/Jmdict and ENAMDICT/Jmnedict dictionary files </br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://github.com/Doublevil/JmdictFurigana
- **Frequency list by Leeds university**</br>
  Words ranking by frequency of usage in internet </br>
  License: Creative Commons BY</br>
  Link: http://corpus.leeds.ac.uk/list.html
- **yomichan-jlpt-vocab**</br>
  This meta dictionary adds JLPT-level tags to words in Yomichan. Provides associations between JLPT words from Tanos and JMDict</br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://github.com/stephenmk/yomichan-jlpt-vocab</br>

### License

> (c) 2022-2023 Yaroslav Shuliak
>
> This is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
>
> This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
>
> You should have received a copy of the GNU General Public License along with this app. If not, see https://www.gnu.org/licenses/.
