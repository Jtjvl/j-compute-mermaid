package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandChromeTest {
    @Test
    fun launcherNameSubtitleAndAboutLineAreLocked() {
        val strings = read("app/src/main/res/values/strings.xml")
        assertTrue(strings.contains(">MermaidPocket<"))
        assertFalse(strings.contains(">Mermaid<"))
        assertTrue(strings.contains("Edit and view Mermaid diagram syntax."))
        assertTrue(strings.contains("MermaidPocket by J-compute · GPL · Diagrams rendered with mermaid.js (MIT)"))
        assertTrue(strings.contains("Knut Sveidqvist"))
        assertTrue(strings.contains("GPL-3.0-or-later"))
    }

    @Test
    fun launcherIsNotADiagramOrCircuitMark() {
        val icon = read("app/src/main/res/drawable/ic_launcher_foreground.xml")
        assertFalse(icon.contains("M30,28h48"))
        assertFalse(icon.contains("Circuit"))
        assertTrue(icon.contains("M34,46h40"))
    }

    private fun read(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
