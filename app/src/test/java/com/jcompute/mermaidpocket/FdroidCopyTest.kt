package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FdroidCopyTest {
    @Test
    fun fastlaneCopyMatchesTheStoreText() {
        val summary = "Private Mermaid editor. Offline by default; optional AI needs your keys."
        assertEquals("MermaidPocket", read("fastlane/metadata/android/en-US/title.txt").trim())
        assertEquals(summary, read("fastlane/metadata/android/en-US/short_description.txt").trim())
        assertEquals(
            "Edit and view Mermaid diagram syntax on-device. Paste from chat, pinch-zoom the diagram, save .mmd locally. Editing is offline. Optional voice-to-diagram AI uses your own OpenRouter key (and optional Brave Search key). No ads, no tracking, no required accounts.",
            read("fastlane/metadata/android/en-US/full_description.txt").trim(),
        )
        val readme = read("README.md")
        assertTrue(readme.contains("Summary:\n\n$summary"))
        assertTrue(readme.contains("Editing is offline. Optional voice-to-diagram AI uses your own OpenRouter key"))
    }

    private fun read(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
