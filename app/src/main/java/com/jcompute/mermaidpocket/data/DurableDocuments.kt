package com.jcompute.mermaidpocket.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

/**
 * Persistable SAF tree for the diagram library.
 *
 * App-private `filesDir` is deleted on uninstall. Files written through a
 * folder the user grants are not. The canonical place is [DISPLAY_PATH]:
 * pick `Documents` and this class uses (or creates) the `MermaidPocket` child,
 * or pick that folder directly. A picked folder that already has `.mmd` files
 * at its root — or nested outside a leftover `MermaidPocket` child — is the
 * library itself. [DocumentRemote] lists every `.mmd` under that tree, not
 * only direct children.
 *
 * The grant itself does not survive uninstall. After reinstall, choose the
 * same folder and [DiagramSync.reconcile] copies the `.mmd` files back.
 */
class DurableDocuments(
    context: Context,
    private val settings: AppSettings,
) {
    private val appContext = context.applicationContext

    fun isReady(): Boolean = libraryRoot() != null

    /** Document id of the granted MermaidPocket folder, or null when no folder is attached. */
    fun libraryDocumentId(): String? {
        val root = libraryRoot() ?: return null
        return documentId(root.uri)
    }

    fun documentId(uri: Uri): String? {
        return runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
    }

    fun remote(): RemoteDiagrams? {
        val root = libraryRoot() ?: return null
        return DocumentRemote(appContext, root)
    }

    fun suggestedInitialUri(): Uri {
        settings.diagramTreeUri()?.let { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()?.let { return it }
        }
        return DocumentsContract.buildDocumentUri(
            EXTERNAL_STORAGE_AUTHORITY,
            "primary:${Environment.DIRECTORY_DOCUMENTS}",
        )
    }

    fun attach(treeUri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val resolver = appContext.contentResolver
        resolver.takePersistableUriPermission(treeUri, flags)
        val previous = settings.diagramTreeUri()
        settings.setDiagramTreeUri(treeUri.toString())
        if (!previous.isNullOrBlank() && previous != treeUri.toString()) {
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(previous), flags) }
        }
        check(libraryRoot() != null) { "Could not use that folder." }
    }

    private fun persistedTreeUri(): Uri? {
        val raw = settings.diagramTreeUri() ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val granted = appContext.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == uri && perm.isReadPermission && perm.isWritePermission
        }
        return uri.takeIf { granted }
    }

    private fun libraryRoot(): DocumentFile? {
        val tree = persistedTreeUri() ?: return null
        val picked = DocumentFile.fromTreeUri(appContext, tree) ?: return null
        if (!picked.isDirectory) return null
        val children = picked.listFiles()?.toList().orEmpty()
        val choice = LibraryRoot.choose(
            pickedName = picked.name,
            children = children.mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                LibraryChild(name = name, directory = doc.isDirectory)
            },
            nestedMmdOutsidePocket = hasMmdOutsidePocket(children),
        )
        return when (choice) {
            LibraryRootChoice.Picked -> picked
            LibraryRootChoice.ExistingChild -> children.firstOrNull { doc ->
                doc.isDirectory && doc.name.equals(FOLDER_NAME, ignoreCase = true)
            }
            LibraryRootChoice.CreateChild -> picked.createDirectory(FOLDER_NAME)
        }
    }

    /**
     * True when a subdirectory other than MermaidPocket already holds `.mmd`
     * files. Bounded so granting `Documents` does not walk the whole tree.
     */
    private fun hasMmdOutsidePocket(children: List<DocumentFile>): Boolean {
        var visits = 0
        fun walk(dir: DocumentFile, depth: Int): Boolean {
            if (depth > 6 || visits > 250) return false
            visits++
            for (doc in dir.listFiles()) {
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue
                if (doc.isFile && LibraryListing.isDiagramFileName(name)) return true
                if (doc.isDirectory && walk(doc, depth + 1)) return true
            }
            return false
        }
        return children.any { doc ->
            doc.isDirectory &&
                !doc.name.equals(FOLDER_NAME, ignoreCase = true) &&
                walk(doc, 1)
        }
    }

    fun isLibraryDocument(uri: Uri): Boolean {
        val library = libraryDocumentId() ?: return false
        val document = documentId(uri) ?: return false
        return LibraryMembership.relativePath(library, document) != null
    }

    private class DocumentRemote(
        private val context: Context,
        private val root: DocumentFile,
    ) : RemoteDiagrams {
        override fun list(): List<RemoteFile> {
            return catalog().map { (storeName, found) ->
                RemoteFile(
                    name = storeName,
                    modifiedAt = found.doc.lastModified(),
                    relativePath = found.relativePath,
                )
            }
        }

        override fun read(name: String): String {
            val doc = find(name) ?: error("Missing $name")
            return context.contentResolver.openInputStream(doc.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not read $name")
        }

        @Suppress("UNUSED_PARAMETER")
        override fun write(name: String, text: String, modifiedAt: Long) {
            val existing = find(name)
            val created = existing ?: root.createFile(MIME, name) ?: error("Could not create $name")
            val doc = if (created.name == name) {
                created
            } else {
                val renamed = DocumentsContract.renameDocument(context.contentResolver, created.uri, name)
                if (renamed != null) DocumentFile.fromSingleUri(context, renamed) ?: created else created
            }
            context.contentResolver.openOutputStream(doc.uri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } ?: error("Could not write $name")
            // Document providers stamp their own mtime. Equal text skips a later rewrite.
        }

        override fun delete(name: String) {
            find(name)?.delete()
        }

        private fun find(name: String): DocumentFile? {
            return catalog()[name]?.doc
        }

        private data class Found(val relativePath: String, val doc: DocumentFile)

        /**
         * Every `.mmd` under [root], keyed by the flat working-copy name.
         * Nested files stay in place; only the list identity is flattened.
         */
        private fun catalog(): Map<String, Found> {
            val found = mutableListOf<Found>()
            collect(root, prefix = "", into = found)
            val names = LibraryListing.storeNames(found.map { it.relativePath })
            return found.associate { entry ->
                val storeName = names.getValue(entry.relativePath)
                storeName to entry
            }
        }

        private fun collect(dir: DocumentFile, prefix: String, into: MutableList<Found>) {
            for (doc in dir.listFiles()) {
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue
                if (doc.isDirectory) {
                    val childPrefix = if (prefix.isEmpty()) name else "$prefix/$name"
                    collect(doc, childPrefix, into)
                } else if (doc.isFile && LibraryListing.isDiagramFileName(name)) {
                    val relative = if (prefix.isEmpty()) name else "$prefix/$name"
                    into.add(Found(relative, doc))
                }
            }
        }
    }

    companion object {
        const val FOLDER_NAME = LibraryRoot.FOLDER_NAME
        const val DISPLAY_PATH = "Documents/MermaidPocket"
        private const val MIME = "text/plain"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}
