package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.LibraryListing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryListingTest {
    @Test
    fun sevenFilesWithMixedNestingAllStayVisible() {
        val paths = listOf(
            "one.mmd",
            "two.mmd",
            "three.mmd",
            "nested/four.mmd",
            "nested/five.mmd",
            "deep/down/six.mmd",
            "other/seven.mmd",
        )
        val names = LibraryListing.storeNames(paths)
        assertEquals(7, names.size)
        assertEquals(7, names.values.toSet().size)
        assertEquals("one.mmd", names["one.mmd"])
        assertEquals("two.mmd", names["two.mmd"])
        assertEquals("three.mmd", names["three.mmd"])
        assertEquals("four.mmd", names["nested/four.mmd"])
        assertEquals("five.mmd", names["nested/five.mmd"])
        assertEquals("six.mmd", names["deep/down/six.mmd"])
        assertEquals("seven.mmd", names["other/seven.mmd"])
    }

    @Test
    fun collidingBasenamesKeepTheRootNameAndPrefixTheNestedFile() {
        val names = LibraryListing.storeNames(listOf("flow.mmd", "nested/flow.mmd", "other/flow.mmd"))
        assertEquals("flow.mmd", names["flow.mmd"])
        assertEquals("nested-flow.mmd", names["nested/flow.mmd"])
        assertEquals("other-flow.mmd", names["other/flow.mmd"])
        assertEquals(3, names.values.toSet().size)
    }

    @Test
    fun flattenJoinsFolderSegments() {
        assertEquals("a.mmd", LibraryListing.flatten("a.mmd"))
        assertEquals("nested-flow.mmd", LibraryListing.flatten("nested/flow.mmd"))
        assertEquals("deep-down-six.mmd", LibraryListing.flatten("deep/down/six.mmd"))
    }

    @Test
    fun durableRemoteWalksNestedMmdFiles() {
        val durable = source("app/src/main/java/com/jcompute/mermaidpocket/data/DurableDocuments.kt")
        assertTrue(durable.contains("LibraryListing.storeNames"))
        assertTrue(durable.contains("private fun collect"))
        assertTrue(durable.contains("nestedMmdOutsidePocket"))
        assertTrue(durable.contains("hasMmdOutsidePocket"))
    }

    private fun source(relative: String): String {
        val candidates = listOf(java.io.File(relative), java.io.File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
