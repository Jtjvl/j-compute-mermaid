package com.jcompute.mermaidpocket.ai

import org.json.JSONArray
import org.json.JSONObject

data class MermaidValidation(
    val ok: Boolean,
    val error: String = "",
)

fun interface MermaidValidator {
    suspend fun validate(source: String): MermaidValidation
}

sealed interface AgentOutcome {
    data class Applied(
        val source: String,
        val strategy: ApplyStrategy,
        val usedWeb: Boolean,
    ) : AgentOutcome

    data class Invalid(
        val attempted: String,
        val error: String,
        val strategy: ApplyStrategy,
    ) : AgentOutcome

    data class Failed(val message: String) : AgentOutcome
}

data class AgentRequest(
    val transcript: String,
    val currentSource: String,
    val strategy: ApplyStrategy,
    val chatModel: String,
    val apiKey: String,
    val searchKey: String = "",
)

class DiagramAgent(
    private val openRouter: OpenRouterClient,
    private val search: BraveSearchClient,
    private val validator: MermaidValidator,
    private val maxRounds: Int = AiDefaults.MAX_TOOL_ROUNDS,
) {
    suspend fun run(request: AgentRequest): AgentOutcome {
        val transcript = request.transcript.trim()
        if (transcript.isEmpty()) return AgentOutcome.Failed("Nothing to send.")
        val messages = mutableListOf(
            ChatMessage(role = "system", content = systemPrompt(request.strategy, request.searchKey.isNotBlank())),
            ChatMessage(
                role = "user",
                content = userPrompt(transcript, request.currentSource, request.strategy),
            ),
        )
        val tools = DiagramTools.schemas(includeSearch = request.searchKey.isNotBlank())
        var usedWeb = false
        var lastInvalid: AgentOutcome.Invalid? = null
        var working = request.currentSource

        repeat(maxRounds) {
            val result = runCatching {
                openRouter.chat(request.apiKey, request.chatModel, messages, tools)
            }.getOrElse { error ->
                return AgentOutcome.Failed(error.message ?: "OpenRouter request failed.")
            }
            val assistant = result.message
            messages.add(assistant)
            if (assistant.toolCalls.isEmpty()) {
                val extracted = MermaidProposal.extract(assistant.content.orEmpty())
                if (!MermaidProposal.looksLikeMermaid(extracted)) {
                    return lastInvalid ?: AgentOutcome.Failed(
                        assistant.content?.takeIf { it.isNotBlank() } ?: "The model did not return a diagram.",
                    )
                }
                val outcome = applyProposed(extracted, request.strategy, usedWeb)
                if (outcome is AgentOutcome.Invalid) lastInvalid = outcome
                return outcome
            }
            for (call in assistant.toolCalls) {
                val toolResult = when (call.name) {
                    DiagramTools.GET_DIAGRAM -> working.ifBlank { "(empty diagram)" }
                    DiagramTools.SET_DIAGRAM -> {
                        val proposed = MermaidProposal.extract(DiagramTools.argument(call.arguments, "source"))
                        if (proposed.isBlank()) {
                            "set_diagram failed: missing source."
                        } else {
                            when (val outcome = applyProposed(proposed, request.strategy, usedWeb)) {
                                is AgentOutcome.Applied -> return outcome
                                is AgentOutcome.Invalid -> {
                                    lastInvalid = outcome
                                    "invalid mermaid: ${outcome.error}. Fix the source and call set_diagram again."
                                }
                                is AgentOutcome.Failed -> return outcome
                            }
                        }
                    }
                    DiagramTools.WEB_LOOKUP -> {
                        if (request.searchKey.isBlank()) {
                            "web_lookup is not configured."
                        } else {
                            val query = DiagramTools.argument(call.arguments, "query")
                            usedWeb = true
                            runCatching {
                                BraveSearchClient.format(search.search(request.searchKey, query))
                            }.getOrElse { error ->
                                "search failed: ${error.message ?: "unknown error"}"
                            }
                        }
                    }
                    else -> "unknown tool ${call.name}"
                }
                messages.add(
                    ChatMessage(
                        role = "tool",
                        content = toolResult,
                        toolCallId = call.id,
                        name = call.name,
                    ),
                )
                if (call.name == DiagramTools.SET_DIAGRAM && lastInvalid == null) {
                    val proposed = MermaidProposal.extract(DiagramTools.argument(call.arguments, "source"))
                    if (proposed.isNotBlank()) working = proposed
                }
            }
        }
        return lastInvalid ?: AgentOutcome.Failed("The model did not finish with a valid diagram.")
    }

    private suspend fun applyProposed(
        proposed: String,
        strategy: ApplyStrategy,
        usedWeb: Boolean,
    ): AgentOutcome {
        if (!MermaidProposal.looksLikeMermaid(proposed)) {
            return AgentOutcome.Invalid(proposed, "Not recognizable Mermaid source.", strategy)
        }
        val check = validator.validate(proposed)
        return if (check.ok) {
            AgentOutcome.Applied(proposed, strategy, usedWeb)
        } else {
            AgentOutcome.Invalid(proposed, check.error.ifBlank { "Renderer rejected the diagram." }, strategy)
        }
    }

    companion object {
        fun systemPrompt(strategy: ApplyStrategy, searchEnabled: Boolean): String {
            val strategyLine = when (strategy) {
                ApplyStrategy.Patch ->
                    "Apply strategy: PATCH. Keep the existing diagram. Change only what the user asked. Preserve node ids when you can."
                ApplyStrategy.Rewrite ->
                    "Apply strategy: REWRITE. You may replace the diagram or change its Mermaid type when that fits the request."
            }
            val searchLine = if (searchEnabled) {
                "Call web_lookup when you need facts (names, steps, relationships). Skip it for layout, color, or rename tweaks."
            } else {
                "Do not invent a web search. Work from the request and the current diagram."
            }
            return """
                You edit Mermaid diagrams for MermaidPocket.
                $strategyLine
                Tools: get_diagram returns the current source. set_diagram submits a complete Mermaid document (no markdown fences).
                $searchLine
                Output only valid Mermaid that mermaid.js 12 can parse. Prefer flowchart/sequence/class/er/state/gantt/pie.
                After you have the final source, call set_diagram. Do not chat with the user.
            """.trimIndent()
        }

        fun userPrompt(transcript: String, currentSource: String, strategy: ApplyStrategy): String {
            val existing = currentSource.ifBlank { "(empty)" }
            return "Voice request:\n$transcript\n\nCurrent diagram:\n$existing\n\nStrategy: ${strategy.promptName}"
        }
    }
}

object DiagramTools {
    const val GET_DIAGRAM = "get_diagram"
    const val SET_DIAGRAM = "set_diagram"
    const val WEB_LOOKUP = "web_lookup"

    fun schemas(includeSearch: Boolean): List<JSONObject> {
        val tools = mutableListOf(
            function(
                GET_DIAGRAM,
                "Return the current Mermaid source.",
                JSONObject().put("type", "object").put("properties", JSONObject()),
            ),
            function(
                SET_DIAGRAM,
                "Submit a complete Mermaid document to validate and apply.",
                JSONObject()
                    .put("type", "object")
                    .put(
                        "properties",
                        JSONObject().put(
                            "source",
                            JSONObject().put("type", "string").put("description", "Full Mermaid source, no markdown fences."),
                        ),
                    )
                    .put("required", JSONArray().put("source")),
            ),
        )
        if (includeSearch) {
            tools.add(
                function(
                    WEB_LOOKUP,
                    "Search the web for facts needed to draw the diagram. Skip for pure layout tweaks.",
                    JSONObject()
                        .put("type", "object")
                        .put(
                            "properties",
                            JSONObject().put(
                                "query",
                                JSONObject().put("type", "string").put("description", "Search query."),
                            ),
                        )
                        .put("required", JSONArray().put("query")),
                ),
            )
        }
        return tools
    }

    fun argument(arguments: String, key: String): String {
        val trimmed = arguments.trim()
        if (trimmed.isEmpty()) return ""
        if (!trimmed.startsWith("{")) {
            return if (key == "source") trimmed else ""
        }
        val obj = runCatching { JSONObject(trimmed) }.getOrNull() ?: return trimmed
        val aliases = when (key) {
            "source" -> listOf("source", "mermaid", "diagram", "code")
            "query" -> listOf("query", "q", "search")
            else -> listOf(key)
        }
        return aliases.mapNotNull { alias -> obj.optString(alias).takeIf { it.isNotBlank() } }.firstOrNull().orEmpty()
    }

    private fun function(name: String, description: String, parameters: JSONObject): JSONObject {
        return JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("parameters", parameters),
            )
    }
}
