# CI

Debug APK CI runs on GitHub Actions after Origin→GitHub sync. Zeus publishes push builds to downloads.j-compute.com.

Workflow: [`.github/workflows/android-debug.yml`](../.github/workflows/android-debug.yml) (`./gradlew :app:assembleDebug` on `ubuntu-latest`).

Triggers: push to `main`, pull requests targeting `main`, and manual `workflow_dispatch`.

## Artifacts

| Event | Assemble | `app-debug` artifact |
| --- | --- | --- |
| `pull_request` → `main` | yes (gate) | not uploaded |
| `push` → `main` | yes | uploaded |
| `workflow_dispatch` | yes | uploaded |

Retention is **2 days**. Zeus copies the artifact to downloads within minutes; Actions storage is only a handoff buffer.

## Failure mail

A failed run (pull request, push, or manual dispatch) mails **ops-alert@j-compute.com**. The subject is `[MermaidPocket CI] failed: <event> <sha>`. The body includes the branch, the job conclusion, and the Actions run URL.

SMTP comes from Actions secrets. Zeus is provisioning the server; Hephaestus adds the secrets when ready:

- `OPS_ALERT_SMTP_SERVER`
- `OPS_ALERT_SMTP_PORT`
- `OPS_ALERT_SMTP_USER`
- `OPS_ALERT_SMTP_PASSWORD`
- `OPS_ALERT_FROM` (optional; default From is `ops-alert@j-compute.com`)

If the required secrets are missing, the notify step logs a warning and skips the send. That skip does not fail the job again.
