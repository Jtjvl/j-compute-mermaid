package com.jcompute.mermaidpocket.ai

/**
 * Jev classifies the voice request before the chat model runs.
 *
 * [Patch] keeps the current graph and asks for a minimal edit.
 * [Rewrite] allows a full replacement (new diagram type or structure).
 */
enum class ApplyStrategy {
    Patch,
    Rewrite,
    ;

    val promptName: String
        get() = when (this) {
            Patch -> "patch"
            Rewrite -> "rewrite"
        }

    companion object {
        fun fromLabel(raw: String?): ApplyStrategy? {
            val value = raw?.trim()?.lowercase() ?: return null
            return when (value) {
                "patch", "edit", "local", "small" -> Patch
                "rewrite", "replace", "full", "scratch", "new" -> Rewrite
                else -> null
            }
        }
    }
}
