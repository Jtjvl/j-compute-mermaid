#!/usr/bin/env python3
"""Read SemVer from Gradle and CHANGELOG notes for merge-CI tagging."""

from __future__ import annotations

import argparse
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
GRADLE = ROOT / "app" / "build.gradle.kts"
CHANGELOG = ROOT / "CHANGELOG.md"

VERSION_NAME_RE = re.compile(r'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"')
VERSION_CODE_RE = re.compile(r"versionCode\s*=\s*([0-9]+)")


def read_versions(gradle_text: str) -> tuple[str, str]:
    name = VERSION_NAME_RE.search(gradle_text)
    code = VERSION_CODE_RE.search(gradle_text)
    if not name:
        raise SystemExit("versionName not found or is not MAJOR.MINOR.PATCH")
    if not code:
        raise SystemExit("versionCode not found")
    return name.group(1), code.group(1)


def changelog_notes(changelog_text: str, version: str, version_code: str) -> str:
    pattern = rf"^## {re.escape(version)}\n+(.*?)(?=^## |\Z)"
    match = re.search(pattern, changelog_text, flags=re.M | re.S)
    if match:
        body = match.group(1).strip()
        return f"## {version}\n\n{body}\n\n`versionName` {version} · `versionCode` {version_code}\n"
    return f"MermaidPocket {version}\n\n`versionName` {version} · `versionCode` {version_code}\n"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("env", "notes"))
    args = parser.parse_args(argv)
    version, version_code = read_versions(GRADLE.read_text())
    tag = f"v{version}"
    if args.command == "env":
        print(f"VERSION={version}")
        print(f"VERSION_CODE={version_code}")
        print(f"TAG={tag}")
        return 0
    sys.stdout.write(changelog_notes(CHANGELOG.read_text(), version, version_code))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
