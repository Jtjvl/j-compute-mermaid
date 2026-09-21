package com.jcompute.mermaid

import com.jcompute.mermaid.ui.FileSelection
import com.jcompute.mermaid.ui.RenderPass
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderPassTest {
    @Test
    fun identicalSourceStillAdvancesEpoch() {
        val source = "flowchart TD\n    A --> B"
        val first = RenderPass.next(0, source)
        val second = RenderPass.next(first.epoch, source)
        assertEquals(1L, first.epoch)
        assertEquals(2L, second.epoch)
        assertTrue(first.showSpinner)
        assertTrue(second.showSpinner)
    }

    @Test
    fun blankSourceDoesNotShowSpinner() {
        val pass = RenderPass.next(4, "   ")
        assertEquals(5L, pass.epoch)
        assertFalse(pass.showSpinner)
    }

    @Test
    fun engineKeysLaunchedEffectOnEpoch() {
        val kt = sourceFile("app/src/main/java/com/jcompute/mermaid/ui/MermaidWebView.kt")
        assertTrue(
            "MermaidEngine must rerun when renderEpoch changes even if source is unchanged",
            kt.contains("LaunchedEffect(source, diagramTheme, renderEpoch)"),
        )
        assertTrue(kt.contains("renderEpoch: Long"))
    }

    @Test
    fun ingestAndOpenBumpEpochInViewModel() {
        val kt = sourceFile("app/src/main/java/com/jcompute/mermaid/ui/EditorViewModel.kt")
        assertTrue(kt.contains("RenderPass.next(it.renderEpoch, source)"))
        assertTrue(kt.contains("renderEpoch = pass.epoch"))
        assertTrue(kt.contains("isRendering = pass.showSpinner"))
    }

    private fun sourceFile(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}

class FileSelectionTest {
    @Test
    fun longPressEntersWithThatFileSelected() {
        val next = FileSelection().enter("/diagrams/a.mmd")
        assertTrue(next.active)
        assertEquals(1, next.count)
        assertTrue(next.contains("/diagrams/a.mmd"))
    }

    @Test
    fun toggleAddsAndRemoves() {
        val one = FileSelection().enter("/a.mmd")
        val two = one.toggle("/b.mmd")
        assertEquals(setOf("/a.mmd", "/b.mmd"), two.paths)
        val oneAgain = two.toggle("/a.mmd")
        assertEquals(setOf("/b.mmd"), oneAgain.paths)
        assertTrue(oneAgain.active)
    }

    @Test
    fun toggleWhileInactiveEntersSelection() {
        val next = FileSelection().toggle("/a.mmd")
        assertTrue(next.active)
        assertTrue(next.contains("/a.mmd"))
    }

    @Test
    fun pruneDropsDeletedPaths() {
        val selected = FileSelection(active = true, paths = setOf("/a.mmd", "/gone.mmd"))
        val pruned = selected.prune(setOf("/a.mmd"))
        assertEquals(setOf("/a.mmd"), pruned.paths)
        assertTrue(pruned.active)
    }

    @Test
    fun clearExitsMultiSelect() {
        val cleared = FileSelection().enter("/a.mmd").clear()
        assertFalse(cleared.active)
        assertTrue(cleared.paths.isEmpty())
    }
}
