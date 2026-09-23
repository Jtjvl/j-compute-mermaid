package com.jcompute.mermaidpocket.data

/**
 * Name for an Open / Open-with `.mmd` that should join the library.
 *
 * Reuses an existing working-copy name when the incoming file is that
 * diagram. A colliding name from some other folder gets a new unique file
 * so Save does not overwrite the wrong row.
 */
object LibraryOpen {
    fun resolveName(
        fileName: String?,
        source: String,
        existingNames: Set<String>,
        existingText: (String) -> String?,
        underLibrary: Boolean,
    ): String {
        val candidate = candidateName(fileName)
        if (candidate != null) {
            val match = existingNames.firstOrNull { it.equals(candidate, ignoreCase = true) }
            if (match != null) {
                val disk = existingText(match)
                if (underLibrary || disk == null || disk == source) return match
            } else if (underLibrary || existingNames.none { it.equals(candidate, ignoreCase = true) }) {
                return candidate
            }
        }
        val slug = TitleParser.fileSlug(
            TitleParser.fromSource(source, fileName = candidate ?: fileName, fallback = "diagram"),
        )
        return uniqueAmong(slug, existingNames)
    }

    fun candidateName(fileName: String?): String? {
        val raw = fileName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val asMmd = when {
            LibraryMembership.isDiagramFileName(raw) -> raw
            raw.endsWith(".mermaid", ignoreCase = true) -> raw.dropLast(".mermaid".length) + ".mmd"
            raw.endsWith(".txt", ignoreCase = true) || raw.endsWith(".md", ignoreCase = true) -> {
                raw.substringBeforeLast('.') + ".mmd"
            }
            else -> null
        }
        return asMmd?.takeIf { LibraryMembership.isDiagramFileName(it) }
    }

    private fun uniqueAmong(slug: String, existingNames: Set<String>): String {
        val used = existingNames.toMutableSet()
        val preferred = "$slug.mmd"
        if (used.add(preferred)) return preferred
        var index = 1
        while (true) {
            val candidate = "$slug-$index.mmd"
            if (used.add(candidate)) return candidate
            index++
        }
    }
}
