package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.DiagramStore
import com.jcompute.mermaidpocket.data.LegacyDiagramMigration
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LegacyDiagramMigrationTest {
    private lateinit var root: File
    private lateinit var store: DiagramStore

    @Before
    fun setUp() {
        root = kotlin.io.path.createTempDirectory("mermaid-migrate").toFile()
        store = DiagramStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun preexistingMmdSurvivesANewStoreAndMigration() {
        val planted = File(root, "diagrams/kept.mmd")
        planted.parentFile?.mkdirs()
        planted.writeText("flowchart TD\n  A-->B", Charsets.UTF_8)

        val reopened = DiagramStore(root)
        assertEquals(0, LegacyDiagramMigration.migrate(root, reopened))
        assertEquals(listOf("kept.mmd"), reopened.list().map { it.name })
        assertEquals("flowchart TD\n  A-->B", reopened.read(planted))
    }

    @Test
    fun legacyJsonBecomesMmdFilesAndIsNotImportedTwice() {
        File(root, "diagrams.json").writeText(
            """
            {
              "diagrams": [
                {
                  "id": "1",
                  "title": "Ignore Me",
                  "source": "flowchart TD\n  Old-->New",
                  "updatedAtMillis": 5000,
                  "originName": "keep-me.mmd"
                },
                {
                  "id": "2",
                  "title": "Second",
                  "source": "flowchart LR\n  A-->B",
                  "updatedAtMillis": 1000
                }
              ]
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val imported = LegacyDiagramMigration.migrate(root, store)
        assertEquals(2, imported)
        assertFalse(File(root, "diagrams.json").exists())
        assertTrue(File(root, "diagrams.json.migrated").isFile)

        val names = store.list().map { it.name }
        assertEquals(listOf("keep-me.mmd", "second.mmd"), names)
        assertEquals("flowchart TD\n  Old-->New", store.read(File(root, "diagrams/keep-me.mmd")))
        assertEquals(5_000L, File(root, "diagrams/keep-me.mmd").lastModified())

        assertEquals(0, LegacyDiagramMigration.migrate(root, store))
        assertEquals(2, store.list().size)
    }

    @Test
    fun identicalSourceIsNotDuplicatedAndBlankEntriesAreSkipped() {
        store.write(store.uniqueFile("already"), "flowchart TD\n  A-->B")
        File(root, "diagrams.json").writeText(
            """
            {
              "diagrams": [
                {"id":"1","title":"Already","source":"flowchart TD\n  A-->B","updatedAtMillis":1},
                {"id":"2","title":"Empty","source":"","updatedAtMillis":2},
                {"id":"3","title":"Quoted \"title\"","source":"sequenceDiagram\n  Alice->>Bob: hi \"x\"","updatedAtMillis":9}
              ]
            }
            """.trimIndent(),
            Charsets.UTF_8,
        )

        assertEquals(1, LegacyDiagramMigration.migrate(root, store))
        val names = store.list().map { it.name }
        assertTrue(names.contains("already.mmd"))
        assertTrue(names.any { it.startsWith("quoted-title") })
        assertEquals(2, names.size)
        val imported = store.list().first { it.name.startsWith("quoted-title") }
        assertTrue(store.read(imported.file).contains("hi \"x\""))
    }

    @Test
    fun corruptJsonDoesNotDeleteExistingMmd() {
        val planted = store.write(store.uniqueFile("safe"), "flowchart TD\n  K-->L")
        File(root, "diagrams.json").writeText("{ not json", Charsets.UTF_8)

        assertEquals(0, LegacyDiagramMigration.migrate(root, store))
        assertTrue(planted.exists())
        assertEquals(listOf("safe.mmd"), store.list().map { it.name })
        assertFalse(File(root, "diagrams.json").exists())
        assertTrue(File(root, "diagrams.json.unreadable").isFile)
        assertTrue(File(root, "diagrams.json.unreadable").readText().contains("not json"))
    }
}
