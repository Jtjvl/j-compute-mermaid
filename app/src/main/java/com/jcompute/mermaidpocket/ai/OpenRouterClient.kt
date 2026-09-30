package com.jcompute.mermaidpocket.ai

import org.json.JSONArray
import org.json.JSONObject

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val name: String? = null,
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

data class ChatResult(
    val message: ChatMessage,
    val finishReason: String,
)

class OpenRouterClient(
    private val http: JsonHttp,
    private val chatUrl: String = AiDefaults.OPENROUTER_CHAT_URL,
    private val decisionsUrl: String = AiDefaults.OPENROUTER_DECISIONS_URL,
) {
    suspend fun classify(
        apiKey: String,
        model: String,
        transcript: String,
        currentSource: String,
    ): ApplyStrategy? {
        val body = JSONObject()
            .put("model", model.ifBlank { AiDefaults.CLASSIFIER_MODEL })
            .put("state", JevClassifier.state(transcript, currentSource))
            .put("questions", JevClassifier.questions())
            .toString()
        val json = http.request("POST", decisionsUrl, authHeaders(apiKey), body)
        return JevClassifier.parse(json)
    }

    suspend fun chat(
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        tools: List<JSONObject>,
    ): ChatResult {
        val payload = JSONObject()
            .put("model", model.ifBlank { AiDefaults.CHAT_MODEL })
            .put("messages", messages.toJson())
            .put("temperature", 0.2)
        if (tools.isNotEmpty()) {
            payload.put("tools", JSONArray(tools))
            payload.put("tool_choice", "auto")
        }
        val json = http.request("POST", chatUrl, authHeaders(apiKey), payload.toString())
        return parseChat(json)
    }

    private fun authHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer ${apiKey.trim()}",
            "Content-Type" to "application/json",
            "HTTP-Referer" to AiDefaults.APP_REFERER,
            "X-OpenRouter-Title" to AiDefaults.APP_TITLE,
            "X-Title" to AiDefaults.APP_TITLE,
        )
    }

    companion object {
        fun parseChat(json: String): ChatResult {
            val root = JSONObject(json)
            val error = root.optJSONObject("error")
            if (error != null) {
                val message = error.optString("message").ifBlank { json }
                error(message)
            }
            val choice = root.optJSONArray("choices")?.optJSONObject(0)
                ?: error("OpenRouter returned no choices.")
            val finish = choice.optString("finish_reason").ifBlank { "stop" }
            val message = choice.optJSONObject("message") ?: JSONObject()
            val toolCalls = message.optJSONArray("tool_calls")
            val parsedCalls = buildList {
                if (toolCalls != null) {
                    for (index in 0 until toolCalls.length()) {
                        val call = toolCalls.optJSONObject(index) ?: continue
                        val fn = call.optJSONObject("function") ?: JSONObject()
                        add(
                            ToolCall(
                                id = call.optString("id").ifBlank { "call_$index" },
                                name = fn.optString("name"),
                                arguments = fn.optString("arguments"),
                            ),
                        )
                    }
                }
            }
            val content = when (val raw = message.opt("content")) {
                null, JSONObject.NULL -> null
                is JSONArray -> buildString {
                    for (index in 0 until raw.length()) {
                        val part = raw.optJSONObject(index) ?: continue
                        if (part.optString("type") == "text") append(part.optString("text"))
                    }
                }.ifBlank { null }
                else -> raw.toString().takeIf { it.isNotBlank() && it != "null" }
            }
            return ChatResult(
                message = ChatMessage(
                    role = message.optString("role").ifBlank { "assistant" },
                    content = content,
                    toolCalls = parsedCalls,
                ),
                finishReason = finish,
            )
        }
    }
}

internal fun List<ChatMessage>.toJson(): JSONArray {
    val array = JSONArray()
    for (message in this) {
        val obj = JSONObject().put("role", message.role)
        if (message.toolCallId != null) obj.put("tool_call_id", message.toolCallId)
        if (message.name != null) obj.put("name", message.name)
        if (message.toolCalls.isNotEmpty()) {
            val calls = JSONArray()
            for (call in message.toolCalls) {
                calls.put(
                    JSONObject()
                        .put("id", call.id)
                        .put("type", "function")
                        .put(
                            "function",
                            JSONObject()
                                .put("name", call.name)
                                .put("arguments", call.arguments),
                        ),
                )
            }
            obj.put("tool_calls", calls)
        }
        if (message.content != null || message.toolCalls.isEmpty()) {
            obj.put("content", message.content ?: "")
        }
        array.put(obj)
    }
    return array
}
