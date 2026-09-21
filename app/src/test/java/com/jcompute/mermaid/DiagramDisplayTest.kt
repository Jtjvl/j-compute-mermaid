package com.jcompute.mermaid

import com.jcompute.mermaid.ui.DiagramDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagramDisplayTest {
    @Test
    fun zoomIsClampedToHalfAndFive() {
        assertEquals(0.5f, DiagramDisplay.clampZoom(0.1f))
        assertEquals(5f, DiagramDisplay.clampZoom(12f))
        assertEquals(1.25f, DiagramDisplay.clampZoom(1.25f), 0.0001f)
    }

    @Test
    fun pinchMultipliesThenClamps() {
        assertEquals(2f, DiagramDisplay.applyPinch(1f, 2f), 0.0001f)
        assertEquals(DiagramDisplay.MIN_ZOOM, DiagramDisplay.applyPinch(0.5f, 0.2f), 0.0001f)
        assertEquals(DiagramDisplay.MAX_ZOOM, DiagramDisplay.applyPinch(4f, 2f), 0.0001f)
    }

    @Test
    fun pngScaleNeverOneTimes() {
        assertEquals(2, DiagramDisplay.pngScale(0.75f))
        assertEquals(2, DiagramDisplay.pngScale(1f))
        assertEquals(2, DiagramDisplay.pngScale(1.5f))
        assertEquals(2, DiagramDisplay.pngScale(2f))
    }

    @Test
    fun pngScalePrefersThreeOnXxhdpi() {
        assertEquals(3, DiagramDisplay.pngScale(2.75f))
        assertEquals(3, DiagramDisplay.pngScale(3f))
        assertEquals(3, DiagramDisplay.pngScale(4f))
    }

    @Test
    fun fitIsIdentity() {
        assertEquals(1f, DiagramDisplay.FIT_ZOOM)
        assertTrue(DiagramDisplay.fitOffset().x == 0f && DiagramDisplay.fitOffset().y == 0f)
    }
}
