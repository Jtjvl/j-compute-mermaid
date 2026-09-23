package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.LibraryMembership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryMembershipTest {
    private val library = "primary:Documents/MermaidPocket"

    @Test
    fun directChildMmdIsTheLibraryFileName() {
        assertEquals(
            "flow-copy.mmd",
            LibraryMembership.directChildName(library, "$library/flow-copy.mmd"),
        )
        assertEquals(
            "Flow Copy.MMD",
            LibraryMembership.directChildName("$library/", "$library/Flow Copy.MMD"),
        )
    }

    @Test
    fun nestedSiblingAndNonMmdDestinationsStayOut() {
        assertNull(LibraryMembership.directChildName(library, "$library/nested/flow.mmd"))
        assertNull(LibraryMembership.directChildName(library, "primary:Documents/MermaidPocketBackup/flow.mmd"))
        assertNull(LibraryMembership.directChildName(library, "primary:Download/flow.mmd"))
        assertNull(LibraryMembership.directChildName(library, "$library/notes.txt"))
        assertNull(LibraryMembership.directChildName(library, "$library/.mmd"))
        assertNull(LibraryMembership.directChildName(library, library))
        assertNull(LibraryMembership.directChildName(null, "$library/flow.mmd"))
        assertNull(LibraryMembership.directChildName(library, null))
        assertFalse(LibraryMembership.isDiagramFileName("../escape.mmd"))
        assertFalse(LibraryMembership.isDiagramFileName("nested/flow.mmd"))
        assertTrue(LibraryMembership.isDiagramFileName("rev..2.mmd"))
        assertFalse(LibraryMembership.isDiagramFileName(".."))
    }

    @Test
    fun opaqueIdIndexesOnlyAListedFileWithTheSameText() {
        val names = setOf("flow-copy.mmd")
        val source = "flowchart TD\n  A-->C"
        assertEquals(
            "flow-copy.mmd",
            LibraryMembership.nameToIndex(
                libraryDocumentId = library,
                documentId = "msf:100",
                displayName = "flow-copy.mmd",
                libraryNames = names,
                writtenText = source,
                libraryText = { source },
            ),
        )
        assertNull(
            LibraryMembership.nameToIndex(
                libraryDocumentId = library,
                documentId = "msf:100",
                displayName = "flow-copy.mmd",
                libraryNames = names,
                writtenText = "exported elsewhere",
                libraryText = { "original" },
            ),
        )
        assertNull(
            LibraryMembership.nameToIndex(
                libraryDocumentId = library,
                documentId = "primary:Download/other.mmd",
                displayName = "other.mmd",
                libraryNames = names,
                writtenText = source,
                libraryText = { source },
            ),
        )
    }

    @Test
    fun pathMatchWinsEvenWhenTheNameIsNotListedYet() {
        assertEquals(
            "fresh.mmd",
            LibraryMembership.nameToIndex(
                libraryDocumentId = library,
                documentId = "$library/fresh.mmd",
                displayName = "fresh.mmd",
                libraryNames = emptySet(),
                writtenText = "flowchart TD\n  A-->B",
                libraryText = { null },
            ),
        )
    }
}
