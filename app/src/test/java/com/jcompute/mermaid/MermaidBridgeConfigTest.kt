package com.jcompute.mermaid

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidBridgeConfigTest {
    private fun bridge(): String {
        val candidates = listOf(
            File("src/main/assets/mermaid/bridge.js"),
            File("app/src/main/assets/mermaid/bridge.js"),
        )
        return candidates.first { it.exists() }.readText()
    }

    @Test
    fun initializeUsesMermaidLivePrettyLookAndElk() {
        val js = bridge()
        assertTrue(js.contains("look: \"neo\""))
        assertTrue(js.contains("layout: \"elk\""))
        assertTrue(js.contains("redux-color"))
        assertTrue(js.contains("redux-dark-color"))
    }

    @Test
    fun initializeDoesNotWashThemeVariablesOrShrinkErPadding() {
        val js = bridge()
        assertFalse(js.contains("themeVariables:"))
        assertFalse(js.contains("entityPadding:"))
        assertFalse(js.contains("theme: \"dark\""))
    }
}
