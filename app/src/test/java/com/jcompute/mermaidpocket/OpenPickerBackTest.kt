package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPickerBackTest {
    @Test
    fun safBrowseStaysSystemPickerAndInAppOpenReturnsToApp() {
        val contract = source("app/src/main/java/com/jcompute/mermaidpocket/ui/OpenDiagramContract.kt")
        assertTrue(contract.contains("ActivityResultContracts.OpenDocument"))
        assertTrue(contract.contains("LibraryOpenDialog"))
        assertTrue(contract.contains("platform limitation"))
        assertTrue(contract.contains("Do not replace it with a custom file browser"))
        assertFalse(contract.contains("ACTION_GET_CONTENT"))

        val dialog = source("app/src/main/java/com/jcompute/mermaidpocket/ui/LibraryOpenDialog.kt")
        assertTrue(dialog.contains("returns to the app"))
        assertTrue(dialog.contains("platform limitation"))
        assertTrue(dialog.contains("do not invent a custom file browser"))

        val screen = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorScreen.kt")
        assertTrue(screen.contains("LibraryOpenDialog"))
        assertTrue(screen.contains("launchSafOpen"))
        assertTrue(screen.contains("OpenDiagramContract"))
        assertFalse(screen.contains("ACTION_GET_CONTENT"))
        assertFalse(File("app/src/main/java/com/jcompute/mermaidpocket/ui/FileBrowser.kt").exists())
        assertFalse(File("../app/src/main/java/com/jcompute/mermaidpocket/ui/FileBrowser.kt").exists())
    }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
