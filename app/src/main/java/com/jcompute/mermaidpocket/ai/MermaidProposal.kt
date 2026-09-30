package com.jcompute.mermaidpocket.ai

import com.jcompute.mermaidpocket.data.MermaidIngest
import org.json.JSONObject

/**
 * Pull a Mermaid document out of a tool argument or a chat reply.
 * Reuses [MermaidIngest] so fences match paste/share.
 */
object MermaidProposal {
    private val typePrefixes = listOf(
        "flowchart",
        "graph",
        "sequenceDiagram",
        "classDiagram",
        "stateDiagram",
        "erDiagram",
        "gantt",
        "pie",
        "gitGraph",
        "mindmap",
        "timeline",
        "journey",
        "quadrantChart",
        "sankey",
        "sankey-beta",
        "xychart",
        "xychart-beta",
        "block-beta",
        "C4Context",
        "C4Container",
        "C4Component",
        "C4Dynamic",
        "C4Deployment",
        "requirementDiagram",
        "architecture-beta",
        "packet-beta",
        "kanban",
        "radar-beta",
    )

    fun extract(raw: String): String {
        val trimmed = raw.replace("\uFEFF", "").trim()
        if (trimmed.isEmpty()) return ""
        parseJsonSource(trimmed)?.let { fromJson ->
            val nested = MermaidIngest.extractSource(fromJson).trim()
            if (nested.isNotEmpty()) return nested
        }
        return MermaidIngest.extractSource(trimmed).trim()
    }

    fun looksLikeMermaid(source: String): Boolean {
        val text = extract(source)
        if (text.isBlank()) return false
        val first = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return false
        if (first.startsWith("%%")) {
            val next = text.lineSequence().map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("%%") }
                .firstOrNull() ?: return false
            return typePrefixes.any { next.startsWith(it, ignoreCase = true) }
        }
        return typePrefixes.any { first.startsWith(it, ignoreCase = true) }
    }

    private fun parseJsonSource(raw: String): String? {
        if (!raw.startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(raw)
            sequenceOf("source", "mermaid", "diagram", "code")
                .mapNotNull { key -> obj.optString(key).takeIf { it.isNotBlank() } }
                .firstOrNull()
        }.getOrNull()
    }
}
