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
        val relative = relativePath(libraryDocumentId, documentId) ?: return null
        if ('/' in relative || '\\' in relative) return null
        if (!isDiagramFileName(relative)) return null
        return relative
    }

    /**
     * Path under the library document, or null when [documentId] is not a descendant.
     * Nested segments stay slash-separated (`nested/flow.mmd`).
     */
    fun relativePath(libraryDocumentId: String?, documentId: String?): String? {
        val library = libraryDocumentId?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: return null
        val document = documentId?.takeIf { it.isNotEmpty() } ?: return null
        val prefix = "$library/"
        if (!document.startsWith(prefix)) return null
        val relative = document.removePrefix(prefix)
        val parts = relative.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.any { it == ".." || it == "." }) return null
        return parts.joinToString("/")
    }

    /**
     * Working-copy name for a library descendant `.mmd`.
     * A listed basename wins when it is already in [libraryNames]; otherwise a
     * flattened relative path is used so nested files can be indexed.
     */
    fun descendantName(
        libraryDocumentId: String?,
        documentId: String?,
        libraryNames: Set<String>,
    ): String? {
        val relative = relativePath(libraryDocumentId, documentId) ?: return null
        val base = LibraryListing.basename(relative)
        if (!isDiagramFileName(base)) return null
        if (base in libraryNames) return base
        val flat = LibraryListing.flatten(relative)
        if (flat in libraryNames) return flat
        return if (base == relative) base else flat
    }

    /**
     * Name to upsert after an `.mmd` export, or null when this write is not a library file.
     * A descendant of the library wins. Otherwise [displayName] counts only when that
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
        descendantName(libraryDocumentId, documentId, libraryNames)?.let { return it }
        val candidate = displayName?.takeIf { isDiagramFileName(it) && it in libraryNames } ?: return null
        val existing = libraryText(candidate) ?: return null
        return candidate.takeIf { existing == writtenText }
    }
}
