package com.jcompute.mermaidpocket.data

/**
 * Where the granted tree's `.mmd` files live.
 *
 * A folder the user picked that already contains `.mmd` files is the library.
 * [MermaidPocket] is used or created only when that folder is an ancestor with
 * no `.mmd` files of its own (the usual case is `Documents`).
 */
internal enum class LibraryRootChoice {
    Picked,
    ExistingChild,
    CreateChild,
}

internal data class LibraryChild(
    val name: String,
    val directory: Boolean,
)

internal object LibraryRoot {
    const val FOLDER_NAME = "MermaidPocket"

    fun choose(pickedName: String?, children: List<LibraryChild>): LibraryRootChoice {
        if (pickedName.equals(FOLDER_NAME, ignoreCase = true)) return LibraryRootChoice.Picked
        val hasMmd = children.any { child ->
            !child.directory && child.name.endsWith(".mmd", ignoreCase = true)
        }
        if (hasMmd) return LibraryRootChoice.Picked
        val hasPocket = children.any { child ->
            child.directory && child.name.equals(FOLDER_NAME, ignoreCase = true)
        }
        return if (hasPocket) LibraryRootChoice.ExistingChild else LibraryRootChoice.CreateChild
    }
}
