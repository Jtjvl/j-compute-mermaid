#!/usr/bin/env bash
# Build the debug APK, run unit tests, copy it to artifacts/, and
# optionally publish/replace a GitHub Release asset.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

if [[ ! -d "$ANDROID_HOME/platforms" ]]; then
  echo "ANDROID_HOME=$ANDROID_HOME has no platforms/. Install Android SDK 35 first." >&2
  exit 1
fi

./gradlew assembleDebug testDebugUnitTest

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "Expected APK missing: $APK" >&2
  exit 1
fi

mkdir -p "$ROOT/artifacts"
cp -f "$APK" "$ROOT/artifacts/app-debug.apk"
echo "Copied $APK -> artifacts/app-debug.apk"
sha256sum "$ROOT/artifacts/app-debug.apk"

if [[ "${PUBLISH_RELEASE:-0}" != "1" ]]; then
  echo "Skipping GitHub Release (set PUBLISH_RELEASE=1 to publish)."
  exit 0
fi

TAG="${RELEASE_TAG:-v0.1.0-debug}"
REPO="${RELEASE_REPO:-Jtjvl/j-compute-mermaid}"
SHA256="$(sha256sum "$APK" | awk '{print $1}')"
COMMIT="$(git rev-parse --short HEAD)"

NOTES=$(cat <<EOF
Debug APK for sideload on a phone. Built from \`${COMMIT}\`.

## Install on a phone
1. Enable **Install unknown apps** for your browser (Settings → Apps → Special app access → Install unknown apps).
2. Open this release **in a browser where you are signed into GitHub** (the repo is private).
3. Tap **app-debug.apk** and install.

If the download 404s, you are not signed in. Private release assets require GitHub auth.

## Desktop download
\`\`\`
gh release download ${TAG} --repo ${REPO} --pattern '*.apk'
\`\`\`

SHA-256: \`${SHA256}\`
EOF
)

if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  gh release upload "$TAG" "$APK" --repo "$REPO" --clobber
  echo "Updated asset on existing release $TAG"
else
  gh release create "$TAG" "$APK" \
    --repo "$REPO" \
    --title "${TAG} — sideload APK" \
    --notes "$NOTES"
fi

gh api "repos/${REPO}/releases/tags/${TAG}" \
  --jq '{release: .html_url, apk: .assets[] | select(.name=="app-debug.apk") | .browser_download_url}'
