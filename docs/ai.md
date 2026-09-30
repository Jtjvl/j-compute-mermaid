# Optional AI (voice → diagram)

Architecture B: a thin on-device agent. The core editor stays offline. Network is used only after you tap **Send** (and for Android SpeechRecognizer, which may use Google’s cloud STT).

## Keys

1. Open **⋮ → Settings**.
2. Paste an [OpenRouter](https://openrouter.ai/) API key. Stored in `EncryptedSharedPreferences` (`mermaid_ai_secrets`) via Android Keystore. Never written to the plaintext `mermaid` prefs. Excluded from Auto Backup.
3. Leave **Chat model** as `openai/gpt-4o-mini` or set any OpenRouter chat model that supports tools.
4. Leave **Classifier model** as `typesafe/jev-1.13` (Jev Decisions API). `~typesafe/jev-latest` follows the newest Jev.
5. Optional: [Brave Search API](https://brave.com/search/api/) key for `web_lookup`. Header `X-Subscription-Token`. Do not scrape Google SERPs.

## Happy path (device)

1. Open a diagram (or paste one) so Edit/View have source.
2. Confirm you have network. The mic stays **visible** when offline or when the OpenRouter key is empty; it is **disabled** with content description `Needs network + OpenRouter key`.
3. Grant microphone when asked (Android `SpeechRecognizer` only; whisper.cpp is not bundled).
4. Tap mic on **Edit** or **View**. Speak something like “make the boxes blue” or “draw a sequence diagram of login”.
5. Edit the transcript in the bottom sheet. **Retry** restarts the mic. **Send** runs the agent. There is no chat panel.
6. Jev classifies patch vs rewrite. The chat model may call `get_diagram`, `web_lookup`, `set_diagram`.
7. Bundled mermaid.js `parseDiagram` validates **before** apply. A valid diagram replaces the buffer (undo restores last-good). An invalid one toasts and does not replace the buffer. If the following View render still fails, the app undoes automatically.

## Unit tests (no keys)

```bash
./gradlew testDebugUnitTest
```

`VoiceAiTest` covers Jev JSON routing, Mermaid extract, disabled mic rules, and a mocked tool loop (valid apply, invalid reject, optional Brave lookup).

## Mock / fake HTTP

`DiagramAgent` takes `OpenRouterClient` + `BraveSearchClient` over `JsonHttp`. Tests use a scripted client. There is no emulator in every environment; use this suite plus the device steps above.
