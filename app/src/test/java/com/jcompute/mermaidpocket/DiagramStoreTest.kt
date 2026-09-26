package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.DiagramStore
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagramStoreTest {
    private lateinit var root: File
    private lateinit var store: DiagramStore

    @Before
    fun setUp() {
        root = kotlin.io.path.createTempDirectory("mermaid-store").toFile()
        store = DiagramStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun listsMmdNewestFirstAndIgnoresThumbs() {
        val older = store.write(store.uniqueFile("alpha"), "flowchart TD\n A-->B")
        val newer = store.write(store.uniqueFile("beta"), "flowchart TD\n C-->D")
        older.setLastModified(1_000L)
        newer.setLastModified(2_000L)
        store.writeThumbnail(newer, byteArrayOf(1, 2, 3, 4))

        store.importFile("older.txt", "flowchart TD\n X-->Y", 500L)
        val names = store.list().map { it.name }
        assertEquals(listOf("beta.mmd", "alpha.mmd", "older.txt"), names)
        assertTrue(root.resolve("diagrams").listFiles().orEmpty().none { it.extension.equals("png", true) })
    }

    @Test
    fun thumbnailSidecarIsReturnedThenRemovedWithFile() {
        val file = store.write(store.uniqueFile("flow"), "flowchart TD\n A-->B")
        assertNull(store.list().single().thumbnail)

        store.writeThumbnail(file, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        val listed = store.list().single()
        assertNotNull(listed.thumbnail)
        assertTrue(listed.thumbnail!!.isFile)
        assertEquals(store.thumbnailFile(file).canonicalPath, listed.thumbnail!!.canonicalPath)

        store.delete(file)
        assertTrue(store.list().isEmpty())
        assertFalse(store.thumbnailFile(file).exists())
    }

    @Test
    fun bulkDeleteRemovesOnlyOwnedFilesAndTheirThumbs() {
        val keep = store.write(store.uniqueFile("keep"), "flowchart TD\n K-->L")
        val goneA = store.write(store.uniqueFile("gone-a"), "flowchart TD\n A-->B")
        val goneB = store.write(store.uniqueFile("gone-b"), "flowchart TD\n C-->D")
        store.writeThumbnail(goneA, byteArrayOf(1, 2, 3))
        val outsider = File(root, "outsider.mmd").apply { writeText("nope") }

        val removed = store.deleteAll(listOf(goneA, goneB, outsider))
        assertEquals(2, removed)
        assertEquals(listOf("keep.mmd"), store.list().map { it.name })
        assertTrue(keep.exists())
        assertFalse(goneA.exists())
        assertFalse(store.thumbnailFile(goneA).exists())
        assertTrue(outsider.exists())
    }

    @Test
    fun sameParsedTitleStillListsEveryFile() {
        val shared = "%% Shared\nflowchart TD\n  A-->B"
        store.importFile("one.mmd", shared, 1_000L)
        store.importFile("two.mmd", shared, 2_000L)
        store.importFile("three.mmd", "%% Shared\nflowchart LR\n  C-->D", 3_000L)
        store.importFile("four.mmd", "%% Shared\nsequenceDiagram\n  A->>B: hi", 4_000L)

        val listed = store.list()
        assertEquals(4, listed.size)
        assertEquals(setOf("one.mmd", "two.mmd", "three.mmd", "four.mmd"), listed.map { it.name }.toSet())
        assertTrue(listed.all { it.title == "Shared" })
        assertEquals(4, listed.map { it.file.absolutePath }.distinct().size)
        val source = File("app/src/main/java/com/jcompute/mermaidpocket/data/DiagramStore.kt")
            .let { file -> if (file.exists()) file else File("../app/src/main/java/com/jcompute/mermaidpocket/data/DiagramStore.kt") }
            .readText()
        assertFalse(source.contains("distinctBy"))
    }

    @Test
    fun emptyThumbnailBytesAreIgnored() {
        val file = store.write(store.uniqueFile("empty"), "flowchart TD\n A-->B")
        store.writeThumbnail(file, byteArrayOf())
        assertNull(store.list().single().thumbnail)
    }
}
