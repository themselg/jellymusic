# JellyMusic

A native **Android** music client for [Jellyfin](https://jellyfin.org), built entirely with
**Kotlin + Jetpack Compose + Material 3** (no React Native / cross-platform UI frameworks).
Inspired by [Findroid](https://github.com/jarnedemeulemeester/findroid), but focused on audio.

## Build & run

Requirements: **Android Studio** (Ladybug or newer, which bundles **JBR 21**) with **JDK 21** and
the Android SDK (compileSdk 35, minSdk 31).

1. **Open the project in Android Studio** (`File → Open` → this folder). Android Studio will
   generate the Gradle wrapper jar and create `local.properties` with your SDK path automatically.
2. Let Gradle sync, then **Run** the `app` configuration on an emulator/device running
   **Android 12 (API 31) or newer**.

> **Note on the Gradle wrapper:** `gradle/wrapper/gradle-wrapper.jar` is a binary and is **not**
> included here. Android Studio regenerates it on import. To build from the command line first run
> `gradle wrapper --gradle-version 8.11.1` (needs a system Gradle once), after which `./gradlew
> assembleDebug` works.

A public demo server is available for testing: `https://demo.jellyfin.org/stable`
(user `demo`, no password).

## Architecture

Multi-module Gradle build: **core layers** + **one module per feature**, with the app module as a
thin shell. **MVVM + repositories**, Hilt DI, coroutines/Flow, type-safe Navigation Compose. Shared
build config lives in convention plugins under `build-logic/` (applied as `jellymusic.*`).

```
:app                     # Application, MainActivity, nav scaffold (NavHost), AppModule
:core
├─ :core:domain          # pure Kotlin: models (Album/Artist/Song/Playlist) + repository interfaces
├─ :core:data            # Jellyfin SDK repos + DTO mappers, session (encrypted creds), DataStore prefs,
│                        #   offline downloads (Media3), data-layer Hilt modules
├─ :core:player          # PlayerController + Media3 MusicService (MediaLibraryService) + Cast
└─ :core:ui              # AppTheme (3 color modes) + dynamic color, shared components (SongRow, cards,
│                        #   MiniPlayer, CollectionHeader, AddToPlaylistSheet), routes, all UI strings
:feature
├─ :feature:login        # each feature = its own module, depends only on :core:* (no feature→feature)
├─ :feature:home
├─ :feature:library      # library, liked songs, downloads
├─ :feature:detail       # album / artist / playlist detail
├─ :feature:player       # now playing, queue, lyrics
├─ :feature:search
└─ :feature:profile      # profile + settings (theme, language, sign out)
```

Dependencies flow one way: `:feature:* → :core:ui → :core:player → :core:data → :core:domain`, and
`:app` wires everything together. Kotlin packages stay under `dev.themselg.jellymusic.*`.

Key libraries: Jellyfin Kotlin SDK (`org.jellyfin.sdk`), AndroidX Media3, Coil 3, MaterialKolor,
Hilt, DataStore, `androidx.security.crypto`. Versions are pinned in `gradle/libs.versions.toml`.

## Internationalization (i18n)

All user-facing text lives in `res/values/strings.xml` (the default/English copy). The app ships
**English** and **Spanish** (`res/values-es/strings.xml`).

To add a language:
1. Create `res/values-<code>/strings.xml` (e.g. `values-fr`, `values-pt-rBR`) and translate every
   `<string>`/`<plurals>`. Leave entries marked `translatable="false"` (`app_name`, language
   endonyms) untranslated.
2. Add the locale to `res/xml/locales_config.xml` (powers the system per-app language picker on
   Android 13+).
3. Add a row to the **Language** picker (`LanguageSection`) in
   `feature/profile/.../ProfileScreen.kt` (one line in the `options` list + a `language_<code>`
   endonym string).

Language can be changed in-app (Profile → *Idioma/Language*) or, on Android 13+, from the system
Settings → Apps → JellyMusic → Language. Switching is handled by
`AppCompatDelegate.setApplicationLocales` (which is why `MainActivity` is an `AppCompatActivity` and
the base theme parents `Theme.AppCompat.DayNight.NoActionBar`).

## Acknowledgements

- [Jellyfin](https://jellyfin.org)
- [Jellyfin Kotlin SDK](https://github.com/jellyfin/jellyfin-sdk-kotlin)
- [Coil](https://github.com/coil-kt/coil)
- [OkHttp](https://github.com/square/okhttp)
- [MaterialKolor](https://github.com/jordond/MaterialKolor)
- [WavySlider](https://github.com/mahozad/wavy-slider)

## License

Copyright © 2026 Guillermo Themsel.

JellyMusic is free software: you can redistribute it and/or modify it under the terms of the
**GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version** (`GPL-3.0-or-later`). See [`LICENSE`](LICENSE) for
the full text.

It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

The **`libre`** build flavor contains only GPL-compatible, open-source dependencies and is suitable
for FOSS distribution (e.g. F-Droid). The **`proprietary`** flavor additionally links Google Cast
(Google Play Services), which is not free software — distribute that flavor accordingly.
