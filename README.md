# Crónicas de la Marca

A digital character sheet for *Crónicas de la Marca del Este* (2022 rulebook). Dice are rolled with real dice at the
table; the app keeps the sheet and fills in everything the rules derive.

## Features

- **Characters:** home screen of portrait cards; step-by-step creation wizard with illustrated carousels from the rulebook (name and portrait, species, class with
  optional multiclass, 3d6 scores and primaries, HP, alignment and deity, starting gold); export and import a
  character (portrait included) as a `.cronicas.json` file.
- **Rules, auto-filled and editable:** the 13 classes and 8 species of the rulebook, the Armónico de Hueso from the
  module *Agitando Pozasdemugre* (B19), plus custom ones, give hit die,
  BA by level, XP thresholds, class and species abilities, species modifiers, movement and languages. Derived values
  can be fixed by hand; a pencil marks overridden values.
- **Resumen:** HP (±1), CA with breakdown, BA, hit die, movement, attacks per weapon (`BA +X · 1d8+FUE`, melee,
  thrown and ranged), saves and checks as `bonus · ND 12/18`, conditions and rule warnings (forbidden weapons or armour,
  alignment, load).
- **Equipo:** full weapon, ammunition, armour, shield, helmet and gear catalog with prices (optionally paid from your
  coins); containers with capacity; mounts and companions that carry load; load bands and their penalties.
- **Magia:** spell slots from the class tables plus bonus spells; known and prepared spells with "used" checkboxes and
  a rest button; catalog of the 371 rulebook spells for Clérigo, Druida, Ilusionista and Mago, CSV import and custom spells.
- **Campaigns:** group characters on the home screen (long-press a card → *Mover a campaña…*). Sections can be
  collapsed, renamed or dissolved. Campaigns are local to each device and are not synced or exported.
- **Sync between devices:** a character can be synced through Firebase (long-press its card → *Sincronizar*). The
  app shows a code; enter it on another device with *Vincular personaje* and both keep the same sheet, live, last
  change wins. Works offline and catches up when back online. See [Character sync](#character-sync).
- **Themes:** Pergamino oscuro, Pergamino claro, Material You, or *Desde retrato* (each sheet takes its colors from the
  character's portrait), with system, light or dark mode.

## Origin of this source code

The original source code was not available. This project was **reconstructed by decompiling `app-debug.apk`**
(versionName `0.1.0`, original package `com.example.cronicasmarca`, now `io.github.pauverblom.cronicas`):

1. The APK was decompiled with [jadx](https://github.com/skylot/jadx). Resources and assets
   (`ic_launcher.xml`, `reglas.json`) were copied as-is.
2. The decompiled Java/bytecode was rewritten by hand into idiomatic Kotlin + Jetpack Compose. That
   covered the Room entities and DAOs, repository, rules, undo system, ViewModel and every screen.
   Ambiguous branches and default arguments were checked against the raw Dalvik bytecode.
3. Library versions were taken from the `META-INF/*.version` files and the Kotlin metadata inside the APK.
4. All sources were type-checked with the Kotlin 2.1.0 compiler (with the Compose and serialization plugins)
   against the exact library classes bundled in the APK.

The Room schema matches the original (DB `cronicas_marca.db`, version 3, same table SQL and
identity hash).

The result is **functionally equivalent but not byte-identical** to the original. Comments, local variable
names and some layout details were lost in compilation and have been reconstructed or renamed.

## Building

Requirements: Android Studio (Ladybug or newer) / JDK 17+, Android SDK 35.

```bash
./gradlew assembleDebug      # APK in app/build/outputs/apk/debug/
./gradlew installDebug       # install on a connected device
```

Or open the folder in Android Studio and press *Run*.

Stack: Kotlin 2.1.0, AGP 8.7.3, Gradle 8.11.1, Jetpack Compose (BOM 2024.12.01, Material 3), Navigation Compose,
Room 2.6.1 (KSP), DataStore, kotlinx-serialization 1.7.3, Coil 3, MaterialKolor, Android Image Cropper. `minSdk` 26, `targetSdk` 35.

> **Note:** the app id changed from `com.example.cronicasmarca` to `io.github.pauverblom.cronicas`, so this app
> installs *next to* the original APK, not over it. Its data doesn't carry over. Uninstall the old one once you've moved over.

## Installing with Obtainium

1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. *Add App* → paste `https://github.com/pauverblom/cronicas-app` → *Add*.
3. Install. Obtainium will notify you when a new release is published.

## Publishing a release

Releases are built and signed by GitHub Actions (`.github/workflows/release.yml`). To publish one:

```bash
git checkout main && git pull
git tag v0.2.0          # vMAJOR.MINOR.PATCH, minor and patch < 100
git push origin v0.2.0
```

The workflow builds `cronicas-marca-v0.2.0.apk`, signs it with the release key and attaches it to a GitHub Release.
`versionName` comes from the tag (`0.2.0`), and `versionCode` is `MAJOR*10000 + MINOR*100 + PATCH`. Tags must
therefore always increase. Android refuses to install an update with a lower `versionCode`.

The signing key lives only in the repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and
`KEY_PASSWORD` if the key has its own password) and in
the maintainer's offline backup. **If it is lost, existing installs can't be updated.** Everyone would have to
uninstall (losing their data) and reinstall.

To (re)set the secrets without copy-pasting, use the [GitHub CLI](https://cli.github.com/) (works in Termux:
`pkg install gh`):

```bash
gh auth login
base64 -w0 cronicas-release.jks | gh secret set KEYSTORE_BASE64 -R pauverblom/cronicas-app
gh secret set KEYSTORE_PASSWORD -R pauverblom/cronicas-app   # prompts for the value
gh secret set KEY_ALIAS -R pauverblom/cronicas-app --body cronicas
```

If a release fails at *Verify signing key*, its log prints the decoded keystore's size and sha256. Compare them with
`wc -c cronicas-release.jks` and `sha256sum cronicas-release.jks`.

Pull requests and pushes to `main` run `.github/workflows/ci.yml`, which runs the rules unit tests and builds the debug APK.

## Web version (iPhone, desktop)

The same app runs in the browser at **https://pauverblom.github.io/cronicas-app/**, built with Compose
Multiplatform for Kotlin/Wasm. It needs a recent browser (Safari 18.2+ on iOS, Chrome/Edge 119+, Firefox 120+).
On an iPhone, open it in Safari and use *Share → Add to Home Screen* to get it as an app.

It shares the code of the Android app: the `web/` module compiles the rules, the data model and every screen straight
from `app/src/main/java` (the list is in `web/build.gradle.kts`). Only the Android-specific pieces have web
replacements in `web/src/wasmJsMain`, with the same names and API: storage (`data/Almacen.kt` and
`data/Repository.kt` keep each character as its export snapshot in the browser's `localStorage`), files, portraits,
fonts and Firebase (`resources/nube.js`, using the Firebase JS SDK on the same Firestore documents). So a web
character can be exported to an Android device and vice versa, and sync works between Android and the web.

Differences from Android:

- Characters live in that browser's storage. Clearing site data deletes them, so sync or export the ones you care about.
- Portraits are center-cropped automatically (there is no crop screen); camera and gallery are the browser's image picker.
- The browser's back button doesn't move between screens; use the app's own back arrows.

```bash
./gradlew :web:wasmJsBrowserDevelopmentRun    # dev server with the app
./gradlew :web:wasmJsBrowserDistribution      # static site in web/build/dist/wasmJs/productionExecutable/
```

The `Web` workflow (`.github/workflows/web.yml`) publishes the site to GitHub Pages on every push to `main`.
Setup, once: *Settings → Pages → Build and deployment → Source: **GitHub Actions***.

## Character sync

Each synced character is one Firestore document, `personajes/{code}`, holding the same snapshot as the export
(`Backup.PersonajeExportado`) as a JSON string. The portrait is a separate document,
`personajes/{code}/retrato/actual`, uploaded only when it changes. Devices sign in anonymously; the 12-character
random code is the only key, so anyone with the code can read and edit that character.

`data/sync/Sincronizador.kt` uploads a character whenever its tables change (debounced) and applies remote changes
by rewriting the local sheet in place (`Backup.escribir`). To avoid echoes, both directions compare a normalized
form of the sheet (`data/sync/Canonica.kt`) that ignores local database ids. Which characters are linked is stored
in `SharedPreferences`, so the database schema is unchanged.

Deleting a synced character only removes it from that device; the cloud copy and other devices keep it.

### Setting up Firebase (once)

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com) (the free Spark plan is enough).
2. *Add app → Android*, package name `io.github.pauverblom.cronicas`. Download `google-services.json`.
3. *Build → Authentication → Get started → Sign-in method*: enable **Anonymous**.
4. *Build → Firestore Database → Create database* (production mode, any location), then *Rules*: paste
   [`firestore.rules`](firestore.rules) and *Publish*.
5. Store the config as a repository secret (it is not committed):

   ```bash
   gh secret set GOOGLE_SERVICES_JSON -R pauverblom/cronicas-app < google-services.json
   ```

6. For the web version: *Project settings → General → Your apps → Add app → Web* (no hosting needed), copy the
   `firebaseConfig` object it shows, and store it as a secret too (JSON or the JS snippet both work):

   ```bash
   gh secret set FIREBASE_WEB_CONFIG -R pauverblom/cronicas-app    # paste it, then Ctrl-D
   ```

   Then add `pauverblom.github.io` under *Authentication → Settings → Authorized domains*.

The release workflow writes the secret to `app/src/release/google-services.json`. Builds without it (debug builds,
pull requests, forks) compile normally and simply don't show the sync options. To try sync in a local debug build,
also register the app `io.github.pauverblom.cronicas.debug` in Firebase and put its `google-services.json` in
`app/src/debug/`.

## Project layout

```
app/src/main/java/io/github/pauverblom/cronicas/
├── CronicasApp.kt          Application: DB, repository and settings; seeds the spell catalog on first launch
├── MainActivity.kt
├── data/                   Room entities and DAOs, repository, settings (DataStore), export/import (Backup.kt)
│   └── sync/               Firebase sync of characters between devices
├── rules/                  Rules engine: Hoja.kt computes the whole derived sheet; Reglas.kt has progression,
│   │                       saves, load, spell slots and coins
│   └── data/               Rulebook data: classes, species, alignments, item catalog
└── ui/
    ├── home/               Character gallery
    ├── wizard/             Creation wizard
    ├── hoja/               Sheet tabs: Resumen, Personaje, Equipo, Magia, Notas
    ├── ajustes/            Theme and mode
    ├── foto/               Portrait picking, cropping and color extraction
    ├── common/             Shared components
    └── theme/              Color schemes and typography
```

`app/src/main/assets/conjuros.json` is generated from the rulebook PDF with `scripts/extract_spells.py`
(needs poppler's `pdftotext`):

```bash
python3 scripts/extract_spells.py LORE/Cronicasdelamarca.pdf app/src/main/assets/conjuros.json
```

The class, species and equipment illustrations in `app/src/main/assets/ilustraciones/` come from the rulebook and from
the red box (*Aventuras en la Marca del Este*, `LORE/caja_roja.pdf`). Each catalog item gets its own picture, cut out of
the equipment plates along its own strokes. Anything the books don't draw shows a red **MISSING PICTURE** card
with the file name it expects (e.g. `objeto_estoque.webp`); drop a `.webp` with that name in the folder to fill it:

```bash
python3 scripts/extract_ilustraciones.py LORE/Cronicasdelamarca.pdf LORE/caja_roja.pdf app/src/main/assets/ilustraciones
```

`web/` is the browser build of the same code (see [Web version](#web-version-iphone-desktop)).

Rules tests live in `app/src/test` (`./gradlew testDebugUnitTest`).

## Known issues

- The database uses `fallbackToDestructiveMigration()`. Changing an entity and bumping the version **wipes all data**.
  Write a real `Migration` before changing the schema (version 4 deliberately discarded the old companion-app data).
- Class ability descriptions are short summaries, not the rulebook text. Edit them on the sheet if you need more.

## Contributing

Work on a branch and open a pull request. Don't commit APKs, keystores or `local.properties`.
