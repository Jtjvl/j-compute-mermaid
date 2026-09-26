package com.jcompute.mermaidpocket.data

/**
 * Direct children of the library folder that should appear in Files.
 *
 * [androidx.documentfile.provider.DocumentFile.isFile] is false when MIME is
 * null. Desktop-copied diagrams often have no MIME (the app writes
 * `text/plain`). Those siblings must still be listed, including non-`.mmd`
 * text names. Skip directories and obvious binaries/images. Identity is the
 * file name, not title or slug.
 */
data class LibraryChildRow(
    val name: String?,
    val mimeType: String?,
    val documentId: String? = null,
    val lastModified: Long = 0L,
)

object LibraryChildren {
    /** DocumentsContract.Document.MIME_TYPE_DIR — kept as a literal for JVM tests. */
    const val DIRECTORY_MIME = "vnd.android.document/directory"

    fun isDirectory(mimeType: String?): Boolean = mimeType == DIRECTORY_MIME

    /**
     * What DocumentFile.isFile() would decide: non-null MIME that is not a folder.
     * Null MIME (unknown type) is **not** a file under that check.
     */
    fun documentFileIsFile(mimeType: String?): Boolean {
        return mimeType != null && !isDirectory(mimeType)
    }

    fun displayName(name: String?, documentId: String?): String? {
        name?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return documentId
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun isVisibleDiagram(name: String?, mimeType: String?, documentId: String? = null): Boolean {
        if (isDirectory(mimeType)) return false
        val display = displayName(name, documentId) ?: return false
        if (!LibraryMembership.isSafeFileName(display)) return false
        if (LibraryMembership.isMermaidLibraryName(display)) return true
        if (!LibraryMembership.isDiagramFileName(display)) return false
        return !LibraryMembership.isExcludedBinaryMime(mimeType)
    }

    fun diagrams(rows: List<LibraryChildRow>): List<LibraryChildRow> {
        return rows.mapNotNull { row ->
            val display = displayName(row.name, row.documentId) ?: return@mapNotNull null
            if (!isVisibleDiagram(display, row.mimeType, row.documentId)) return@mapNotNull null
            row.copy(name = display)
        }
    }
}
