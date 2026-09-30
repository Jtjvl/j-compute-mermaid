package com.jcompute.mermaidpocket.ai

data class AiConfig(
    val openRouterKey: String = "",
    val braveSearchKey: String = "",
    val chatModel: String = AiDefaults.CHAT_MODEL,
    val classifierModel: String = AiDefaults.CLASSIFIER_MODEL,
) {
    val hasOpenRouterKey: Boolean get() = openRouterKey.isNotBlank()
    val hasSearchKey: Boolean get() = braveSearchKey.isNotBlank()
}

object AiAvailability {
    const val NEEDS_NETWORK_AND_KEY = "Needs network + OpenRouter key"

    fun voiceEnabled(hasOpenRouterKey: Boolean, hasNetwork: Boolean): Boolean {
        return hasOpenRouterKey && hasNetwork
    }

    fun disabledReason(hasOpenRouterKey: Boolean, hasNetwork: Boolean): String? {
        if (voiceEnabled(hasOpenRouterKey, hasNetwork)) return null
        return when {
            !hasOpenRouterKey && !hasNetwork -> NEEDS_NETWORK_AND_KEY
            !hasOpenRouterKey -> "Add an OpenRouter key in Settings"
            else -> "Needs network + OpenRouter key"
        }
    }
}
