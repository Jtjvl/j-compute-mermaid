# Versioning

SemVer on MermaidPocket (`com.jcompute.mermaidpocket`) communicates changes to **end users**, not every merge to `main`.

Do **not** auto-bump on every merge to `main`.

Bump `versionName` in `app/build.gradle.kts` and add a git tag `vMAJOR.MINOR.PATCH` only for an intentional user-facing release:

| Bump | When |
| --- | --- |
| **PATCH** | A bugfix users care about |
| **MINOR** | A new user-visible feature |
| **MAJOR** | A breaking change for users |

Stay on **0.y.z** until a public store listing or F-Droid.

Increment `versionCode` on every APK users are expected to install, including when the debug downloads APK is replaced.

Zeus cron publishes by git SHA. SemVer is how Play, F-Droid, and sideload builds tell users what changed.

Older tags `v0.1.0-debug` and `v0.1.1-debug` were early sideload cuts. New tags drop the `-debug` suffix (`v0.2.0`, not `v0.2.0-debug`). The release pull request does not create the tag; Hephaestus tags after merge.
