package com.jcompute.mermaidpocket.data

/**
 * Whether a system-picker `.mmd` write belongs in the Files list.
 *
 * Files lists working copies in [DiagramStore]. A Save-a-copy that lands in the
 * granted library folder has to be imported there; a write to some other folder does not.
 *
 * External-storage document ids look like `primary:Documents/MermaidPocket/copy.mmd`.
 * Other providers use opaque ids, so a listed library file whose text matches the
 * bytes just written counts too.
 */
object LibraryMembership {
    fun isDiagramFileName(name: String): Boolean {
        if (name.isBlank()) return false
        if ('/' in name || '\\' in name || ".." in name) return false
        if (name.equals(".mmd", ignoreCase = true)) return false
        return name.endsWith(".mmd", ignoreCase = true)
    }

    /** File name when [documentId] is a direct `.mmd` child of [libraryDocumentId]. */
    fun directChildName(libraryDocumentId: String?, documentId: String?): String? {
        val library = libraryDocumentId?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: return null
        val document = documentId?.takeIf { it.isNotEmpty() } ?: return null
        val prefix = "$library/"
        if (!document.startsWith(prefix)) return null
        val name = document.removePrefix(prefix)
        if (!isDiagramFileName(name)) return null
        return name
    }

    /**
     * Name to upsert after an `.mmd` export, or null when this write is not a library file.
     * A direct child of the library wins. Otherwise [displayName] counts only when that
     * library file already exists and its text is the text just written.
     */
    fun nameToIndex(
        libraryDocumentId: String?,
        documentId: String?,
        displayName: String?,
        libraryNames: Set<String>,
        writtenText: String,
        libraryText: (String) -> String?,
    ): String? {
        directChildName(libraryDocumentId, documentId)?.let { return it }
        val candidate = displayName?.takeIf { isDiagramFileName(it) && it in libraryNames } ?: return null
        val existing = libraryText(candidate) ?: return null
        return candidate.takeIf { existing == writtenText }
    }
}
