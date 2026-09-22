# MermaidPocket

Edit and view Mermaid diagram syntax.

MermaidPocket by J-compute · GPL · Diagrams rendered with mermaid.js (MIT).

MermaidPocket is an independent Android app. It is not [Mermaid Chart](https://www.mermaidchart.com/) and it is not the official [mermaid.js](https://github.com/mermaid-js/mermaid) product. The app is **GPL-3.0-or-later**. The bundled diagram engine remains **MIT** (Knut Sveidqvist / mermaid-js). See `LICENSE`, `NOTICE`, and `THIRD_PARTY.md`.

Package: `com.jcompute.mermaidpocket`

Launcher name: **MermaidPocket**

The launcher mark is a pocket outline. It is not a mermaid.live or Mermaid Chart diagram logo. Circuit-J is not used on the launcher.

## Locked loop

1. Copy a diagram from chat (plain Mermaid or a ` ```mermaid ` fence) **or** share that text into MermaidPocket.
2. The app strips markdown fences and opens **View**.
3. **Copy image** puts a PNG on the clipboard (device pixel ratio). Paste it into another app.
4. **Save .mmd** keeps the source in app-private storage. **Files** lists those diagrams.

Paste and share-to-app are equal ingest paths.

## Edit

- **Copy all** in the toolbar copies the entire source. Long-press the editor does the same and shows a short confirmation.
- **Undo / Redo** walk prior source text (not theme). Toolbar buttons, plus a swipe on the Edit history strip (right undoes, left redoes). About 40 steps; rapid keystrokes collapse into one step. Restoring a step updates the Edit buffer. View refreshes from that source when you switch to View.

## Export

Overflow menu, offline only:

| Action | PNG | JPEG | SVG | .mmd |
| --- | --- | --- | --- | --- |
| **Share** | share sheet | share sheet | share sheet | share sheet |
| **Save** | you pick a folder (Downloads, Pictures, …) | same | same | same |

PNG and JPEG ask for a **width** (slider, or an exact pixel value). Height follows the diagram. Default **1920**, remembered after each export, limited to **256–8192**. The dialog shows an approximate file size from the pixel count. SVG has no size control. **Copy image** still uses the screen density bake and does not open this dialog.

In-app **Save .mmd** still writes the library copy under app-private storage. **Save .mmd as…** is the copy you place yourself. Files rows can **Share .mmd**.

## What is included

- **Paste** and **SEND** text ingest, including `.md` fences
- **Edit ↔ View** (one mode on screen)
- **Diagram theme** picker (mermaid.live Default = `redux-color`, Dark = `redux-dark-color`), remembered; app chrome light/dark is separate
- View: pinch-zoom, double-tap to fit; tap **View** again for full-screen (Back or View exits); rotation relayouts the SVG
- **Copy image** to the clipboard
- Share / Save **PNG**, **JPEG**, **SVG**, and **.mmd**
- **Files** with thumbnails; long-press to multi-select and bulk-delete; swipe deletes one file

No accounts, sync, or network. The manifest does not request `INTERNET`.

## Diagrams survive upgrades

Saved diagrams are plain UTF-8 `.mmd` files in internal storage: `filesDir/diagrams/`. That directory is not versioned and is not cleared on startup. Installing a newer APK over **the same** `com.jcompute.mermaidpocket` package and signing key keeps those files.

`com.jcompute.mermaidpocket` is a clean install. MermaidPocket does not migrate diagrams from `com.jcompute.mermaid`.

If this package’s own `filesDir` already contains a pre-v1 `diagrams.json`, the first launch copies those entries into `diagrams/*.mmd` and renames the JSON to `diagrams.json.migrated`. That file is not read from the old package. A new install has nothing to copy. Existing `.mmd` files are left as they are. The `.mmd` text format is unchanged.

Auto Backup is explicit:

- Android 11 and lower: `res/xml/backup_rules.xml`
- Android 12+: `res/xml/data_extraction_rules.xml` (cloud backup and device-to-device)

Both include `filesDir` and shared preferences (appearance, diagram theme, last export width) and exclude `diagrams/.thumbs`, so preview PNGs do not consume the 25 MB cloud quota. Clipboard and share-sheet files live in `cacheDir` and are not backed up.

**Uninstall.** Android deletes app-private files. On Android 10 and later the system may ask whether to keep app data (`hasFragileUserData`). If a Google backup exists, reinstall can restore `.mmd` files; backup runs when the device is idle, on Wi-Fi, and backup is enabled. If you clear storage, decline to keep data, or no backup exists, the library is gone. **Save .mmd as…** is the copy that lives outside the app.

## Build

JDK 17 or 21. Android SDK 35 (`compileSdk` / `targetSdk` 35). Dependencies are pinned in `build.gradle.kts` and `app/build.gradle.kts`.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

`assembleRelease` is unsigned and is not run through R8 (`isMinifyEnabled` is false). Do not add a keystore to the repo. For a closer bit-match, build with JDK 17 and `SOURCE_DATE_EPOCH` set to the commit time. There is no native code. `mermaid.min.js` is a vendored asset.

### F-Droid

Short description: Edit and view Mermaid diagram syntax.

A metadata merge request can follow once this repository is public. This tree does not ship `metadata/*.yml`.

minSdk 26.

## Ingest

| Path | Result |
| --- | --- |
| **Paste** | Clipboard text → strip fences → current document → **View** |
| **Share to MermaidPocket** | `ACTION_SEND` `text/plain` or `text/*` → same ingest → **View** |
| **Open with** | `VIEW` / `EDIT` for text, `.mmd`, `.mermaid`, `.md` |
| **Open…** | System picker; markdown fences stripped |

## Files

- **Save .mmd** writes `filesDir/diagrams/<name>.mmd` and a preview PNG under `diagrams/.thumbs/`
- **Files** lists those files with diagram thumbnails (placeholder until a diagram has been viewed or saved)
- Long-press a row to multi-select and bulk-delete; swipe deletes one file; the share icon sends that `.mmd`
- Unsaved paste/share stays in memory until you save

## Architecture

Rendering is **offline**. A `WebView` loads bundled Mermaid 12.0.0. View is a live SVG (pinch-zoom / pan / Fit).

```
app/src/main/assets/mermaid/
  index.html
  bridge.js
  mermaid.min.js    # MIT
```

- `MermaidIngest` unwraps ` ```mermaid ` / ` ``` ` fences
- `DiagramStore` is a list of real `.mmd` files, not a JSON recents log
- Chrome is Material 3; the diagram canvas is the hero

## Device smoke

No emulator in this environment. On a phone:

1. Edit: type, **Copy all**, long-press the editor (toast), **Undo** / **Redo**, swipe the history strip.
2. Rotate View, tap View again for full-screen, paste the same diagram twice (spinner must clear). Theme Default/Dark still switches.
3. Files: thumbnail after View, long-press bulk delete, swipe one row, share icon.
4. Overflow: Share and Save for PNG, JPEG, SVG, and `.mmd`. PNG/JPEG show the width slider and a size estimate; SVG does not. Save opens the system picker. Copy image still copies without that dialog.
5. Save a `.mmd`, install a newer build of `com.jcompute.mermaidpocket` over it, and confirm the file is still listed.
