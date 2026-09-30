package com.jcompute.mermaidpocket.ai

import org.json.JSONObject

/**
 * Maps a Jev Decisions API payload (or a heuristic fallback) to [ApplyStrategy].
 *
 * Expected shape (OpenRouter cookbook, 2026):
 * `{ "answers": { "apply_strategy": { "type": "choice", "choice": "patch" } } }`
 */
object JevClassifier {
    const val QUESTION_ID = "apply_strategy"

    fun parse(json: String): ApplyStrategy? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val answers = root.optJSONObject("answers") ?: root.optJSONObject("decisions")
        val node = answers?.optJSONObject(QUESTION_ID) ?: answers
        val label = sequenceOf("choice", "decision", "value", "answer")
            .mapNotNull { key -> node?.optString(key)?.takeIf { it.isNotBlank() } }
            .firstOrNull()
            ?: root.optString("choice").takeIf { it.isNotBlank() }
        return ApplyStrategy.fromLabel(label)
    }

    fun questions(): JSONObject {
        val criteria = JSONObject()
            .put("patch", "Small layout, color, label, or local node/edge edits to the existing diagram.")
            .put("rewrite", "A new diagram, a different Mermaid type, or a large structural change.")
        return JSONObject().put(
            QUESTION_ID,
            JSONObject()
                .put("type", "choice")
                .put("instructions", "Should this voice request patch the existing diagram or rewrite it from scratch?")
                .put("criteria", criteria),
        )
    }

    fun state(transcript: String, currentSource: String): JSONObject {
        return JSONObject()
            .put("request", transcript)
            .put("has_existing_diagram", currentSource.isNotBlank())
            .put("diagram_chars", currentSource.length)
            .put("diagram_preview", currentSource.take(800))
    }

    /**
     * Used when Jev is unreachable. Short local tweaks stay [ApplyStrategy.Patch]
     * when a diagram already exists; otherwise rewrite.
     */
    fun heuristic(transcript: String, currentSource: String): ApplyStrategy {
        if (currentSource.isBlank()) return ApplyStrategy.Rewrite
        val text = transcript.lowercase()
        val rewriteHints = listOf(
            "from scratch",
            "start over",
            "new diagram",
            "replace the diagram",
            "redraw",
            "draw a",
            "create a",
            "make a diagram",
            "sequence diagram",
            "class diagram",
            "er diagram",
            "state diagram",
            "gantt",
            "mind map",
            "flowchart of",
        )
        if (rewriteHints.any { text.contains(it) }) return ApplyStrategy.Rewrite
        val patchHints = listOf(
            "rename",
            "relabel",
            "color",
            "colour",
            "layout",
            "align",
            "move",
            "space",
            "font",
            "thicker",
            "dashed",
            "dotted",
            "add a node",
            "add an edge",
            "add a box",
            "remove",
            "delete the",
            "swap",
            "left to right",
            "top to bottom",
        )
        if (patchHints.any { text.contains(it) }) return ApplyStrategy.Patch
        return if (transcript.trim().length < 80) ApplyStrategy.Patch else ApplyStrategy.Rewrite
    }
}
