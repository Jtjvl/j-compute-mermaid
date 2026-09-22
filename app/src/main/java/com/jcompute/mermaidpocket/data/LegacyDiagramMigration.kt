package com.jcompute.mermaidpocket.data

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pre-v1 builds stored diagrams in `filesDir/diagrams.json` instead of
 * filesDir/diagrams as one .mmd file per diagram. That switch hid existing saves from Files.
 * First launch copies each entry into a `.mmd` file and retires the JSON.
 * The `.mmd` text itself is not rewritten.
 */
object LegacyDiagramMigration {
    const val LEGACY_NAME = "diagrams.json"
    const val MIGRATED_SUFFIX = ".migrated"
    const val UNREADABLE_SUFFIX = ".unreadable"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun migrate(filesDir: File, store: DiagramStore): Int {
        val legacy = File(filesDir, LEGACY_NAME)
        if (!legacy.isFile) return 0
        val parsed = runCatching {
            json.decodeFromString(LegacyStore.serializer(), legacy.readText(Charsets.UTF_8))
        }
        if (parsed.isFailure) {
            retire(legacy, UNREADABLE_SUFFIX)
            return 0
        }
        val existing = store.list().map { store.read(it.file) }.toMutableSet()
        var imported = 0
        for (diagram in parsed.getOrThrow().diagrams) {
            if (diagram.source.isEmpty() || diagram.source in existing) continue
            val slug = fileSlug(diagram.title, diagram.originName)
            val file = store.write(store.uniqueFile(slug), diagram.source)
            if (diagram.updatedAtMillis > 0L) file.setLastModified(diagram.updatedAtMillis)
            existing.add(diagram.source)
            imported++
        }
        retire(legacy, MIGRATED_SUFFIX)
        return imported
    }

    fun fileSlug(title: String, originName: String?): String {
        val fromOrigin = originName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
            ?.removeSuffix(".mmd")
            ?.removeSuffix(".txt")
            ?.removeSuffix(".mermaid")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        return TitleParser.fileSlug(fromOrigin ?: title.ifBlank { "diagram" })
    }

    private fun retire(file: File, suffix: String) {
        val dest = File(file.parentFile, file.name + suffix)
        if (dest.exists()) dest.delete()
        if (!file.renameTo(dest)) {
            dest.writeBytes(file.readBytes())
            if (!file.delete()) return
        }
    }

    @Serializable
    private data class LegacyStore(
        val diagrams: List<LegacyDiagram> = emptyList(),
    )

    @Serializable
    private data class LegacyDiagram(
        val id: String = "",
        val title: String = "",
        val source: String = "",
        val updatedAtMillis: Long = 0L,
        val originName: String? = null,
    )
}
