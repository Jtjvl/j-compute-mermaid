package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.DiagramTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagramThemeTest {
    @Test
    fun defaultIsMermaidLivePrettyReduxColor() {
        assertEquals("redux-color", DiagramTheme.Default.mermaidId)
        assertEquals("#ffffff", DiagramTheme.Default.canvasHex)
        assertEquals(false, DiagramTheme.Default.isDarkCanvas)
    }

    @Test
    fun darkIsMermaidLiveReduxDarkColor() {
        assertEquals("redux-dark-color", DiagramTheme.Dark.mermaidId)
        assertEquals("#111113", DiagramTheme.Dark.canvasHex)
        assertEquals(true, DiagramTheme.Dark.isDarkCanvas)
    }

    @Test
    fun unknownStoredValueFallsBackToDefault() {
        assertEquals(DiagramTheme.Default, DiagramTheme.fromStored(null))
        assertEquals(DiagramTheme.Default, DiagramTheme.fromStored(""))
        assertEquals(DiagramTheme.Default, DiagramTheme.fromStored("forest"))
    }

    @Test
    fun storedNameRoundTrips() {
        assertEquals(DiagramTheme.Dark, DiagramTheme.fromStored("Dark"))
        assertEquals(DiagramTheme.Dark, DiagramTheme.fromStored("dark"))
        assertEquals(DiagramTheme.Default, DiagramTheme.fromStored("Default"))
    }

    @Test
    fun pickerCyclesDefaultAndDark() {
        assertEquals(DiagramTheme.Dark, DiagramTheme.Default.next())
        assertEquals(DiagramTheme.Default, DiagramTheme.Dark.next())
    }
}
