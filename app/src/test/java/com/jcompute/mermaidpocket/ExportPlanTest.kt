package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.export.ExportFormat
import com.jcompute.mermaidpocket.export.ExportNames
import com.jcompute.mermaidpocket.export.ExportSize
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPlanTest {
    @Test
    fun defaultWidthIs1920AndOutOfRangeValuesClamp() {
        assertEquals(1920, ExportSize.DEFAULT_WIDTH)
        assertEquals(1920, ExportSize.parseWidth(""))
        assertEquals(1920, ExportSize.parseWidth("   "))
        assertNull(ExportSize.parseWidth("wide"))
        assertEquals(256, ExportSize.parseWidth("100"))
        assertEquals(8192, ExportSize.parseWidth("90000"))
        assertEquals(1920, ExportSize.clampWidth(1920))
    }

    @Test
    fun heightFollowsAspectAndTallDiagramsStayInsideTheCap() {
        assertEquals(1920 to 1080, ExportSize.outputSize(1920, 1920, 1080))
        assertEquals(819 to 8192, ExportSize.outputSize(1920, 100, 1000))
        val square = ExportSize.outputSize(1920, 0, 0)
        assertEquals(1920, square.first)
        assertEquals(1920, square.second)
    }

    @Test
    fun estimateGrowsWithWidthAndJpegIsSmallerThanPng() {
        val (pngW, pngH) = ExportSize.outputSize(1920, 1600, 900)
        val png = ExportSize.estimateBytes(pngW, pngH, ExportFormat.Png)!!
        val jpeg = ExportSize.estimateBytes(pngW, pngH, ExportFormat.Jpeg)!!
        val (smallW, smallH) = ExportSize.outputSize(256, 1600, 900)
        val small = ExportSize.estimateBytes(smallW, smallH, ExportFormat.Png)!!
        assertTrue(jpeg < png)
        assertTrue(small < png)
        assertEquals("About 810 KB", ExportSize.approxLabel(png))
        assertNull(ExportSize.estimateBytes(pngW, pngH, ExportFormat.Svg))
        assertNull(ExportSize.estimateBytes(pngW, pngH, ExportFormat.Mmd))
    }

    @Test
    fun menuOffersPngSvgJpegAndMmdBesideClipboardCopy() {
        assertEquals(
            listOf(ExportFormat.Png, ExportFormat.Jpeg, ExportFormat.Svg, ExportFormat.Mmd),
            ExportFormat.entries,
        )
        val menu = sourceFile("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorScreen.kt")
        for (label in listOf(
            "Share PNG",
            "Save PNG…",
            "Share SVG",
            "Save SVG…",
            "Share JPEG",
            "Save JPEG…",
            "Share .mmd",
            "Save .mmd as…",
            "Copy image",
        )) {
            assertTrue(menu.contains(label))
        }
        assertTrue(menu.contains("onCopyImage"))
        assertFalse(menu.contains("WebP"))
        assertFalse(menu.contains("image/webp"))
    }

    private fun sourceFile(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }

    @Test
    fun fileNamesUseTheFormatExtension() {
        assertEquals("Foo_Bar.png", ExportNames.fileName("Foo Bar.mmd", ExportFormat.Png))
        assertEquals("Foo_Bar.jpg", ExportNames.fileName("Foo Bar.mmd", ExportFormat.Jpeg))
        assertEquals("Foo_Bar.svg", ExportNames.fileName("Foo Bar.mmd", ExportFormat.Svg))
        assertEquals("Foo_Bar.mmd", ExportNames.fileName("Foo Bar.mmd", ExportFormat.Mmd))
        assertEquals("diagram.png", ExportNames.fileName("diagram.png", ExportFormat.Png))
    }
}
