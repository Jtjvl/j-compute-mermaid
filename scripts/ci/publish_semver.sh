#!/usr/bin/env bash
# Tag vMAJOR.MINOR.PATCH from versionName and publish a GitHub Release.
# Usage: publish_semver.sh <target-sha> <apk-path>
set -euo pipefail

TARGET_SHA="${1:?target sha required}"
APK="${2:?apk path required}"

if [ ! -f "$APK" ]; then
  echo "APK not found: ${APK}" >&2
  exit 1
fi

eval "$(python3 scripts/ci/semver_release.py env)"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "version=${VERSION}" >> "$GITHUB_OUTPUT"
  echo "version_code=${VERSION_CODE}" >> "$GITHUB_OUTPUT"
  echo "tag=${TAG}" >> "$GITHUB_OUTPUT"
fi

if git ls-remote --exit-code --tags origin "refs/tags/${TAG}" >/dev/null; then
  echo "Tag ${TAG} already exists; skipping (versionName was not a new cut)."
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    echo "created=false" >> "$GITHUB_OUTPUT"
  fi
  exit 0
fi

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git tag -a "${TAG}" -m "MermaidPocket ${VERSION}" "${TARGET_SHA}"
git push origin "${TAG}"
NOTES="$(python3 scripts/ci/semver_release.py notes)"
ASSET="MermaidPocket-${VERSION}-debug.apk"
cp "${APK}" "${ASSET}"
gh release create "${TAG}" "${ASSET}" \
  --title "${TAG}" \
  --notes "${NOTES}" \
  --target "${TARGET_SHA}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "created=true" >> "$GITHUB_OUTPUT"
fi
echo "Published ${TAG} at ${TARGET_SHA}."
