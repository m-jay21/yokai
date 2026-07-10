<div align="center">

<a href="https://github.com/m-jay21/yokai">
    <img src="./.github/readme-images/app-icon.webp" alt="Yokai logo" height="200px" width="200px" />
</a>

# Yōkai

</div>

<div align="center">

A free and open source manga reader — fork with custom **Folders** for multi-series collections

[![License: Apache-2.0](https://img.shields.io/github/license/m-jay21/yokai?labelColor=27303D&color=0877d2)](/LICENSE)
[![Upstream: null2264/yokai](https://img.shields.io/badge/upstream-null2264%2Fyokai-blue.svg?labelColor=27303D)](https://github.com/null2264/yokai)

<img src="./.github/readme-images/screens.gif" alt="Yokai screenshots" />

## Download

Build from source, or install a local variant such as **YōkaiJ** (`./gradlew :app:installStandardYokaij`).

*Requires Android 6.0 or higher.*

## About this fork

Personal fork of [null2264/yokai](https://github.com/null2264/yokai) focused on **Folders**: group chapters from multiple series into one place, reorder them freely, and manage them with a details UI that matches the series page (including cover-based theming and tablet layout).

Also includes folder backup/restore, a **YōkaiJ** side-by-side build variant (release-style, no reader debug overlay), and a Glance widget crash fix on some Samsung devices.

## Features

<div align="left">

<details open="">
    <summary><h3>From this fork</h3></summary>

* **Folders** — create collections of chapters across different series.
* Folder details screen aligned with series details (cover palette theming, tablet split layout).
* Manual chapter reorder with optional free-movement drag handles.
* Folder metadata (name, description, author/artist, tags, custom cover).
* Folders included in create/restore backup.
* **YōkaiJ** build type for a release-style install next to stock Yōkai (`eu.kanade.tachiyomi.yokaij`).
* Fix for Glance widget startup crash (`SecurityException` on some devices).

</details>

<details open="">
    <summary><h3>From Yōkai (upstream)</h3></summary>

* NSFW/SFW library filter (taken from [TachiyomiSY](https://github.com/jobobby04/TachiyomiSY)).
* Fix backup incompatibility with upstream.
* New theme.
* Local Source chapters now reads ComicInfo.xml for chapter title, number, and scanlator.

</details>

<details open="">
    <summary><h3>From Tachiyomi / Mihon</h3></summary>

* Local reading of downloaded content.
* A configurable reader with multiple viewers, reading directions and other settings.
* Tracker support:
  [MyAnimeList](https://myanimelist.net/),
  [AniList](https://anilist.co/),
  [Kitsu](https://kitsu.app/explore/anime),
  [Manga Updates](https://www.mangaupdates.com/),
  [Shikimori](https://shikimori.one),
  and [Bangumi](https://bgm.tv/) support.
* Categories to organize your library.
* Light and dark themes.
* Schedule updating your library for new chapters.
* Create backups locally to read offline or to your desired cloud service.

</details>

<details>
    <summary><h3>From J2K</h3></summary>

* UI redesign.
* New Manga details screens, themed by their manga covers.
* Combine 2 pages while reading into a single one for a better tablet experience.
* An expanded toolbar for easier one handed use (with the option to reduce the size back down).
* Floating searchbar to easily start a search in your library or while browsing.
* Library redesigned as a single list view: See categories listed in a vertical view, that can be collasped or expanded with a tap.
* Staggered Library grid.
* Drag & Drop Sorting in Library.
* Dynamic Categories: Group your library automatically by the tags, tracking status, source, and more.
* New Recents page: Providing quick access to newly added manga, new chapters, and to continue where you left on in a series.
* Stats Page.
* New Themes.
* Dynamic Shortcuts: open the latest chapter of what you were last reading right from your homescreen.
* [New material snackbar](.github/readme-images/material%20snackbar.png): Removing manga now auto deletes chapters and has an undo button in case you change your mind.
* Batch Auto-Source Migration (taken from [TachiyomiEH](https://github.com/NerdNumber9/TachiyomiEH)).
* [Share sheets upgrade for Android 10](.github/readme-images/share%20menu.png)
* View all chapters right in the reader.
* A lot more Material Design You additions.
* Android 12 features such as automatic extension and app updates.

</details>

</div>

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

<div align="left">

<details><summary>Issues</summary>

**Before reporting a new issue, take a look at the [FAQ](https://mihon.app/docs/faq/general) and the already opened [issues](https://github.com/m-jay21/yokai/issues).**

</details>

<details><summary>Bugs</summary>

* Include version (**Settings → About → Version**).
  * If not latest, try updating, it may have already been solved.
* Include steps to reproduce (if not obvious from description).
* Include screenshot (if needed).
* If it could be device-dependent, try reproducing on another device (if possible).
* For large logs use [Pastebin](https://pastebin.com/) (or similar).
* Don't group unrelated requests into one issue.

</details>

<details><summary>Feature Requests</summary>

* Write a detailed issue, explaining what it should do or how.
  * Avoid writing just "like X app does"
* Include screenshot (if needed).

</details>

</div>

### Credits

Based on [null2264/yokai](https://github.com/null2264/yokai). Thank you to everyone who has contributed upstream.

<a href="https://github.com/null2264/yokai/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=null2264/yokai" alt="Yokai app contributors" title="Yokai app contributors" width="600"/>
</a>

### Disclaimer

The developer(s) of this application does not have any affiliation with the content providers available, and this application hosts zero content.

### License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 null2264

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
</div>
