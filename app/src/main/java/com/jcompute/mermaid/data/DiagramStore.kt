package com.jcompute.mermaid.data

import java.io.File

data class StoredDiagram(
    val file: File,
    val name: String,
    val updatedAtMillis: Long,
    val thumbnail: File? = null,
)

class DiagramStore(private val filesDir: File) {
    private val dir: File = File(filesDir, "diagrams").apply { mkdirs() }
    private val thumbsDir: File = File(dir, ".thumbs").apply { mkdirs() }

    fun list(): List<StoredDiagram> {
        dir.mkdirs()
        thumbsDir.mkdirs()
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".mmd", ignoreCase = true) }
            .orEmpty()
            .map { file ->
                val thumb = thumbnailFile(file)
                StoredDiagram(
                    file = file,
                    name = file.name,
                    updatedAtMillis = file.lastModified(),
                    thumbnail = thumb.takeIf { it.isFile && it.length() > 0L },
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
        if (!owned(diagram) || png.isEmpty()) return
        thumbsDir.mkdirs()
        thumbnailFile(diagram).writeBytes(png)
    }

    fun create(title: String, source: String): File {
        val file = uniqueFile(TitleParser.fileSlug(title))
        return write(file, source)
    }

    fun delete(file: File) {
        if (!owned(file)) return
        file.delete()
        thumbnailFile(file).delete()
    }

    fun deleteAll(files: Collection<File>): Int {
        var removed = 0
        files.forEach { file ->
            val existed = owned(file) && file.exists()
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

    private fun owned(file: File): Boolean {
        return runCatching { file.parentFile?.canonicalPath == dir.canonicalPath }.getOrDefault(false)
    }
}
