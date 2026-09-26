package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.LibraryChild
import com.jcompute.mermaidpocket.data.LibraryRoot
import com.jcompute.mermaidpocket.data.LibraryRootChoice
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRootTest {
    @Test
    fun pickedFolderWithMmdFilesIsTheLibraryEvenWhenAPocketChildExists() {
        val children = listOf(
            LibraryChild("one.mmd", directory = false),
            LibraryChild("two.mmd", directory = false),
            LibraryChild("three.mmd", directory = false),
            LibraryChild("four.mmd", directory = false),
            LibraryChild("MermaidPocket", directory = true),
        )
        assertEquals(LibraryRootChoice.Picked, LibraryRoot.choose("Notes", children))
    }

    @Test
    fun documentsWithoutMmdUsesTheMermaidPocketChild() {
        assertEquals(
            LibraryRootChoice.ExistingChild,
            LibraryRoot.choose(
                "Documents",
                listOf(LibraryChild("MermaidPocket", directory = true), LibraryChild("notes.txt", directory = false)),
            ),
        )
        assertEquals(LibraryRootChoice.CreateChild, LibraryRoot.choose("Documents", emptyList()))
    }

    @Test
    fun folderNamedMermaidPocketIsUsedDirectly() {
        assertEquals(LibraryRootChoice.Picked, LibraryRoot.choose("mermaidpocket", emptyList()))
        assertEquals(
            LibraryRootChoice.Picked,
            LibraryRoot.choose("MermaidPocket", listOf(LibraryChild("a.mmd", directory = false))),
        )
    }

    @Test
    fun mmdMatchIsCaseInsensitiveAndDirectoriesDoNotCount() {
        assertEquals(
            LibraryRootChoice.Picked,
            LibraryRoot.choose("Notes", listOf(LibraryChild("Diagram.MMD", directory = false))),
        )
        assertEquals(
            LibraryRootChoice.Picked,
            LibraryRoot.choose("Notes", listOf(LibraryChild("flow.mermaid", directory = false))),
        )
        assertEquals(
            LibraryRootChoice.CreateChild,
            LibraryRoot.choose("Notes", listOf(LibraryChild("not-a-file.mmd", directory = true))),
        )
    }

    @Test
    fun sevenPeerMmdFilesMakeThePickedFolderTheLibrary() {
        val children = listOf(
            LibraryChild("one.mmd", directory = false),
            LibraryChild("two.mmd", directory = false),
            LibraryChild("three.mmd", directory = false),
            LibraryChild("four.mmd", directory = false),
            LibraryChild("five.mmd", directory = false),
            LibraryChild("six.mmd", directory = false),
            LibraryChild("seven.mmd", directory = false),
            LibraryChild("MermaidPocket", directory = true),
        )
        assertEquals(LibraryRootChoice.Picked, LibraryRoot.choose("Notes", children))
    }

    @Test
    fun durableDocumentsFollowsThePlacementChoice() {
        val src = source("app/src/main/java/com/jcompute/mermaidpocket/data/DurableDocuments.kt")
        assertTrue(src.contains("LibraryRoot.choose"))
        assertTrue(src.contains("LibraryRootChoice.Picked -> picked"))
        assertTrue(src.contains("LibraryRootChoice.ExistingChild"))
        assertTrue(src.contains("LibraryRootChoice.CreateChild -> picked.createDirectory"))
    }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
