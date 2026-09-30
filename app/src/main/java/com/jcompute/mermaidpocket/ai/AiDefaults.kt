package com.jcompute.mermaidpocket.ai

/**
 * Defaults and endpoints for optional Architecture B (thin agent).
 *
 * Jev is TypeSafe's decision model on OpenRouter. It is not a chat model:
 * POST https://openrouter.ai/api/alpha/decisions with `state` + typed
 * `questions`. Pinned slug is `typesafe/jev-1.13` (OpenRouter also exposes
 * `~typesafe/jev-latest`). Settings field `classifier_model` overrides this.
 */
object AiDefaults {
    const val CHAT_MODEL = "openai/gpt-4o-mini"
    const val CLASSIFIER_MODEL = "typesafe/jev-1.13"
    const val CLASSIFIER_MODEL_ALIAS = "~typesafe/jev-latest"

    const val OPENROUTER_CHAT_URL = "https://openrouter.ai/api/v1/chat/completions"
    const val OPENROUTER_DECISIONS_URL = "https://openrouter.ai/api/alpha/decisions"
    const val BRAVE_SEARCH_URL = "https://api.search.brave.com/res/v1/web/search"

    const val APP_TITLE = "MermaidPocket"
    const val APP_REFERER = "https://github.com/Jtjvl/j-compute-mermaid"

    const val SECRETS_PREFS = "mermaid_ai_secrets"
    const val KEY_OPENROUTER = "openrouter_api_key"
    const val KEY_BRAVE = "brave_search_api_key"

    const val MAX_TOOL_ROUNDS = 8
    const val SEARCH_RESULT_COUNT = 5
}
