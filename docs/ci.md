# CI

Debug APK CI is GitHub Actions only. The happy path does not wait on Zeus or Hephaestus.

Workflow: [`.github/workflows/android-debug.yml`](../.github/workflows/android-debug.yml) (`./gradlew :app:assembleDebug` on `ubuntu-latest`).

Triggers: push to `main`, pull requests targeting `main`, and manual `workflow_dispatch`.

## Happy path

| Event | Assemble | Merge | Tag / GitHub Release | `app-debug` artifact |
| --- | --- | --- | --- | --- |
| `pull_request` → `main` | yes (gate) | yes, when green | yes, after merge, if `v{versionName}` is new | 1-day handoff |
| `push` → `main` | yes | n/a | `vMAJOR.MINOR.PATCH` from `versionName` if that tag is new | 2 days |
| `workflow_dispatch` on `main` | yes | no | same as push | 2 days |

Auto-merge applies to same-repository, non-draft pull requests whose tested head SHA is still the PR head. Mark a PR as draft until it is ready. Fork PRs assemble only.

A `GITHUB_TOKEN` merge does not start another `push` workflow. The pull-request job therefore tags and publishes after it merges, using the merge commit SHA and the assembled APK.

A merge that already has tag `v{versionName}` (same cut, no SemVer bump) skips tagging. Bump `versionName` / `versionCode` and add `CHANGELOG.md` `## x.y.z` when the cut should publish.

The GitHub Release attaches `MermaidPocket-{versionName}-debug.apk`.

## Failure mail

A failed assemble or auto-merge mails **ops-alert@j-compute.com**. The subject is `[MermaidPocket CI] failed: <event> <sha>`. The body includes the branch, job results, and the Actions run URL.

From is `alerts@j-compute.com`. The connection is SMTPS on port **465** (`secure: true`, implicit TLS). These Actions secrets must be set:

- `STALWART_SMTP_HOST`
- `STALWART_SMTP_PORT` (465, SMTPS)
- `STALWART_SMTP_USER` (`alerts@j-compute.com`)
- `STALWART_SMTP_PASSWORD`
- `STALWART_SMTP_FROM` (`alerts@j-compute.com`)
- `OPS_ALERT_TO` (`ops-alert@j-compute.com`)

If any of those secrets is empty, the notify step logs a warning and skips the send. That skip does not fail the job again.
