package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.render.RendererAttach
import com.jcompute.mermaidpocket.ui.EditorMode
import com.jcompute.mermaidpocket.ui.Screen
import com.jcompute.mermaidpocket.ui.ViewChrome
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RendererAttachTest {
    @Test
    fun newWebViewIsNeverReady() {
        assertFalse(RendererAttach.isReady(sameView = false, previouslyReady = true))
        assertFalse(RendererAttach.isReady(sameView = false, previouslyReady = false))
    }

    @Test
    fun sameWebViewKeepsReadyFlag() {
        assertTrue(RendererAttach.isReady(sameView = true, previouslyReady = true))
        assertFalse(RendererAttach.isReady(sameView = true, previouslyReady = false))
    }

    @Test
    fun controllerResetsReadyOnNewView() {
        val kt = sourceFile("app/src/main/java/com/jcompute/mermaidpocket/render/MermaidRendererController.kt")
        assertTrue(kt.contains("RendererAttach.isReady"))
        assertTrue(kt.contains("fun relayout()"))
    }

    @Test
    fun engineRelayoutsWhenBoundsChange() {
        val kt = sourceFile("app/src/main/java/com/jcompute/mermaidpocket/ui/MermaidWebView.kt")
        assertTrue(kt.contains("addOnLayoutChangeListener"))
        assertTrue(kt.contains("controller.relayout()"))
        assertTrue(kt.contains("onRelease"))
    }

    @Test
    fun activitySurvivesOrientationSoWebViewCanRelayout() {
        val xml = sourceFile("app/src/main/AndroidManifest.xml")
        assertTrue(xml.contains("orientation|screenSize|screenLayout|smallestScreenSize"))
    }

    private fun sourceFile(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}

class ViewChromeTest {
    @Test
    fun firstViewTapFromEditIsNotImmersive() {
        val next = ViewChrome.onMode(
            currentMode = EditorMode.Edit,
            currentScreen = Screen.Document,
            immersive = false,
            requested = EditorMode.View,
        )
        assertEquals(EditorMode.View, next.mode)
        assertEquals(Screen.Document, next.screen)
        assertFalse(next.immersive)
    }

    @Test
    fun secondViewTapEntersImmersive() {
        val next = ViewChrome.onMode(
            currentMode = EditorMode.View,
            currentScreen = Screen.Document,
            immersive = false,
            requested = EditorMode.View,
        )
        assertTrue(next.immersive)
        assertEquals(EditorMode.View, next.mode)
    }

    @Test
    fun viewTapWhileImmersiveExits() {
        val next = ViewChrome.onMode(
            currentMode = EditorMode.View,
            currentScreen = Screen.Document,
            immersive = true,
            requested = EditorMode.View,
        )
        assertFalse(next.immersive)
        assertEquals(EditorMode.View, next.mode)
    }

    @Test
    fun editTapLeavesImmersive() {
        val next = ViewChrome.onMode(
            currentMode = EditorMode.View,
            currentScreen = Screen.Document,
            immersive = true,
            requested = EditorMode.Edit,
        )
        assertEquals(EditorMode.Edit, next.mode)
        assertFalse(next.immersive)
    }

    @Test
    fun viewFromFilesIsNotImmersive() {
        val next = ViewChrome.onMode(
            currentMode = EditorMode.View,
            currentScreen = Screen.Files,
            immersive = false,
            requested = EditorMode.View,
        )
        assertFalse(next.immersive)
        assertEquals(Screen.Document, next.screen)
    }
}
