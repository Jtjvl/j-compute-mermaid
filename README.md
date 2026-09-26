# MermaidPocket

Edit and view Mermaid diagram syntax.

MermaidPocket by J-compute · GPL · Diagrams rendered with mermaid.js (MIT).

MermaidPocket is an independent Android app. It is not [Mermaid Chart](https://www.mermaidchart.com/) and it is not the official [mermaid.js](https://github.com/mermaid-js/mermaid) product. The app is **GPL-3.0-or-later**. The bundled diagram engine remains **MIT** (Knut Sveidqvist / mermaid-js). See `LICENSE`, `NOTICE`, and `THIRD_PARTY.md`.

Package: `com.jcompute.mermaidpocket`

Launcher name: **MermaidPocket**

The launcher mark is the signed-off diamond-fork on a square full-bleed `#2A2A2E` field (adaptive icon v2). It is not a mermaid.live or Mermaid Chart diagram logo. Circuit-J is not used on the launcher.

## Locked loop

1. Copy a diagram from chat (plain Mermaid or a ` ```mermaid ` fence) **or** share that text into MermaidPocket.
2. The app strips markdown fences and opens **View**.
3. **Copy image** puts a PNG on the clipboard (device pixel ratio). Paste it into another app.
4. **Save** writes the source into **Documents/MermaidPocket** (the opened file’s real name, or `.mmd` for a new blank). **Files** lists those diagrams.

Paste and share-to-app are equal ingest paths.

## Edit

- **Save** floats in the lower corner of Edit, the same control family as diagram-view **Copy image** (size, elevation, corner padding, shape) with a Save glyph and no text label. A linked file is overwritten. A blank is not linked to an existing file: the control stays visible, the first tap creates the `.mmd` and links it (announced as **Save as**), and later taps save that file.
- **New** is menu-only (document overflow, and the Files overflow). It starts a blank `.mmd` that is not linked to any existing file. There is no lower-corner New control.
- **Copy all** in the toolbar copies the entire source. Long-press the editor does the same and shows a short confirmation.
- **Undo / Redo** walk prior source text (not theme). Toolbar buttons, plus a swipe on the Edit history strip (right undoes, left redoes). About 40 steps; rapid keystrokes collapse into one step. Restoring a step updates the Edit buffer. View refreshes from that source when you switch to View.

## Export

Overflow menu, offline only: exactly **Save** and **Share**. Pressing either one opens the formats (PNG, SVG, JPEG, .mmd). It is not a flat list of one row per format.

| Action | What you get |
| --- | --- |
| **Save** | PNG, JPEG, and SVG open the system picker. **.mmd** writes the library copy. **Save a copy…** places a `.mmd` wherever you choose. |
| **Share** | The same formats, then the Android share sheet. |

PNG and JPEG ask for a **width** (slider, or an exact pixel value) on that same screen. Height follows the diagram. Default **1920**, remembered after each export, limited to **256–8192**. The dialog shows an approximate file size from the pixel count. SVG and `.mmd` have no size control. **Copy image** still uses the screen density bake and does not open this dialog. **Copy all** stays in the Edit toolbar.

## What is included

- **Paste** and **SEND** text ingest, including `.md` fences
- **Edit ↔ View** (one mode on screen)
- **Diagram theme** picker (mermaid.live Default = `redux-color`, Dark = `redux-dark-color`), remembered; app chrome light/dark is separate
- View: pinch-zoom, double-tap to fit; tap **View** again for full-screen (Back or View exits); rotation relayouts the SVG
- **Copy image** to the clipboard
- **Save** and **Share**, then PNG, SVG, JPEG, or `.mmd`
- **Files** with thumbnails; long-press to multi-select and bulk-delete; swipe deletes one file

No accounts, sync, or network. The manifest does not request `INTERNET`.

## Diagrams survive uninstall

Saved diagrams are plain UTF-8 `.mmd` files in **Documents/MermaidPocket**.

That folder is on shared storage, granted with the system folder picker (`ACTION_OPEN_DOCUMENT_TREE`, persistable URI permission). Android deletes app-private storage on uninstall; it does not delete this folder. After you reinstall MermaidPocket, choose the same folder and the files are listed again. If you only grant `Documents`, the app uses the `MermaidPocket` directory inside it. If the folder you pick already contains `.mmd` files, that folder is the library; a nested `MermaidPocket` directory is not created in its place.

The app also keeps a working copy in `filesDir/diagrams/` so the editor can open files directly. The first time you grant the folder, existing `.mmd` files in that working copy (including a pre-v1 `diagrams.json` after it is converted) are copied into `Documents/MermaidPocket`. Files already in the folder are copied back into the working copy. Same text is not duplicated. The newer file wins when both sides differ.

`com.jcompute.mermaidpocket` is a clean install. MermaidPocket does not migrate diagrams from `com.jcompute.mermaid`.

If this package’s own `filesDir` already contains a pre-v1 `diagrams.json`, the first launch copies those entries into `diagrams/*.mmd` and renames the JSON to `diagrams.json.migrated`. That file is not read from the old package. Existing `.mmd` files are left as they are. The `.mmd` text format is unchanged.

Auto Backup still covers the working copy and preferences:

- Android 11 and lower: `res/xml/backup_rules.xml`
- Android 12+: `res/xml/data_extraction_rules.xml` (cloud backup and device-to-device)

Both include `filesDir` and shared preferences (appearance, diagram theme, last export width, folder grant) and exclude `diagrams/.thumbs`, so preview PNGs do not consume the 25 MB cloud quota. Clipboard and share-sheet files live in `cacheDir` and are not backed up. The grant itself does not survive uninstall; the `.mmd` files in `Documents/MermaidPocket` do.

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

Debug APK CI is GitHub Actions only. A green, non-draft same-repo pull request to `main` merges itself and, when `versionName` is a new cut, tags `vMAJOR.MINOR.PATCH` and publishes a GitHub Release with the APK. Push to `main` and manual runs do the same assemble/tag path. A failed run mails ops-alert@j-compute.com from alerts@j-compute.com over SMTPS. See [`docs/ci.md`](docs/ci.md).

`assembleRelease` is unsigned and is not run through R8 (`isMinifyEnabled` is false). Do not add a keystore to the repo. For a closer bit-match, build with JDK 17 and `SOURCE_DATE_EPOCH` set to the commit time. There is no native code. `mermaid.min.js` is a vendored asset.

### F-Droid

Product name: **MermaidPocket**

Summary:

Private, offline Mermaid diagram editor for your phone. No ads, no accounts, no network.

Longer description:

Edit and view Mermaid diagram syntax on-device. Paste from chat, pinch-zoom the diagram, save .mmd locally. Fully offline. No ads, no tracking.

The same Summary is in `fastlane/metadata/android/en-US/short_description.txt`. A metadata merge request can follow once this repository is public. This tree does not ship `metadata/*.yml`.

minSdk 26.

## Ingest

| Path | Result |
| --- | --- |
| **Paste** | Clipboard text → strip fences → current document → **View** |
| **Share to MermaidPocket** | `ACTION_SEND` `text/plain` or `text/*` → same ingest → **View** |
| **Open with** | `VIEW` / `EDIT` for a diagram file (`.mmd`, `.txt`, or other readable text) **links** that file into the library under its real name so Files shows it and Save overwrites it. Plain shared text still ingests unlinked. |
| **Open…** | In-app list of the granted diagram folder (Back returns to the app). **Browse storage…** uses the system picker. Opening a file copies or reuses the working copy so it appears in Files. Markdown fences stripped. |

## Files

- **Save** of a linked file overwrites that same name in `Documents/MermaidPocket` (and a working copy under `filesDir/diagrams/`). A new blank first-save still uses `.mmd`. Preview PNGs stay in `diagrams/.thumbs/`
- **Files** lists readable text/mermaid peers in the granted library folder (not only `.mmd`; including files with no MIME). Each row shows the parsed title and the real file name, so two diagrams can share a title. Opening a listed row sets the chrome filename and parsed title to that row.
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
- `DiagramStore` is a list of real diagram files, not a JSON recents log
- Chrome is Material 3; the diagram canvas is the hero

## Device smoke

No emulator in this environment. On a phone:

1. Edit: type, **Copy all**, long-press the editor (toast), **Undo** / **Redo**, swipe the history strip.
2. Rotate View, tap View again for full-screen, paste the same diagram twice (spinner must clear). Theme Default/Dark still switches.
3. Files: thumbnail after View, long-press bulk delete, swipe one row, share icon.
4. Overflow: one **Save** and one **Share**. Each opens PNG, SVG, JPEG, and `.mmd`. PNG/JPEG show the width slider and a size estimate. Save of PNG, JPEG, and SVG opens the system picker. Share opens the Android share sheet. Copy all and Copy image still copy without that dialog.
5. Save a `.mmd`, uninstall `com.jcompute.mermaidpocket`, reinstall it, choose `Documents/MermaidPocket` again, and confirm the file is listed.

## License notes

Bundled `mermaid.min.js` is [Mermaid](https://github.com/mermaid-js/mermaid) (MIT). See `THIRD_PARTY.md`.
