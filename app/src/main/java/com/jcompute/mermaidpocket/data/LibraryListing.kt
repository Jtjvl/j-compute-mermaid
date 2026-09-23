package com.jcompute.mermaidpocket.data

/**
 * How nested library `.mmd` files become flat working-copy names.
 *
 * The durable folder may have diagrams in subdirectories. [DiagramStore] is
 * one directory of `.mmd` files, so each relative path gets a store-safe name.
 * A unique basename stays as-is (root files keep today's names). Colliding
 * basenames use the folder prefix: `nested/flow.mmd` → `nested-flow.mmd`.
 */
object LibraryListing {
    data class Entry(
        val relativePath: String,
        val modifiedAt: Long,
    )

    fun isDiagramFileName(name: String): Boolean = LibraryMembership.isDiagramFileName(name)

    fun basename(relativePath: String): String {
        return relativePath.replace('\\', '/').substringAfterLast('/')
    }

    fun flatten(relativePath: String): String {
        val parts = relativePath.replace('\\', '/').split('/').filter { part ->
            part.isNotEmpty() && part != "."
        }
        require(parts.isNotEmpty() && parts.none { it == ".." }) { "Not a diagram path" }
        if (parts.size == 1) return parts[0]
        return parts.joinToString("-")
    }

    /**
     * Relative path → store name. Order is stable so the same tree always
     * maps the same way. When two files would flatten to the same string,
     * later ones get `-1`, `-2`, …
     */
    fun storeNames(relativePaths: List<String>): Map<String, String> {
        val uniqueBases = relativePaths
            .map { basename(it) }
            .groupingBy { it.lowercase() }
            .eachCount()
        val used = mutableSetOf<String>()
        val assigned = linkedMapOf<String, String>()
        for (path in relativePaths.sorted()) {
            val base = basename(path)
            val preferred = if (uniqueBases[base.lowercase()] == 1) base else flatten(path)
            assigned[path] = uniquify(preferred, used)
        }
        return assigned
    }

    fun uniquify(preferred: String, used: MutableSet<String>): String {
        if (used.add(preferred)) return preferred
        val stem = preferred.removeSuffix(".mmd").removeSuffix(".MMD").removeSuffix(".Mmd")
        var index = 1
        while (true) {
            val candidate = "$stem-$index.mmd"
            if (used.add(candidate)) return candidate
            index++
        }
    }

    fun hasMmd(nodes: List<LibraryNode>): Boolean {
        return nodes.any { node ->
            if (node.directory) hasMmd(node.children) else isDiagramFileName(node.name)
        }
    }

    /** `.mmd` under a subdirectory that is not the MermaidPocket child. */
    fun hasMmdOutsidePocket(children: List<LibraryNode>): Boolean {
        return children.any { child ->
            child.directory &&
                !child.name.equals(LibraryRoot.FOLDER_NAME, ignoreCase = true) &&
                hasMmd(listOf(child))
        }
    }
}

/** Tree node used by [LibraryRoot.choose] when nested files matter. */
data class LibraryNode(
    val name: String,
    val directory: Boolean,
    val children: List<LibraryNode> = emptyList(),
)
