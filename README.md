# Mermaid

A native Android editor for [Mermaid](https://mermaid.js.org) diagrams. The v1 UX follows [mermaid.live](https://mermaid.live) on a phone: a first-class **Edit / View** toggle, sparse chrome, and a large canvas that uses Mermaid’s default (or dark) theme.

Package: `com.jcompute.mermaid`  
Working title: **Mermaid**

## Features

- **Edit / View toggle** — write source in a monospace editor, then slide to a rendered diagram. Both modes stay mounted so export still works from Edit.
- **Pretty defaults** — bundled Mermaid 11.12.1 with `theme: default` in light mode and `theme: dark` when the system is dark. Material 3 follows the system (dynamic color on Android 12+).
- **Export** — PNG, SVG, `.mmd`, and plain text through the Android sharesheet.
- **Quick open** — share-to-app (`ACTION_SEND` text), `VIEW` / `EDIT` for `text/plain`, `text/*`, `text/x-mermaid`, and `*.mmd` when the file manager exposes a path, plus an in-app file picker and paste-from-clipboard.
- **Recents** — local list on device. Titles come from a `%%` comment, the filename, or the first source line.

No accounts, sync, analytics, or network permission. Rendering is offline.

## Architecture

Diagrams are rendered **on-device** in a `WebView` that loads bundled assets:

```
app/src/main/assets/mermaid/
  index.html          # canvas, error/empty states
  bridge.js           # mermaid.initialize / render / SVG+PNG export
  mermaid.min.js      # Mermaid 11.12.1 UMD bundle (MIT)
```

`bridge.js` calls `mermaid.render()` and posts results back through a small `JavascriptInterface`. PNG export draws the SVG to a canvas at 2× and returns a data URL. The WebView has `blockNetworkLoads = true`; the app does not request `INTERNET`.

The rest of the app is Kotlin + Jetpack Compose + Material 3:

- `EditorViewModel` holds the current source, mode, and recents.
- `DiagramRepository` persists diagrams as `filesDir/diagrams.json`.
- Incoming `SEND` / `VIEW` / `EDIT` intents are handled in `MainActivity` (`singleTop`) and opened as new recents.

This is a pragmatic v1 renderer, not a reimplementation of Mermaid’s layout engine. Restyling the chrome later (Muses brief) should not require touching the renderer bridge.

## Requirements

- Android Studio Ladybug / Koala or newer, or JDK 17+
- Android SDK Platform 35 and Build-Tools 35.0.0
- minSdk 26 (Android 8.0)

## Build

### Android Studio

1. Open this folder as a project.
2. Let Gradle sync (it will write `local.properties` with your SDK path).
3. Run the `app` configuration on a device or emulator.

### Command line

```bash
# Point Gradle at an SDK (Android Studio does this via local.properties).
export ANDROID_HOME="$HOME/Android/Sdk"   # or your sdk.dir
./gradlew assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Unit tests (title parsing):

```bash
./gradlew testDebugUnitTest
```

## Intents and file open

| Path | How it works |
| --- | --- |
| **Share to Mermaid** | Other apps share `text/plain` or `text/*`. The source opens in **Edit**. |
| **Open with** | `ACTION_VIEW` / `ACTION_EDIT` for `text/plain`, `text/*`, `text/x-mermaid`, and many `*.mmd` content URIs. |
| **In-app Open file** | Overflow → **Open file** (`OpenDocument`). Pick a `.mmd` / `.txt` / any text file. |
| **Paste** | Overflow → **Paste from clipboard**. |

File-manager support for `*.mmd` depends on the `content://` URI and path the provider exposes. If “Open with” does not list Mermaid, use **Open file** or share the text.

Shared / opened documents become a new recent. They are not written back to the original URI in v1.

## Export

Overflow-adjacent **share** icon, or the Export sheet:

- **PNG image** — rasterized from the current SVG (2×), light or dark canvas.
- **SVG vector** — serialized Mermaid SVG.
- **Mermaid source** — `.mmd` text file.
- **Plain text** — same source as `.txt`.

All four go through `Intent.ACTION_SEND` and `FileProvider` (`cache/exports/`).

## Samples

Overflow → **Samples** loads flowchart, mermaid.live’s classic shopping flowchart, sequence, and class diagrams. First launch (no recents) starts on the flowchart in **View**.

## Non-goals (v1)

Accounts, cloud sync, AI, collaboration, and iOS. Not every mermaid.live diagram type is hand-tuned; core flowchart / sequence / class are the supported happy path.

## License notes

App source is yours to reuse. Bundled `mermaid.min.js` is [Mermaid](https://github.com/mermaid-js/mermaid) (MIT).
