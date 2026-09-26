package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.LibraryChildRow
import com.jcompute.mermaidpocket.data.LibraryChildren
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryChildrenTest {
    @Test
    fun sevenSiblingMmdFilesAreListedEvenWhenFourHaveNullMime() {
        val rows = listOf(
            LibraryChildRow("one.mmd", mimeType = "text/plain", lastModified = 1),
            LibraryChildRow("two.mmd", mimeType = "text/plain", lastModified = 2),
            LibraryChildRow("three.mmd", mimeType = "text/plain", lastModified = 3),
            LibraryChildRow("four.mmd", mimeType = null, lastModified = 4),
            LibraryChildRow("five.mmd", mimeType = null, lastModified = 5),
            LibraryChildRow("six.mmd", mimeType = null, lastModified = 6),
            LibraryChildRow("seven.mmd", mimeType = null, lastModified = 7),
            LibraryChildRow("notes.txt", mimeType = "text/plain", lastModified = 8),
            LibraryChildRow("photo.png", mimeType = "image/png", lastModified = 9),
            LibraryChildRow("extra.mmd", mimeType = LibraryChildren.DIRECTORY_MIME, lastModified = 10),
        )
        val isFileKeeps = rows.count { row ->
            LibraryChildren.documentFileIsFile(row.mimeType) &&
                row.name.orEmpty().endsWith(".mmd", ignoreCase = true)
        }
        assertEquals(3, isFileKeeps)

        val listed = LibraryChildren.diagrams(rows)
        assertEquals(8, listed.size)
        assertEquals(
            listOf(
                "one.mmd",
                "two.mmd",
                "three.mmd",
                "four.mmd",
                "five.mmd",
                "six.mmd",
                "seven.mmd",
                "notes.txt",
            ),
            listed.map { it.name },
        )
        assertFalse(listed.any { it.name == "photo.png" })
        assertFalse(listed.any { it.name == "extra.mmd" })
    }

    @Test
    fun unknownMimeAndMissingDisplayNameStillCountWhenTheIdEndsWithMmd() {
        val rows = listOf(
            LibraryChildRow(name = null, mimeType = null, documentId = "primary:Documents/MermaidPocket/from-id.mmd"),
            LibraryChildRow("Diagram.MMD", mimeType = "application/octet-stream"),
            LibraryChildRow("older", mimeType = null),
        )
        assertEquals(
            listOf("from-id.mmd", "Diagram.MMD", "older"),
            LibraryChildren.diagrams(rows).map { it.name },
        )
    }

    @Test
    fun sharedTitlesDoNotCollapseSiblingNames() {
        val rows = (1..7).map { index ->
            LibraryChildRow("q$index.mmd", mimeType = if (index <= 3) "text/plain" else null)
        }
        assertEquals(7, LibraryChildren.diagrams(rows).map { it.name }.toSet().size)
    }

    @Test
    fun durableListDoesNotRequireDocumentFileIsFile() {
        val durable = source("app/src/main/java/com/jcompute/mermaidpocket/data/DurableDocuments.kt")
        assertTrue(durable.contains("LibraryChildren.diagrams"))
        assertTrue(durable.contains("queryChildren"))
        assertTrue(durable.contains("QUERY_ARG_LIMIT"))
        assertFalse(durable.contains("doc.isFile"))
        assertFalse(durable.contains("it.isFile"))
        assertTrue(durable.contains("direct children only"))
    }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
