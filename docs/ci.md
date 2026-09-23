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

From is `alerts@j-compute.com`. The connection is SMTPS on port **465** (`secure: true`, implicit TLS). Zeus provisioned Stalwart and set these Actions secrets:

- `STALWART_SMTP_HOST`
- `STALWART_SMTP_PORT` (465, SMTPS)
- `STALWART_SMTP_USER` (`alerts@j-compute.com`)
- `STALWART_SMTP_PASSWORD`
- `STALWART_SMTP_FROM` (`alerts@j-compute.com`)
- `OPS_ALERT_TO` (`ops-alert@j-compute.com`)

If any of those secrets is empty, the notify step logs a warning and skips the send. That skip does not fail the job again.

## Failure webhook

After the ops-alert mail, the same failed run POSTs JSON to the Hephaestus routine webhook. The body is `source`, `repo`, `event`, `ref`, `sha`, `conclusion` (`failure`), `run_url`, and `workflow`.

Secrets (empty until Zeus or Hephaestus fills them):

- `HEPHAESTUS_WEBHOOK_URL` (routine panel **POST to**)
- `HEPHAESTUS_WEBHOOK_KEY` (routine panel raw key)

The request sends `Authorization: Bearer <HEPHAESTUS_WEBHOOK_KEY>`. That is the header the [Grok Bot routine panel](https://cursor.com/help/grok-bot/routines) documents: the key is the secret, senders include it as `Authorization: Bearer <key>`, and the copyable header field is that same Bearer value. `X-Webhook-Key` is not sent.

If either secret is empty, the step logs a warning and skips the POST. That skip does not fail the job.
