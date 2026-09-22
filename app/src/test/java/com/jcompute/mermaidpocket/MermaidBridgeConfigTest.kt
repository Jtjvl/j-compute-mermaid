package com.jcompute.mermaidpocket

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

    @Test
    fun identicalSourceReplaysLastSvgAndNotifies() {
        val js = bridge()
        assertTrue(js.contains("lastOkSource"))
        assertTrue(js.contains("lastOkSvg"))
        assertTrue(js.contains("function presentSuccess"))
        assertTrue(js.contains("text === lastOkSource && resolved === lastOkTheme && lastOkSvg"))
    }

    @Test
    fun rasterExportIsSeparateFromClipboardPngScale() {
        val js = bridge()
        assertTrue(js.contains("window.exportPng"))
        assertTrue(js.contains("window.exportSvg"))
        assertTrue(js.contains("window.exportRaster"))
        assertTrue(js.contains("const EXPORT_MIN = 256"))
        assertTrue(js.contains("const EXPORT_MAX = 8192"))
        val png = js.substringAfter("window.exportPng = function").substringBefore("window.exportRaster")
        assertTrue(png.contains("svgToPngDataUrl"))
        assertFalse(png.contains("fitExportSize"))
    }

    @Test
    fun relayoutRefitsWithoutResettingZoom() {
        val js = bridge()
        assertTrue(js.contains("window.relayoutView"))
        val relayout = js.substringAfter("window.relayoutView = function").substringBefore("window.addEventListener")
        assertTrue(relayout.contains("applyViewTransform()"))
        assertFalse(relayout.contains("resetViewTransform"))
        assertTrue(js.contains("stageEl.clientHeight"))
    }
}
