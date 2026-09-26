package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.data.DiagramStore
import com.jcompute.mermaidpocket.data.DiagramSync
import com.jcompute.mermaidpocket.data.DiagramSyncPlan
import com.jcompute.mermaidpocket.data.RemoteDiagrams
import com.jcompute.mermaidpocket.data.RemoteFile
import com.jcompute.mermaidpocket.data.SyncFile
import com.jcompute.mermaidpocket.data.SyncOp
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagramSyncTest {
    private lateinit var root: File
    private lateinit var store: DiagramStore
    private lateinit var remote: MemoryRemote
    private var attached = true

    @Before
    fun setUp() {
        root = kotlin.io.path.createTempDirectory("mermaid-sync").toFile()
        store = DiagramStore(root)
        remote = MemoryRemote()
        attached = true
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun planPushesPrivateFilesAndPullsFolderFilesWithoutDuplicating() {
        val ops = DiagramSyncPlan.plan(
            local = listOf(SyncFile("kept.mmd", 10, "flowchart TD\n  A-->B")),
            remote = listOf(SyncFile("restored.mmd", 20, "flowchart LR\n  C-->D")),
        )
        assertEquals(listOf("kept.mmd", "restored.mmd"), ops.map { it.name })
        assertTrue(ops[0] is SyncOp.Push)
        assertTrue(ops[1] is SyncOp.Pull)
        assertTrue(
            DiagramSyncPlan.plan(
                local = listOf(SyncFile("kept.mmd", 1, "same")),
                remote = listOf(SyncFile("kept.mmd", 9, "same")),
            ).isEmpty(),
        )
    }

    @Test
    fun newerLocalCopyWinsWhenTextDiffers() {
        val ops = DiagramSyncPlan.plan(
            local = listOf(SyncFile("a.mmd", 50, "local")),
            remote = listOf(SyncFile("a.mmd", 10, "remote")),
        )
        assertEquals(listOf(SyncOp.Push("a.mmd")), ops)
        val reverse = DiagramSyncPlan.plan(
            local = listOf(SyncFile("a.mmd", 10, "local")),
            remote = listOf(SyncFile("a.mmd", 50, "remote")),
        )
        assertEquals(listOf(SyncOp.Pull("a.mmd")), reverse)
    }

    @Test
    fun reconcileCopiesBothDirectionsAndDeleteRemovesTheFolderCopy() {
        store.write(store.uniqueFile("local-only"), "flowchart TD\n  L-->M")
        remote.write("remote-only.mmd", "flowchart TD\n  R-->S", modifiedAt = 40)
        val sync = sync()
        val report = sync.reconcile()
        assertEquals(1, report.pushed)
        assertEquals(1, report.pulled)
        assertEquals(setOf("local-only.mmd", "remote-only.mmd"), store.list().map { it.name }.toSet())
        assertEquals(setOf("local-only.mmd", "remote-only.mmd"), remote.files.keys)
        assertEquals("flowchart TD\n  R-->S", store.read(File(root, "diagrams/remote-only.mmd")))

        val again = sync.reconcile()
        assertEquals(0, again.copied)

        val local = store.list().first { it.name == "local-only.mmd" }.file
        sync.delete(local)
        assertFalse(remote.files.containsKey("local-only.mmd"))
        assertEquals(listOf("remote-only.mmd"), store.list().map { it.name })
    }

    @Test
    fun saveMirrorsIntoTheFolder() {
        val sync = sync()
        val written = sync.write(sync.uniqueFile("flow"), "flowchart TD\n  A-->B")
        assertTrue(written.mirrored)
        assertEquals("flowchart TD\n  A-->B", remote.files.getValue("flow.mmd").first)
    }

    @Test
    fun withoutAFolderTheWorkingCopyStaysPrivate() {
        attached = false
        val sync = sync()
        val written = sync.write(sync.uniqueFile("flow"), "flowchart TD\n  A-->B")
        assertFalse(written.mirrored)
        assertFalse(sync.isDurable)
        assertTrue(remote.files.isEmpty())
        assertEquals(0, sync.reconcile().copied)
    }

    @Test
    fun importRejectsPathsOutsideTheLibrary() {
        val failed = runCatching { store.importFile("../escape.mmd", "nope", 1) }
        assertTrue(failed.isFailure)
        assertFalse(File(root, "escape.mmd").exists())
        val dotted = store.importFile("rev..2.mmd", "flowchart TD\n  A-->B", 1)
        assertEquals("rev..2.mmd", dotted.name)
        assertTrue(dotted.isFile)
    }

    @Test
    fun saveACopyInsideTheLibraryIsListedWithoutReconcile() {
        val sync = sync()
        val original = sync.write(sync.uniqueFile("flow"), "flowchart TD\n  A-->B")
        original.file.setLastModified(1_000L)
        val source = "flowchart TD\n  A-->C"
        remote.write("flow-copy.mmd", source, modifiedAt = 2_000L)

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "primary:Documents/MermaidPocket/flow-copy.mmd",
            displayName = "flow-copy.mmd",
            source = source,
        )

        assertTrue(indexed)
        assertEquals(listOf("flow-copy.mmd", "flow.mmd"), sync.list().map { it.name })
        assertEquals(source, sync.read(sync.list().first { it.name == "flow-copy.mmd" }.file))
        assertEquals(setOf("flow.mmd", "flow-copy.mmd"), remote.files.keys)
        assertEquals(2_000L, remote.files.getValue("flow-copy.mmd").second)
        assertEquals(0, sync.reconcile().copied)
    }

    @Test
    fun saveACopyOutsideTheLibraryIsNotListed() {
        val sync = sync()
        sync.write(sync.uniqueFile("flow"), "flowchart TD\n  A-->B")

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "primary:Download/flow-copy.mmd",
            displayName = "flow-copy.mmd",
            source = "flowchart TD\n  A-->C",
        )

        assertFalse(indexed)
        assertEquals(listOf("flow.mmd"), sync.list().map { it.name })
        assertEquals(setOf("flow.mmd"), remote.files.keys)
    }

    @Test
    fun saveACopyMatchesAListedLibraryFileWhenTheDocumentIdIsOpaque() {
        val source = "flowchart TD\n  A-->C"
        remote.write("flow-copy.mmd", source, modifiedAt = 2_000L)
        val sync = sync()

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "msf:100",
            displayName = "flow-copy.mmd",
            source = source,
        )

        assertTrue(indexed)
        assertEquals(listOf("flow-copy.mmd"), sync.list().map { it.name })
        assertEquals(source, remote.read("flow-copy.mmd"))
        assertEquals(2_000L, remote.files.getValue("flow-copy.mmd").second)
    }

    @Test
    fun opaqueSaveDoesNotReplaceADifferentLibraryFile() {
        remote.write("flow.mmd", "original", modifiedAt = 2_000L)
        val sync = sync()
        sync.reconcile()

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "msf:100",
            displayName = "flow.mmd",
            source = "exported elsewhere",
        )

        assertFalse(indexed)
        assertEquals("original", remote.read("flow.mmd"))
        assertEquals("original", sync.read(sync.list().single().file))
        assertEquals(2_000L, remote.files.getValue("flow.mmd").second)
    }

    @Test
    fun saveACopyOverALibraryNameUpdatesTheListedFile() {
        val sync = sync()
        sync.write(sync.uniqueFile("flow"), "old")
        remote.write("flow.mmd", "stale-on-disk", modifiedAt = 2_000L)

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "primary:Documents/MermaidPocket/flow.mmd",
            displayName = "flow.mmd",
            source = "exported",
        )

        assertTrue(indexed)
        assertEquals(listOf("flow.mmd"), sync.list().map { it.name })
        assertEquals("exported", sync.read(sync.list().single().file))
        assertEquals("exported", remote.read("flow.mmd"))
    }

    @Test
    fun pathMatchIndexesANewLibraryNameWithoutCreatingASecondRemoteFile() {
        val sync = sync()
        val source = "flowchart TD\n  A-->B"

        val indexed = sync.upsertSavedCopy(
            libraryDocumentId = "primary:Documents/MermaidPocket",
            documentId = "primary:Documents/MermaidPocket/copy.mmd",
            displayName = "copy.mmd",
            source = source,
        )

        assertTrue(indexed)
        assertEquals(listOf("copy.mmd"), sync.list().map { it.name })
        assertEquals(source, sync.read(sync.list().single().file))
        assertTrue(remote.files.isEmpty())
    }

    @Test
    fun reconcilePullsSevenSiblingFilesIncludingOddNames() {
        val paths = listOf(
            "one.mmd",
            "two.mmd",
            "three.mmd",
            "four.mmd",
            "five.mmd",
            "six.mmd",
            "rev..2.mmd",
        )
        paths.forEachIndexed { index, name ->
            remote.write(name, "%% $name\nflowchart TD\n  A-->B", modifiedAt = (index + 1) * 10L)
        }
        val sync = sync()
        val report = sync.reconcile()
        assertEquals(7, report.pulled)
        assertEquals(0, report.pushed)
        assertEquals(paths.toSet(), sync.list().map { it.name }.toSet())
        assertEquals(7, sync.list().map { it.file.canonicalPath }.distinct().size)
        assertEquals(7, sync.list().map { it.title }.distinct().size)
    }

    @Test
    fun linkOpenedTxtKeepsTxtAndSaveOverwritesIt() {
        val sync = sync()
        val source = "%% External\nflowchart TD\n  A-->B"
        val linked = sync.linkOpened("foo.txt", source)
        assertEquals("foo.txt", linked.file.name)
        assertEquals(source, sync.read(linked.file))
        assertEquals(setOf("foo.txt"), remote.files.keys)
        assertEquals(listOf("foo.txt"), sync.list().map { it.name })

        val again = sync.write(linked.file, "%% External\nflowchart TD\n  A-->C")
        assertEquals(linked.file.canonicalPath, again.file.canonicalPath)
        assertEquals("foo.txt", again.file.name)
        assertEquals("%% External\nflowchart TD\n  A-->C", remote.files.getValue("foo.txt").first)
        assertEquals(1, sync.list().size)
        assertFalse(remote.files.containsKey("foo.txt.mmd"))
        assertFalse(remote.files.containsKey("foo.mmd"))
    }

    @Test
    fun planSyncsTxtSiblingsTheSameAsMmd() {
        val ops = DiagramSyncPlan.plan(
            local = listOf(SyncFile("kept.txt", 10, "flowchart TD\n  A-->B")),
            remote = listOf(SyncFile("restored.txt", 20, "flowchart LR\n  C-->D")),
        )
        assertEquals(listOf("kept.txt", "restored.txt"), ops.map { it.name })
    }

    @Test
    fun linkOpenedCopiesAMissingFileAndSaveOverwritesIt() {
        val sync = sync()
        val source = "%% External\nflowchart TD\n  A-->B"
        val linked = sync.linkOpened("external.mmd", source)
        assertEquals("external.mmd", linked.file.name)
        assertEquals(source, sync.read(linked.file))
        assertEquals(setOf("external.mmd"), remote.files.keys)
        assertEquals(listOf("external.mmd"), sync.list().map { it.name })

        val again = sync.write(linked.file, "%% External\nflowchart TD\n  A-->C")
        assertEquals(linked.file.canonicalPath, again.file.canonicalPath)
        assertEquals("%% External\nflowchart TD\n  A-->C", remote.files.getValue("external.mmd").first)
        assertEquals(1, sync.list().size)
    }

    @Test
    fun reconcilePullsEveryRemoteFileWhenTitlesMatch() {
        val shared = "%% Quarterly\nflowchart TD\n  A-->B"
        listOf("q1.mmd", "q2.mmd", "q3.mmd", "q4.mmd").forEachIndexed { index, name ->
            remote.write(name, shared, modifiedAt = (index + 1) * 10L)
        }
        val sync = sync()
        val report = sync.reconcile()
        assertEquals(4, report.pulled)
        assertEquals(0, report.pushed)
        val listed = sync.list()
        assertEquals(4, listed.size)
        assertEquals(setOf("q1.mmd", "q2.mmd", "q3.mmd", "q4.mmd"), listed.map { it.name }.toSet())
        assertTrue(listed.all { it.title == "Quarterly" })
        assertEquals(4, listed.map { it.file.canonicalPath }.distinct().size)
        assertEquals(shared, sync.read(listed.first { it.name == "q1.mmd" }.file))
        assertEquals(shared, sync.read(listed.first { it.name == "q4.mmd" }.file))
    }

    @Test
    fun readmeNamesTheDurablePath() {
        val readme = source("README.md")
        assertTrue(readme.contains("Documents/MermaidPocket"))
        assertTrue(readme.contains("takePersistableUriPermission") || source("app/src/main/java/com/jcompute/mermaidpocket/data/DurableDocuments.kt").contains("takePersistableUriPermission"))
        val durable = source("app/src/main/java/com/jcompute/mermaidpocket/data/DurableDocuments.kt")
        assertTrue(durable.contains("Documents/MermaidPocket"))
        assertTrue(durable.contains("takePersistableUriPermission"))
        assertTrue(durable.contains("ACTION_OPEN_DOCUMENT_TREE") || durable.contains("OpenDocumentTree") || source("app/src/main/java/com/jcompute/mermaidpocket/ui/PersistableTreeContract.kt").contains("OpenDocumentTree"))
    }

    private fun sync() = DiagramSync(store) { if (attached) remote else null }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }

    private class MemoryRemote : RemoteDiagrams {
        val files = linkedMapOf<String, Pair<String, Long>>()

        override fun list(): List<RemoteFile> = files.map { RemoteFile(it.key, it.value.second) }

        override fun read(name: String): String = files.getValue(name).first

        override fun write(name: String, text: String, modifiedAt: Long) {
            files[name] = text to modifiedAt
        }

        override fun delete(name: String) {
            files.remove(name)
        }
    }
}
