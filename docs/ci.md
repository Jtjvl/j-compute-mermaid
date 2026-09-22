# CI

Debug APK CI runs on GitHub Actions after Origin→GitHub sync; download the `app-debug` artifact; Zeus publishes to downloads.j-compute.com.

Workflow: [`.github/workflows/android-debug.yml`](../.github/workflows/android-debug.yml) (`./gradlew :app:assembleDebug` on `ubuntu-latest`).

Triggers: push to `main`, pull requests targeting `main`, and manual `workflow_dispatch`.
