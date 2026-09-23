package com.jcompute.mermaidpocket.ui

import com.jcompute.mermaidpocket.data.DiagramSync
import com.jcompute.mermaidpocket.data.LocalWrite
import com.jcompute.mermaidpocket.data.TitleParser
import java.io.File

/**
 * Library write for the Edit Save control.
 *
 * A linked file is overwritten in place. A blank is not attached to any existing
 * file: the first save creates a new `.mmd` and returns it so the editor can link it.
 */
object LibrarySave {
    fun commit(
        sync: DiagramSync,
        currentFile: File?,
        title: String,
        source: String,
        thumbnail: ByteArray? = null,
    ): LocalWrite {
        val target = currentFile ?: sync.uniqueFile(TitleParser.fileSlug(title))
        val written = sync.write(target, source)
        if (thumbnail != null) sync.writeThumbnail(written.file, thumbnail)
        return written
    }
}
