# Crónicas de la Marca

A digital character sheet for *Crónicas de la Marca del Este* (2022 rulebook). Dice are rolled with real dice at the
table; the app keeps the sheet and fills in everything the rules derive.

## Features

- **Characters:** home screen of portrait cards; step-by-step creation wizard with illustrated carousels from the rulebook (name and portrait, species, class with
  optional multiclass, 3d6 scores and primaries, HP, alignment and deity, starting gold); export and import a
  character (portrait included) as a `.cronicas.json` file.
- **Rules, auto-filled and editable:** the 13 classes and 8 species of the rulebook (plus custom ones) give hit die,
  BA by level, XP thresholds, class and species abilities, species modifiers, movement and languages. Derived values
  can be fixed by hand; a pencil marks overridden values.
- **Resumen:** HP (±1), CA with breakdown, BA, hit die, movement, attacks per weapon (`BA +X · 1d8+FUE`, melee,
  thrown and ranged), saves and checks as `bonus · ND 12/18`, conditions and rule warnings (forbidden weapons or armour,
  alignment, load).
- **Equipo:** full weapon, ammunition, armour, shield, helmet and gear catalog with prices (optionally paid from your
  coins); containers with capacity; mounts and companions that carry load; load bands and their penalties.
- **Magia:** spell slots from the class tables plus bonus spells; known and prepared spells with "used" checkboxes and
  a rest button; catalog of the 371 rulebook spells for Clérigo, Druida, Ilusionista and Mago, CSV import and custom spells.
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

## Project layout

```
app/src/main/java/io/github/pauverblom/cronicas/
├── CronicasApp.kt          Application: DB, repository and settings; seeds the spell catalog on first launch
├── MainActivity.kt
├── data/                   Room entities and DAOs, repository, settings (DataStore), export/import (Backup.kt)
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

The class, species and equipment illustrations in `app/src/main/assets/ilustraciones/` come from the same PDF
(armour, shields and helmets are cut out of their labelled plates; other items show their category plate):

```bash
python3 scripts/extract_ilustraciones.py LORE/Cronicasdelamarca.pdf app/src/main/assets/ilustraciones
```

Rules tests live in `app/src/test` (`./gradlew testDebugUnitTest`).

## Known issues

- The database uses `fallbackToDestructiveMigration()`. Changing an entity and bumping the version **wipes all data**.
  Write a real `Migration` before changing the schema (version 4 deliberately discarded the old companion-app data).
- Class ability descriptions are short summaries, not the rulebook text. Edit them on the sheet if you need more.

## Contributing

Work on a branch and open a pull request. Don't commit APKs, keystores or `local.properties`.
