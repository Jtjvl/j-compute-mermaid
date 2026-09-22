package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FdroidCopyTest {
    @Test
    fun fastlaneCopyMatchesTheStoreText() {
        val summary = "Private, offline Mermaid diagram editor for your phone. No ads, no accounts, no network."
        assertEquals("MermaidPocket", read("fastlane/metadata/android/en-US/title.txt").trim())
        assertEquals(summary, read("fastlane/metadata/android/en-US/short_description.txt").trim())
        assertEquals(
            "Edit and view Mermaid diagram syntax on-device. Paste from chat, pinch-zoom the diagram, save .mmd locally. Fully offline. No ads, no tracking.",
            read("fastlane/metadata/android/en-US/full_description.txt").trim(),
        )
        val readme = read("README.md")
        assertTrue(readme.contains("Summary:\n\n$summary"))
        assertTrue(readme.contains("Edit and view Mermaid diagram syntax on-device. Paste from chat, pinch-zoom the diagram, save .mmd locally. Fully offline. No ads, no tracking."))
    }

    private fun read(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
