#!/usr/bin/env python3
import unittest

from semver_release import changelog_notes, read_versions


class SemverReleaseTest(unittest.TestCase):
    def test_read_versions(self):
        text = 'defaultConfig {\n        versionCode = 7\n        versionName = "0.3.4"\n}\n'
        self.assertEqual(("0.3.4", "7"), read_versions(text))

    def test_rejects_non_semver_name(self):
        with self.assertRaises(SystemExit):
            read_versions('versionName = "0.3.4-debug"\nversionCode = 1\n')

    def test_notes_from_changelog_section(self):
        changelog = "# Changelog\n\n## 0.3.4\n\n- sibling list\n\n## 0.3.3\n\n- old\n"
        notes = changelog_notes(changelog, "0.3.4", "7")
        self.assertEqual(
            "## 0.3.4\n\n- sibling list\n\n`versionName` 0.3.4 · `versionCode` 7\n",
            notes,
        )

    def test_notes_fallback_when_section_missing(self):
        notes = changelog_notes("# Changelog\n\n## 0.3.3\n\n- old\n", "0.3.4", "7")
        self.assertEqual(
            "MermaidPocket 0.3.4\n\n`versionName` 0.3.4 · `versionCode` 7\n",
            notes,
        )


if __name__ == "__main__":
    unittest.main()
