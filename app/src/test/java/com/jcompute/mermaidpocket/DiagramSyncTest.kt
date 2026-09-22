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
