# Translations

JellyMusic ships these UI languages. **English** and **Spanish** are author-maintained. The rest
were **machine-assisted** and are marked for **native-speaker review** — they are complete and build
correctly, but a fluent speaker should confirm wording/tone before treating them as polished.

Source of truth: `core/ui/src/main/res/values/strings.xml`. Each translation lives in
`core/ui/src/main/res/values-<code>/strings.xml`. To add a language see the README (i18n section).

| Language | Code | Dir | Status |
|---|---|---|---|
| English | en | `values` | ✅ author |
| Spanish | es | `values-es` | ✅ author |
| German | de | `values-de` | 🟡 needs native review |
| French | fr | `values-fr` | 🟡 needs native review |
| Italian | it | `values-it` | 🟡 needs native review |
| Dutch | nl | `values-nl` | 🟡 needs native review |
| Polish | pl | `values-pl` | 🟡 needs native review |
| Portuguese (Brazil) | pt-BR | `values-pt-rBR` | 🟡 needs native review |
| Portuguese (Portugal) | pt-PT | `values-pt-rPT` | 🟡 needs native review |
| Russian | ru | `values-ru` | 🟡 needs native review |
| Ukrainian | uk | `values-uk` | 🟡 needs native review |
| Turkish | tr | `values-tr` | 🟡 needs native review |
| Simplified Chinese | zh-CN | `values-zh-rCN` | 🟡 needs native review |
| Japanese | ja | `values-ja` | 🟡 needs native review |
| Korean | ko | `values-ko` | 🟡 needs native review |

## Cross-cutting items to review in every language

- **"Cast" wording** — most languages have both an anglicism and an official term (e.g. de
  *Streamen* vs *Casten*, fr *Caster* vs *Diffuser*, ru *Трансляция*, zh *投放*). Pick what matches
  Google Cast's localized branding.
- **"Liked songs" vs "Favorites"** — the app uses **two** distinct concepts (`liked_songs` and
  `favorite`/`tab_favorites`). Several languages collapse them to one word by default; confirm the
  distinction reads clearly (or is intentionally merged).
- **`songs_count` ("%1$d songs")** — this is a *static* string (album/artist detail), not a plural,
  so in languages with case/number agreement (ru, uk, pl) it can't agree with every number; the
  translators used the most common form. Consider migrating it to a `plurals` resource later. The
  real `song_count` plural *is* correct.
- **"Brand" color mode (`color_mode_static`)** — translated literally in most languages; verify it
  reads well as a theme-mode label.
- **"Material You"** and **"AMOLED"** kept untranslated (product/standard terms).

## Per-language notes

### German (de)
- `liked_songs`/`stat_liked` → "Lieblingssongs"; confirm against "Favoriten" usage.
- `cast` → "Streamen" — confirm vs "Casten"/"Übertragen".
- `shuffle` → "Zufallswiedergabe" (long — check button width); `home` → "Start".
- Uses informal **du** register throughout — confirm that's the desired tone (vs formal *Sie*).

### French (fr)
- "Titres" vs "Morceaux" for songs — pick one consistently.
- `cast` → "Caster" (anglicism) vs official "Diffuser".
- "Playlist" kept as-is vs "Liste de lecture".
- Many apostrophes escaped `\'` — quick read-through advised.

### Italian (it)
- `cast` → "Trasmetti" (Italian Cast UI sometimes keeps "Cast").
- `shuffle` → "Casuale"; `top_songs` → "Brani principali" (vs "più ascoltati").
- `color_mode_static` "Brand" → left literal; "Marchio" alternative.

### Dutch (nl)
- `liked_songs` → "Leuk gevonden nummers" (Spotify NL style); confirm vs "Vind-ik-leuks".
- `shuffle` → "Willekeurig" vs keeping "Shuffle".
- `color_mode_static` "Brand" → "Merk" reads oddly; consider keeping "Brand".

### Polish (pl)
- **Plural forms** (one/few/many/other) for "utwór" — Polish plurals are tricky, verify grammar.
- "song/track" rendered "utwór"; `cast` → "Przesyłaj"; `shuffle` → "Losowo".
- "Playlist" anglicism vs native "lista odtwarzania".

### Portuguese — Brazil (pt-BR)
- BR vocabulary: "Baixar" (download), "Músicas curtidas" (liked), "Transmitir" (cast), "Playlist".
- Confirm these vs the pt-PT file so the two stay distinct.

### Portuguese — Portugal (pt-PT)
- PT vocabulary: "Transferências" (download), "Aspeto" (appearance), European "estar a + infinitivo".
- `liked_songs` → "Faixas gostadas" is slightly awkward — consider "Favoritas".
- "Faixas" vs "Músicas" for songs — confirm consistency.

### Russian (ru)
- **Plural forms** (one/few/many/other) for "песня" — verify grammar.
- Mixed "трек" (tabs) vs "песня" (plural) — consider unifying.
- `cast` → "Трансляция / Транслировать".

### Ukrainian (uk)
- **Plural forms** (one/few/many/other) for "пісня" — verify; "трек" is an alternative unit.
- "Уподобані" vs "Вподобані" for liked — confirm form.

### Turkish (tr)
- `cast` → "Yayınla"; `library` → "Kitaplık" (vs "Kütüphane").
- Suffix vowel-harmony on "JellyMusic" assumed (e.g. "JellyMusic\'in") — native may adjust.

### Simplified Chinese (zh-CN)
- `cast` → 投放 (vs 投射); song measure word 首.
- 艺术家 (artist) vs 歌手; 收藏 (favorite) vs 我喜欢的歌曲 (liked) — confirm distinction.

### Japanese (ja)
- `cast` → キャスト (katakana). "liked"/"favorite" both → お気に入り (collapsed) — confirm.
- `top_songs` → 人気の曲; profile stat labels may need shorter forms for narrow columns.

### Korean (ko)
- `cast` → 캐스트 (vs 전송). `liked_songs` → 좋아요 표시한 곡 (vs shorter 좋아하는 곡 for stats).
- 즐겨찾기 (favorites) vs 좋아요 (liked) — confirm the two concepts stay distinct.

## Contributing fixes

Edit the relevant `values-<code>/strings.xml` and open a PR, keeping every `name=` and the `%1$d`
placeholders intact. For scaling this up, the project is a good fit for a
[Weblate](https://weblate.org) instance (as Jellyfin itself uses) so native speakers can review
without touching the repo directly.
