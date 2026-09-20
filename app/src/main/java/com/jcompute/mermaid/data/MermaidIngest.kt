package com.jcompute.mermaid.data

object MermaidIngest {
    private val taggedFence = Regex(
        """```mermaid[ \t]*\r?\n([\s\S]*?)```""",
        RegexOption.IGNORE_CASE,
    )
    private val wrappedDocument = Regex(
        """^```[a-zA-Z0-9_-]*[ \t]*\r?\n([\s\S]*?)```\s*$""",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val anyFence = Regex(
        """```[a-zA-Z0-9_-]*[ \t]*\r?\n([\s\S]*?)```""",
    )

    fun extractSource(raw: String): String {
        val text = raw.replace("\uFEFF", "").trim()
        if (text.isEmpty()) return text

        taggedFence.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        wrappedDocument.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        if (looksLikeMarkdown(text)) {
            anyFence.find(text)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }

        return text
    }

    fun looksLikeMarkdown(text: String): Boolean {
        return text.contains("```") ||
            text.lineSequence().any { it.trimStart().startsWith("#") }
    }
}
