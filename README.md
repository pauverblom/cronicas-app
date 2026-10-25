# Crónicas de la Marca

An Android companion app for our role-playing sessions (character sheet, spells, combat, equipment, notes and dice).

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
Room 2.6.1 (KSP), kotlinx-serialization 1.7.3. `minSdk` 26, `targetSdk` 35.

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

Pull requests and pushes to `main` run `.github/workflows/ci.yml`, which only checks that the debug build compiles.

## Project layout

```
app/src/main/java/io/github/pauverblom/cronicas/
├── CronicasApp.kt          Application: DB/repository singletons, seeds initial data
├── MainActivity.kt
├── data/                   Room entities, DAOs, database, repository, seed data, spell catalog (160 spells)
├── rules/                  Game rules: modifiers, dice, combat, encumbrance, spell slots
├── undo/                   Undo stack (serialized payloads stored in the `accion_undo` table)
└── ui/
    ├── MainViewModel.kt
    ├── navigation/         Scaffold, bottom bar, character picker, new-character dialog
    ├── mesa/               Character sheet (HP, stats, saving throws, effects)
    ├── conjuros/           Prepared spells, grimoire, catalog, CSV import, long rest
    ├── combate/            Weapons, round tracker, timed effects, damage
    ├── equipo/             Coins, items, gems, magic items, carry limit
    ├── notas/              Notes
    ├── dados/              Dice roller bottom sheet
    └── theme/              Colors and typography
```

`app/src/main/assets/reglas.json` ships with the original APK but no code reads it.

## Known issues (inherited from the original APK)

These issues are kept intentionally so the app behaves exactly like the original. Fix them as normal changes.

- **Undo after casting a spell corrupts the prepared count.** `UndoManager` (marked `FIXME`) restores
  `preparadoVeces` with the previous *gastadoVeces* value. It should restore `gastadoVeces`.
- The database uses `fallbackToDestructiveMigration()`. Changing an entity and bumping the version **wipes all data**.
  Write a real `Migration` before changing the schema.
- The `datastore-preferences` dependency is declared but unused.

## Contributing

Work on a branch and open a pull request. Don't commit APKs, keystores or `local.properties`.
