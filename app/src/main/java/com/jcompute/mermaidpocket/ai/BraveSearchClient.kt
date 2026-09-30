package com.jcompute.mermaidpocket.ai

import java.net.URLEncoder
import org.json.JSONObject

data class SearchHit(
    val title: String,
    val url: String,
    val snippet: String,
)

class BraveSearchClient(
    private val http: JsonHttp,
    private val endpoint: String = AiDefaults.BRAVE_SEARCH_URL,
) {
    suspend fun search(apiKey: String, query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val encoded = URLEncoder.encode(q.take(400), Charsets.UTF_8.name())
        val url = "$endpoint?q=$encoded&count=${AiDefaults.SEARCH_RESULT_COUNT}"
        val headers = mapOf(
            "Accept" to "application/json",
            "X-Subscription-Token" to apiKey.trim(),
        )
        val json = http.request("GET", url, headers, body = null)
        return parse(json)
    }

    companion object {
        fun parse(json: String): List<SearchHit> {
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
            val results = root.optJSONObject("web")?.optJSONArray("results") ?: return emptyList()
            val hits = ArrayList<SearchHit>(results.length())
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                hits.add(
                    SearchHit(
                        title = item.optString("title"),
                        url = item.optString("url"),
                        snippet = item.optString("description"),
                    ),
                )
            }
            return hits
        }

        fun format(hits: List<SearchHit>): String {
            if (hits.isEmpty()) return "No search results."
            return hits.joinToString("\n\n") { hit ->
                buildString {
                    append(hit.title.ifBlank { "(untitled)" })
                    if (hit.url.isNotBlank()) append("\n").append(hit.url)
                    if (hit.snippet.isNotBlank()) append("\n").append(hit.snippet)
                }
            }
        }
    }
}
