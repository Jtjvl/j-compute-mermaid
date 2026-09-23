package com.jcompute.mermaidpocket.data

import java.io.File

data class StoredDiagram(
    val file: File,
    val name: String,
    val updatedAtMillis: Long,
    val thumbnail: File? = null,
    /** Parsed diagram title. List identity is [file], never this string. */
    val title: String = name,
)

/**
 * Working copies of plain UTF-8 `.mmd` files in [filesDir]`/diagrams`.
 *
 * This directory is internal app storage. An upgrade of the same applicationId
 * keeps it, and nothing here deletes it on startup. Uninstall wipes it.
 * [DurableDocuments] mirrors the files to `Documents/MermaidPocket`, which
 * stays on the device. Thumbnails live in `diagrams/.thumbs` and are regenerable.
 */
class DiagramStore(private val filesDir: File) {
    private val dir: File = File(filesDir, "diagrams").apply { mkdirs() }
    private val thumbsDir: File = File(dir, ".thumbs").apply { mkdirs() }

    fun list(): List<StoredDiagram> {
        dir.mkdirs()
        thumbsDir.mkdirs()
        // One row per file path. Do not collapse diagrams that share a title.
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".mmd", ignoreCase = true) }
            .orEmpty()
            .map { file ->
                val thumb = thumbnailFile(file)
                val text = runCatching { read(file) }.getOrDefault("")
                StoredDiagram(
                    file = file,
                    name = file.name,
                    updatedAtMillis = file.lastModified(),
                    thumbnail = thumb.takeIf { it.isFile && it.length() > 0L },
                    title = TitleParser.fromSource(text, fileName = file.name, fallback = file.name),
                )
            }
            .sortedByDescending { it.updatedAtMillis }
    }

    fun read(file: File): String = file.readText(Charsets.UTF_8)

    fun write(file: File, source: String): File {
        file.parentFile?.mkdirs()
        file.writeText(source, Charsets.UTF_8)
        return file
    }

    fun thumbnailFile(diagram: File): File = File(thumbsDir, "${diagram.name}.png")

    fun writeThumbnail(diagram: File, png: ByteArray) {
        if (!owns(diagram) || png.isEmpty()) return
        thumbsDir.mkdirs()
        thumbnailFile(diagram).writeBytes(png)
    }

    fun create(title: String, source: String): File {
        val file = uniqueFile(TitleParser.fileSlug(title))
        return write(file, source)
    }

    fun delete(file: File) {
        if (!owns(file)) return
        file.delete()
        thumbnailFile(file).delete()
    }

    fun deleteAll(files: Collection<File>): Int {
        var removed = 0
        files.forEach { file ->
            val existed = owns(file) && file.exists()
            delete(file)
            if (existed) removed++
        }
        return removed
    }

    fun uniqueFile(slug: String): File {
        val base = slug.ifBlank { "untitled" }
        var index = 0
        while (true) {
            val name = if (index == 0) "$base.mmd" else "$base-$index.mmd"
            val candidate = File(dir, name)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    /**
     * Write a diagram that already has a file name (restore from the durable folder).
     * Does not invent a new slug.
     */
    fun importFile(name: String, source: String, modifiedAt: Long): File {
        require(LibraryMembership.isDiagramFileName(name)) { "Not a diagram name" }
        val file = File(dir, name)
        val parent = file.parentFile?.canonicalFile
        check(parent == dir.canonicalFile) { "Diagram path escaped the library" }
        val written = write(file, source)
        if (modifiedAt > 0L) written.setLastModified(modifiedAt)
        return written
    }

    fun owns(file: File): Boolean {
        return runCatching { file.parentFile?.canonicalPath == dir.canonicalPath }.getOrDefault(false)
    }
}
