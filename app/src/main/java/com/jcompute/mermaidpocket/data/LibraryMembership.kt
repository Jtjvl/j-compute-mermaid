package com.jcompute.mermaidpocket.data

/**
 * Whether a file belongs in the Files list / Open / Save identity.
 *
 * New blank diagrams still use `.mmd`. Opened or scanned siblings do not have
 * to: `.txt`, `.md`, no extension, and other readable text names stay as-is.
 * Path escapes and obvious binaries/images stay out.
 *
 * External-storage document ids look like `primary:Documents/MermaidPocket/copy.mmd`.
 * Other providers use opaque ids, so a listed library file whose text matches the
 * bytes just written counts too.
 */
object LibraryMembership {
    private val excludedExtensions = setOf(
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "heic", "heif", "tif", "tiff", "svg",
        "mp3", "mp4", "m4a", "aac", "wav", "flac", "ogg", "webm", "mov", "avi", "mkv",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "zip", "rar", "7z", "tar", "gz", "tgz", "bz2",
        "apk", "aab", "exe", "dll", "so", "jar", "class", "dex",
        "bin", "dat", "db", "sqlite",
        "ttf", "otf", "woff", "woff2",
        "psd", "ai",
    )

    fun isSafeFileName(name: String): Boolean {
        if (name.isBlank()) return false
        if ('/' in name || '\\' in name) return false
        if (name == "." || name == "..") return false
        return true
    }

    /**
     * Signal that a picked folder already holds mermaid sources, so it is the
     * library (instead of nesting `MermaidPocket`). Keep this tighter than
     * [isDiagramFileName]: a `Documents` tree with a stray `.txt` must still
     * use the `MermaidPocket` child.
     */
    fun isMermaidLibraryName(name: String): Boolean {
        if (!isSafeFileName(name)) return false
        if (name.equals(".mmd", ignoreCase = true) || name.equals(".mermaid", ignoreCase = true)) {
            return false
        }
        return name.endsWith(".mmd", ignoreCase = true) ||
            name.endsWith(".mermaid", ignoreCase = true)
    }

    /**
     * Working-copy / Files / Open / Save identity. Not only `.mmd`.
     */
    fun isDiagramFileName(name: String): Boolean {
        if (!isSafeFileName(name)) return false
        if (name.startsWith(".")) return false
        return !isExcludedBinaryName(name)
    }

    fun isExcludedBinaryName(name: String): Boolean {
        val extension = fileExtension(name) ?: return false
        return extension in excludedExtensions
    }

    fun isExcludedBinaryMime(mimeType: String?): Boolean {
        if (mimeType == null) return false
        val mime = mimeType.lowercase()
        if (mime == LibraryChildren.DIRECTORY_MIME) return true
        if (mime.startsWith("image/") || mime.startsWith("audio/") || mime.startsWith("video/")) {
            return true
        }
        if (mime == "application/pdf" || mime == "application/zip" || mime == "application/gzip") {
            return true
        }
        return mime.startsWith("application/vnd.")
    }

    fun fileExtension(name: String): String? {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) return null
        return name.substring(dot + 1).lowercase()
    }

    /** File name when [documentId] is a direct library child of [libraryDocumentId]. */
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
     * Name to upsert after a source export, or null when this write is not a library file.
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
