package com.jcompute.mermaid.data

object TitleParser {
    fun fromSource(
        source: String,
        fileName: String? = null,
        fallback: String = "Untitled",
    ): String {
        val namedFile = fileName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.removeSuffix(".mmd")
            ?.removeSuffix(".txt")
            ?.removeSuffix(".mermaid")
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("document", ignoreCase = true) }

        val lines = source.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val commentTitle = lines.firstOrNull { it.startsWith("%%") }
            ?.removePrefix("%%")
            ?.trim()
            ?.trimStart(':', '-')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val firstLine = lines.firstOrNull { !it.startsWith("%%") }?.take(72)

        return when {
            !commentTitle.isNullOrBlank() -> commentTitle.take(72)
            !namedFile.isNullOrBlank() -> namedFile.take(72)
            !firstLine.isNullOrBlank() -> firstLine
            else -> fallback
        }
    }

    fun fileSlug(title: String): String {
        val slug = title.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
        return slug.ifBlank { "diagram" }
    }
}
