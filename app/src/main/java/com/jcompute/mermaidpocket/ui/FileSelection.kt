package com.jcompute.mermaidpocket.ui

data class FileSelection(
    val active: Boolean = false,
    val paths: Set<String> = emptySet(),
) {
    val count: Int get() = paths.size

    fun enter(path: String) = FileSelection(active = true, paths = setOf(path))

    fun toggle(path: String): FileSelection {
        if (!active) return enter(path)
        return copy(paths = if (path in paths) paths - path else paths + path)
    }

    fun contains(path: String): Boolean = path in paths

    fun clear() = FileSelection()

    fun prune(existingPaths: Set<String>) = copy(paths = paths.intersect(existingPaths))
}
