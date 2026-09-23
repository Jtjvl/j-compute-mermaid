package com.jcompute.mermaidpocket.data

/**
 * One `.mmd` side of a local-vs-folder compare.
 * [modifiedAt] is the file timestamp. Equal text is left alone even when timestamps differ.
 */
data class SyncFile(
    val name: String,
    val modifiedAt: Long,
    val text: String,
)

sealed interface SyncOp {
    val name: String

    data class Push(override val name: String) : SyncOp
    data class Pull(override val name: String) : SyncOp
}

data class SyncReport(val pushed: Int, val pulled: Int) {
    val copied: Int get() = pushed + pulled
}

/**
 * Decide which copies to move. This does not delete: a local delete has to
 * remove the folder copy itself, or the next pass would treat it as a restore.
 */
object DiagramSyncPlan {
    fun plan(local: List<SyncFile>, remote: List<SyncFile>): List<SyncOp> {
        val localBy = local.filter { it.name.endsWith(".mmd", ignoreCase = true) }.associateBy { it.name }
        val remoteBy = remote.filter { it.name.endsWith(".mmd", ignoreCase = true) }.associateBy { it.name }
        return (localBy.keys + remoteBy.keys).sorted().mapNotNull { name ->
            val left = localBy[name]
            val right = remoteBy[name]
            when {
                left == null && right != null -> SyncOp.Pull(name)
                right == null && left != null -> SyncOp.Push(name)
                left != null && right != null && left.text == right.text -> null
                left != null && right != null && left.modifiedAt >= right.modifiedAt -> SyncOp.Push(name)
                else -> SyncOp.Pull(name)
            }
        }
    }
}

data class RemoteFile(val name: String, val modifiedAt: Long)

/** Folder of `.mmd` files outside app-private storage. */
interface RemoteDiagrams {
    fun list(): List<RemoteFile>
    fun read(name: String): String
    fun write(name: String, text: String, modifiedAt: Long)
    fun delete(name: String)
}

data class LocalWrite(val file: java.io.File, val mirrored: Boolean)

/**
 * Working copies stay in [DiagramStore] so the editor can use files.
 * When a durable folder is attached, writes and deletes are mirrored there,
 * and [reconcile] copies anything that exists on only one side.
 */
class DiagramSync(
    private val local: DiagramStore,
    private val remoteProvider: () -> RemoteDiagrams?,
) {
    val isDurable: Boolean get() = remoteProvider() != null

    fun list() = local.list()

    fun read(file: java.io.File) = local.read(file)

    fun thumbnailFile(diagram: java.io.File) = local.thumbnailFile(diagram)

    fun writeThumbnail(diagram: java.io.File, png: ByteArray) = local.writeThumbnail(diagram, png)

    fun uniqueFile(slug: String) = local.uniqueFile(slug)

    fun write(file: java.io.File, source: String): LocalWrite {
        val written = local.write(file, source)
        val remote = remoteProvider() ?: return LocalWrite(written, mirrored = false)
        return try {
            remote.write(written.name, source, written.lastModified())
            LocalWrite(written, mirrored = true)
        } catch (_: Exception) {
            LocalWrite(written, mirrored = false)
        }
    }

    fun create(title: String, source: String): LocalWrite {
        return write(local.uniqueFile(TitleParser.fileSlug(title)), source)
    }

    /**
     * Index an `.mmd` that Save-a-copy just wrote.
     * Returns true when that destination is a library file and the working copy was
     * created or updated, so [list] includes it. Other folders return false.
     * A listed library file is updated in place when its text is not the export yet.
     */
    fun upsertSavedCopy(
        libraryDocumentId: String?,
        documentId: String?,
        displayName: String?,
        source: String,
    ): Boolean {
        val remote = remoteProvider()
        val listed = remote?.list().orEmpty()
        val libraryNames = listed.map { it.name }.toSet()
        val name = LibraryMembership.nameToIndex(
            libraryDocumentId = libraryDocumentId,
            documentId = documentId,
            displayName = displayName,
            libraryNames = libraryNames,
            writtenText = source,
            libraryText = { candidate ->
                if (remote == null || candidate !in libraryNames) {
                    null
                } else {
                    runCatching { remote.read(candidate) }.getOrNull()
                }
            },
        ) ?: return false
        val modifiedAt = listed.firstOrNull { it.name == name }?.modifiedAt?.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        runCatching { local.importFile(name, source, modifiedAt) }.getOrElse { return false }
        if (remote != null && name in libraryNames) {
            val onDisk = runCatching { remote.read(name) }.getOrNull()
            if (onDisk != null && onDisk != source) {
                runCatching { remote.write(name, source, System.currentTimeMillis()) }
            }
        }
        return true
    }

    fun delete(file: java.io.File) {
        val name = file.name
        val owned = local.owns(file)
        local.delete(file)
        if (!owned) return
        val remote = remoteProvider() ?: return
        runCatching { remote.delete(name) }
    }

    fun deleteAll(files: Collection<java.io.File>): Int {
        val names = files.filter { local.owns(it) && it.exists() }.map { it.name }
        val removed = local.deleteAll(files)
        val remote = remoteProvider() ?: return removed
        names.forEach { name -> runCatching { remote.delete(name) } }
        return removed
    }

    /**
     * Pull and push by file name. Diagrams that share a title stay separate rows
     * when their `.mmd` names differ.
     */
    fun reconcile(): SyncReport {
        val remote = remoteProvider() ?: return SyncReport(0, 0)
        val localFiles = local.list()
        val remoteFiles = remote.list().filter { it.name.endsWith(".mmd", ignoreCase = true) }
        val ops = DiagramSyncPlan.plan(
            local = localFiles.map { SyncFile(it.name, it.updatedAtMillis, local.read(it.file)) },
            remote = remoteFiles.map { SyncFile(it.name, it.modifiedAt, remote.read(it.name)) },
        )
        var pushed = 0
        var pulled = 0
        for (op in ops) {
            when (op) {
                is SyncOp.Push -> {
                    val item = local.list().first { it.name == op.name }
                    remote.write(op.name, local.read(item.file), item.updatedAtMillis)
                    pushed++
                }
                is SyncOp.Pull -> {
                    val modified = remoteFiles.first { it.name == op.name }.modifiedAt
                    local.importFile(op.name, remote.read(op.name), modified)
                    pulled++
                }
            }
        }
        return SyncReport(pushed, pulled)
    }
}
