# Crónicas de la Marca

An Android companion app for our role-playing sessions (character sheet, spells, combat, equipment, notes and dice).

## Origin of this source code

The original source code was not available. This project was **reconstructed by decompiling `app-debug.apk`**
(versionName `0.1.0`, package `com.example.cronicasmarca`):

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

> **Note:** the APK from your friend was signed with *their* debug key. Android will refuse to install a build
> from this repo over it ("signatures don't match"). Uninstall the old app first. **This deletes its local
> data.** Export anything you need to keep before uninstalling.

## Project layout

```
app/src/main/java/com/example/cronicasmarca/
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
