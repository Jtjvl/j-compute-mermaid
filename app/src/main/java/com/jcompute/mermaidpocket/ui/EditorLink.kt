package com.jcompute.mermaidpocket.ui

import com.jcompute.mermaidpocket.data.MermaidIngest
import com.jcompute.mermaidpocket.data.TitleParser
import java.io.File

/**
 * Which library file the open buffer will overwrite on Save.
 *
 * [stored] keeps the link. [imported] drops it: paste, Open, and share-intent
 * load text only, so the next Save creates a new `.mmd` instead of writing
 * over the diagram that happened to be open before.
 */
internal data class LinkedDocument(
    val currentFile: File?,
    val source: String,
    val savedSource: String,
    val title: String,
) {
    val isDirty: Boolean get() = source != savedSource
}

internal object EditorLink {
    fun stored(file: File, source: String): LinkedDocument {
        return LinkedDocument(
            currentFile = file,
            source = source,
            savedSource = source,
            title = TitleParser.fromSource(source, fileName = file.name, fallback = "Untitled"),
        )
    }

    fun imported(raw: String, fileName: String?): LinkedDocument {
        val source = MermaidIngest.extractSource(raw)
        return LinkedDocument(
            currentFile = null,
            source = source,
            savedSource = "",
            title = TitleParser.fromSource(source, fileName = fileName, fallback = "Untitled"),
        )
    }
}
