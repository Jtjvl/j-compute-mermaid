package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.DiagramStore
import com.jcompute.mermaidpocket.data.DiagramSync
import com.jcompute.mermaidpocket.ui.EditSaveChrome
import com.jcompute.mermaidpocket.ui.EditorLink
import com.jcompute.mermaidpocket.ui.EditorMode
import com.jcompute.mermaidpocket.ui.LibrarySave
import com.jcompute.mermaidpocket.ui.Screen
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditSaveTest {
    private lateinit var root: File
    private lateinit var store: DiagramStore
    private lateinit var sync: DiagramSync

    @Before
    fun setUp() {
        root = kotlin.io.path.createTempDirectory("mermaid-edit-save").toFile()
        store = DiagramStore(root)
        sync = DiagramSync(store) { null }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun saveFabStaysVisibleOnABlankEditAndAnnouncesSaveAs() {
        assertTrue(EditSaveChrome.visible(Screen.Document, EditorMode.Edit))
        val blank = EditSaveChrome.of(linked = false)
        assertEquals("Save as", blank.contentDescription)
        assertTrue(blank.createsFile)
    }

    @Test
    fun linkedSaveAnnouncesSaveAndOverwrites() {
        val linked = EditSaveChrome.of(linked = true)
        assertEquals("Save", linked.contentDescription)
        assertFalse(linked.createsFile)
    }

    @Test
    fun saveFabIsHiddenOutsideEdit() {
        assertFalse(EditSaveChrome.visible(Screen.Document, EditorMode.View))
        assertFalse(EditSaveChrome.visible(Screen.Files, EditorMode.Edit))
        assertFalse(EditSaveChrome.visible(Screen.Files, EditorMode.View))
    }

    @Test
    fun blankSaveCreatesAFileAndDoesNotTouchAnExistingDiagram() {
        val existing = sync.write(sync.uniqueFile("keep"), "flowchart TD\n  A-->B")
        val written = LibrarySave.commit(
            sync = sync,
            currentFile = null,
            title = "Untitled",
            source = "flowchart TD\n  C-->D",
        )
        assertNotEquals(existing.file.canonicalPath, written.file.canonicalPath)
        assertEquals("flowchart TD\n  A-->B", existing.file.readText())
        assertEquals("flowchart TD\n  C-->D", written.file.readText())
        assertTrue(written.file.name.endsWith(".mmd"))
        assertEquals(listOf(existing.file.name, written.file.name).sorted(), store.list().map { it.name }.sorted())
    }

    @Test
    fun blankSaveDoesNotAttachToAnExistingFileWithTheSameTitle() {
        val existing = sync.write(sync.uniqueFile("untitled"), "keep me")
        val written = LibrarySave.commit(
            sync = sync,
            currentFile = null,
            title = "Untitled",
            source = "fresh",
        )
        assertEquals("untitled.mmd", existing.file.name)
        assertEquals("untitled-1.mmd", written.file.name)
        assertEquals("keep me", existing.file.readText())
        assertEquals("fresh", written.file.readText())
    }

    @Test
    fun firstBlankSaveLinksAndTheNextTapOverwritesThatFile() {
        val created = LibrarySave.commit(sync, currentFile = null, title = "Untitled", source = "one")
        val again = LibrarySave.commit(sync, currentFile = created.file, title = "Untitled", source = "two")
        assertEquals(created.file.canonicalPath, again.file.canonicalPath)
        assertEquals("two", again.file.readText())
        assertEquals(listOf(created.file.name), store.list().map { it.name })
    }

    @Test
    fun linkedSaveOverwritesOnlyThatFile() {
        val linked = sync.write(sync.uniqueFile("alpha"), "old")
        val other = sync.write(sync.uniqueFile("beta"), "stay")
        val written = LibrarySave.commit(
            sync = sync,
            currentFile = linked.file,
            title = "alpha",
            source = "new",
        )
        assertEquals(linked.file.canonicalPath, written.file.canonicalPath)
        assertEquals("new", linked.file.readText())
        assertEquals("stay", other.file.readText())
        assertEquals(2, store.list().size)
    }

    @Test
    fun editScreenMirrorsCopyImageChromeWithoutASaveLabel() {
        val screen = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorScreen.kt")
        val fab = screen.substringAfter("floatingActionButton = {").substringBefore("snackbarHost")
        assertTrue(fab.contains("EditSaveChrome.visible"))
        assertTrue(fab.contains("EditSaveChrome.of"))
        assertTrue(fab.contains("viewModel::save"))
        assertTrue(fab.contains("Icons.Outlined.Save"))
        assertTrue(fab.contains("Icons.Outlined.ContentCopy"))
        assertTrue(fab.contains("\"Copy image\""))
        assertFalse(fab.contains("Text(\"Save"))
        assertFalse(fab.contains("enabled ="))
        val control = screen.substringAfter("private fun DiagramCornerControl")
        assertTrue(control.contains("FloatingActionButtonDefaults.extendedFabShape"))
        assertTrue(control.contains("FloatingActionButtonDefaults.elevation()"))
        assertTrue(control.contains("FloatingActionButton("))
        assertTrue(control.contains("ExtendedFloatingActionButton("))
        assertTrue(control.contains("if (label == null)"))

        val menu = screen.substringAfter("private fun DocumentTopBar")
            .substringBefore("private fun DocumentBottomBar")
        assertTrue(menu.contains("Text(\"New\")"))
        assertTrue(menu.contains("onNew()"))
        assertEquals(1, Regex("""Text\("Save"\)""").findAll(menu).count())

        val bottom = screen.substringAfter("private fun DocumentBottomBar")
            .substringBefore("private fun DiagramCornerControl")
        assertFalse(bottom.contains("New"))
        assertFalse(bottom.contains("onNew"))
        assertFalse(bottom.contains("NoteAdd"))
    }

    @Test
    fun blankIsMenuOnlyAndNewDiagramIsUnlinked() {
        val files = source("app/src/main/java/com/jcompute/mermaidpocket/ui/FilesPane.kt")
        val filesMenu = files.substringAfter("fun FilesTopBar").substringBefore("fun BulkDeleteDialog")
        assertTrue(filesMenu.contains("PocketOverflowMenu"))
        assertTrue(filesMenu.contains("onNew = onNew"))
        assertTrue(filesMenu.contains("showDocumentActions = false"))
        val selecting = filesMenu.substringAfter("actions = {").substringAfter("if (selection.active)").substringBefore("} else")
        assertTrue(selecting.contains("Delete selected"))
        assertFalse(selecting.contains("PocketOverflowMenu"))
        val pane = files.substringAfter("fun FilesPane")
        assertFalse(pane.contains("Text(\"New\")"))
        assertFalse(pane.contains("onNew"))

        val model = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorViewModel.kt")
        val blank = model.substringAfter("fun newDiagram()").substringBefore("fun ingest")
        assertTrue(blank.contains("currentFile = null"))
        assertTrue(blank.contains("source = \"\""))
        assertTrue(blank.contains("mode = EditorMode.Edit"))
        val save = model.substringAfter("fun save()").substringBefore("fun dismissFolderPrompt")
        assertTrue(save.contains("LibrarySave.commit"))
        assertTrue(save.contains("currentFile = snapshot.currentFile"))
        assertTrue(save.contains("currentFile = result.file"))
    }

    @Test
    fun openStoredThenOpenImportedWithTheSameTitleSavesADifferentFile() {
        val original = "%% Shared\nflowchart TD\n  A-->B"
        val incoming = "%% Shared\nflowchart TD\n  C-->D"
        val existing = sync.write(sync.uniqueFile("alpha"), original).file
        val before = existing.readBytes()
        val stored = EditorLink.stored(existing, sync.read(existing))
        val imported = EditorLink.imported(incoming, "beta.mmd")

        assertEquals(existing.canonicalPath, stored.currentFile!!.canonicalPath)
        assertFalse(stored.isDirty)
        assertEquals("Shared", stored.title)
        assertEquals(stored.title, imported.title)
        assertNull(imported.currentFile)
        assertTrue(imported.isDirty)
        assertEquals("", imported.savedSource)

        val written = LibrarySave.commit(
            sync = sync,
            currentFile = imported.currentFile,
            title = imported.title,
            source = imported.source,
        )
        assertNotEquals(existing.canonicalPath, written.file.canonicalPath)
        assertTrue(before.contentEquals(existing.readBytes()))
        assertEquals(imported.source, written.file.readText())
        assertEquals(original, existing.readText())
        assertEquals(
            listOf(existing.name, written.file.name).sorted(),
            store.list().map { it.name }.sorted(),
        )
    }

    @Test
    fun ingestAndOpenImportedUnlinkThePreviousFile() {
        val model = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorViewModel.kt")
        val ingest = model.substringAfter("fun ingest(").substringBefore("fun pasteFromClipboard")
        assertTrue(ingest.contains("EditorLink.imported"))
        assertTrue(ingest.contains("currentFile = link.currentFile"))
        assertTrue(ingest.contains("savedSource = link.savedSource"))
        val openImported = model.substringAfter("fun openImported").substringBefore("fun enterSelection")
        assertTrue(openImported.contains("ingest("))
        val intent = model.substringAfter("fun handleIntent").substringBefore("fun openStored")
        assertTrue(intent.contains("ingest("))
        val openStored = model.substringAfter("fun openStored").substringBefore("fun openImported")
        assertTrue(openStored.contains("EditorLink.stored"))
        assertTrue(openStored.contains("currentFile = link.currentFile"))
        assertFalse(openStored.contains("currentFile = null"))
    }

    @Test
    fun filesOverflowMatchesEditAndViewAndRowsKeepFileNames() {
        val screen = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorScreen.kt")
        val overflow = screen.substringAfter("fun PocketOverflowMenu").substringBefore("private fun DocumentBottomBar")
        assertTrue(overflow.contains("Text(\"New\")"))
        assertTrue(overflow.contains("onNew()"))
        assertTrue(overflow.contains("Text(\"Open…\")"))
        assertTrue(overflow.contains("Text(\"Diagram folder…\")"))
        assertTrue(overflow.contains("stringResource(R.string.about)"))
        assertTrue(overflow.contains("Diagram:"))
        assertTrue(overflow.contains("Chrome:"))
        assertTrue(overflow.contains("showDocumentActions"))
        assertEquals(1, Regex("""Text\("Save"\)""").findAll(overflow).count())
        assertTrue(screen.contains("showDocumentActions = true"))
        assertTrue(screen.contains("onOpen = ::launchOpen"))

        val files = source("app/src/main/java/com/jcompute/mermaidpocket/ui/FilesPane.kt")
        assertTrue(files.contains("key = { it.file.absolutePath }"))
        assertFalse(files.contains("distinctBy"))
        val row = files.substringAfter("private fun FileRow").substringBefore("private fun DiagramThumbnail")
        val headline = row.substringAfter("headlineContent").substringBefore("supportingContent")
        val supporting = row.substringAfter("supportingContent").substringBefore("leadingContent")
        assertTrue(headline.contains("diagram.title"))
        assertFalse(headline.contains("diagram.name"))
        assertTrue(supporting.contains("diagram.name"))
    }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}
