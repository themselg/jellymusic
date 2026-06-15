# JellyMusic

A native **Android** music client for [Jellyfin](https://jellyfin.org), built entirely with
**Kotlin + Jetpack Compose + Material 3** (no React Native / cross-platform UI frameworks).
Inspired by [Findroid](https://github.com/jarnedemeulemeester/findroid), but focused on audio.

## Build & run

Requirements: **Android Studio** (Ladybug or newer) with **JDK 17** and the Android SDK
(compileSdk 35, minSdk 31).

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

Single Gradle module (`:app`) with packages mirroring a clean multi-module layout, so it can be
split later. **MVVM + repositories**, Hilt DI, coroutines/Flow, type-safe Navigation Compose.

```
dev.themselg.jellymusic
├─ domain/            # models (Album/Artist/Song/Playlist) + repository interfaces
├─ data/
│  ├─ session/        # SessionManager + JellyfinUrls (auth, encrypted creds, URL builders)
│  ├─ repository/     # Jellyfin SDK-backed repository implementations + DTO mappers
│  └─ prefs/          # ThemePreferences (DataStore)
├─ player/            # PlayerController + Media3 MusicService (MediaLibraryService)
├─ ui/
│  ├─ theme/          # AppTheme (3 color modes) + SeedColorExtractor + AlbumColorThemeController
│  ├─ navigation/     # type-safe routes
│  ├─ components/     # CoverArt, SongRow, cards, MiniPlayer, state views
│  └─ feature/        # login, library, detail, search, player, settings (screen + ViewModel each)
└─ di/                # Hilt modules (AppModule, DataModule, PlayerModule)
```

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
3. Add a row to the **Language** picker in `ui/feature/settings/SettingsScreen.kt` (one line in the
   `options` list + a `language_<code>` endonym string).

Language can be changed in-app (Settings → *Idioma/Language*) or, on Android 13+, from the system
Settings → Apps → JellyMusic → Language. Switching is handled by
`AppCompatDelegate.setApplicationLocales` (which is why `MainActivity` is an `AppCompatActivity` and
the base theme parents `Theme.AppCompat.DayNight.NoActionBar`).
