package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertEquals
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
    fun launcherIsTheSquareAdaptivePack() {
        val icon = read("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        val round = read("app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml")
        assertTrue(icon.contains("@mipmap/ic_launcher_background"))
        assertTrue(icon.contains("@mipmap/ic_launcher_foreground"))
        assertFalse(icon.contains("@drawable/ic_launcher_foreground"))
        assertFalse(icon.contains("#1A1A1A"))
        assertEquals(icon, round)
        assertFalse(icon.contains("Circuit"))
        val manifest = read("design/launcher/v2/manifest.json")
        assertTrue(manifest.contains("v2-adaptive-square"))
        assertTrue(manifest.contains("Muses"))
    }

    private fun read(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
