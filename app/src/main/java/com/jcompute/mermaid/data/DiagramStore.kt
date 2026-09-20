package com.jcompute.mermaid.data

import java.io.File

data class StoredDiagram(
    val file: File,
    val name: String,
    val updatedAtMillis: Long,
)

class DiagramStore(private val filesDir: File) {
    private val dir: File = File(filesDir, "diagrams").apply { mkdirs() }

    fun list(): List<StoredDiagram> {
        dir.mkdirs()
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".mmd", ignoreCase = true) }
            .orEmpty()
            .map { file ->
                StoredDiagram(
                    file = file,
                    name = file.name,
                    updatedAtMillis = file.lastModified(),
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

    fun create(title: String, source: String): File {
        val file = uniqueFile(TitleParser.fileSlug(title))
        return write(file, source)
    }

    fun delete(file: File) {
        if (file.parentFile?.canonicalPath == dir.canonicalPath) {
            file.delete()
        }
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
}
