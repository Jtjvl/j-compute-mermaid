# Mermaid

Native Android app for the phone loop: **agent chat → copy or share → View → copy image**, then optionally **Save .mmd**.

Package: `com.jcompute.mermaid`

## Locked v1 loop

1. Copy a diagram from chat (plain Mermaid or a ` ```mermaid ` fence) **or** share that text to Mermaid.
2. The app strips markdown fences and opens **View**.
3. Tap **Copy image** and paste the PNG into another app.
4. Optional: **Save .mmd** into app storage, or **Save as…** via the system file picker.

Paste and share-to-app are equal ingest paths.

## What is in v1

- **Paste** and **SEND** text ingest, including `.md` fences
- **Edit ↔ View** toggle (one mode on screen; not a dual pane)
- Mermaid **default** (light) / **dark** themes, system light/dark with a remembered override
- View: pinch-zoom, double-tap to fit
- **Copy image to clipboard** (primary export)
- **Save / Open `.mmd`** in app-private `filesDir/diagrams/`
- Optional **Save as…** / **Open…** through Storage Access Framework
- Open `.mmd` / `.mermaid` / `.md` when the file manager offers them

Raw source only. No snippet bar, templates, autocomplete, or AI.

## What is not v1

PNG/SVG/share-sheet as primary actions, PDF, accounts, sync, theme store, plugins.

## Architecture

Rendering is **offline**. A hidden `WebView` loads bundled Mermaid 11.12.1 and is used only as the engine. View shows a native bitmap (pan/zoom). No `INTERNET` permission.

```
app/src/main/assets/mermaid/
  index.html
  bridge.js
  mermaid.min.js    # MIT
```

- `MermaidIngest` unwraps ` ```mermaid ` / ` ``` ` fences
- `DiagramStore` is a list of real `.mmd` files, not a JSON recents log
- Chrome is Material 3; the diagram canvas is the hero

## Build

Android Studio (SDK 35) or:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
./gradlew testDebugUnitTest
```

minSdk 26. JDK 17+.

## Ingest

| Path | Result |
| --- | --- |
| **Paste** | Clipboard text → strip fences → current document → **View** |
| **Share to Mermaid** | `ACTION_SEND` `text/plain` or `text/*` → same ingest → **View** |
| **Open with** | `VIEW` / `EDIT` for text, `.mmd`, `.mermaid`, `.md` |
| **Open…** | SAF picker; markdown fences stripped |

## Files

- **Save .mmd** writes `filesDir/diagrams/<name>.mmd`
- **Files** lists those files (swipe to delete)
- **Save as…** exports through SAF to shared storage
- Unsaved paste/share stays in memory until you save

## License notes

Bundled `mermaid.min.js` is [Mermaid](https://github.com/mermaid-js/mermaid) (MIT). See `THIRD_PARTY.md`.
