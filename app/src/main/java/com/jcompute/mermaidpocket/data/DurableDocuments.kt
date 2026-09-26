package com.jcompute.mermaidpocket.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

/**
 * Persistable SAF tree for the diagram library.
 *
 * App-private `filesDir` is deleted on uninstall. Files written through a
 * folder the user grants are not. The canonical place is [DISPLAY_PATH]:
 * pick `Documents` and this class uses (or creates) the `MermaidPocket` child,
 * or pick that folder directly. A picked folder that already has mermaid
 * sources (`.mmd` / `.mermaid`) at its root is the library itself — those
 * files are not skipped in favor of a nested `MermaidPocket` directory.
 *
 * Listing is **direct children only**. Peer text/mermaid files are included
 * by name even when MIME is null ([DocumentFile.isFile] is false then —
 * typical for desktop-copied diagrams next to app-created `text/plain`
 * ones). Extension is not required. The child query paginates;
 * [DocumentFile.listFiles] is the fallback.
 *
 * The grant itself does not survive uninstall. After reinstall, choose the
 * same folder and [DiagramSync.reconcile] copies the diagram files back.
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
        val tree = persistedTreeUri() ?: return null
        return DocumentRemote(appContext, root, tree)
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
        val childRows = queryChildren(appContext, tree, picked)
        val children = childRows.mapNotNull { row ->
            val name = LibraryChildren.displayName(row.name, row.documentId) ?: return@mapNotNull null
            LibraryChild(name = name, directory = LibraryChildren.isDirectory(row.mimeType))
        }
        val choice = LibraryRoot.choose(pickedName = picked.name, children = children)
        return when (choice) {
            LibraryRootChoice.Picked -> picked
            LibraryRootChoice.ExistingChild -> childRows.firstNotNullOfOrNull { row ->
                val name = LibraryChildren.displayName(row.name, row.documentId)
                if (LibraryChildren.isDirectory(row.mimeType) &&
                    name.equals(FOLDER_NAME, ignoreCase = true)
                ) {
                    documentFile(appContext, tree, row)
                } else {
                    null
                }
            }
            LibraryRootChoice.CreateChild -> picked.createDirectory(FOLDER_NAME)
        }
    }

    fun isLibraryDocument(uri: Uri): Boolean {
        val library = libraryDocumentId() ?: return false
        val document = documentId(uri) ?: return false
        return LibraryMembership.directChildName(library, document) != null
    }

    private class DocumentRemote(
        private val context: Context,
        private val root: DocumentFile,
        private val tree: Uri,
    ) : RemoteDiagrams {
        override fun list(): List<RemoteFile> {
            return diagrams().map { (name, row) ->
                RemoteFile(name = name, modifiedAt = row.lastModified, relativePath = name)
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
        }

        override fun delete(name: String) {
            find(name)?.delete()
        }

        private fun find(name: String): DocumentFile? {
            val match = diagrams().firstOrNull { (listed, _) -> listed == name } ?: return null
            documentFile(context, tree, match.second)?.let { return it }
            return root.listFiles().firstOrNull { doc ->
                LibraryChildren.displayName(doc.name, null) == name
            }
        }

        private fun diagrams(): List<Pair<String, LibraryChildRow>> {
            return LibraryChildren.diagrams(queryChildren(context, tree, root)).map { row ->
                val name = checkNotNull(row.name)
                name to row
            }
        }
    }

    companion object {
        const val FOLDER_NAME = LibraryRoot.FOLDER_NAME
        const val DISPLAY_PATH = "Documents/MermaidPocket"
        private const val MIME = "text/plain"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        private const val PAGE_SIZE = 256
        private const val MAX_CHILDREN = 10_000

        internal fun queryChildren(context: Context, tree: Uri, parent: DocumentFile): List<LibraryChildRow> {
            val parentId = runCatching { DocumentsContract.getDocumentId(parent.uri) }.getOrNull()
            if (parentId != null) {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                )
                val paged = queryPaged(context.contentResolver, childrenUri, projection)
                if (paged.isNotEmpty()) return paged
                val plain = context.contentResolver.query(childrenUri, projection, null, null, null)
                    ?.use { cursor -> readRows(cursor) }
                    .orEmpty()
                if (plain.isNotEmpty()) return plain
            }
            return parent.listFiles().map { doc ->
                LibraryChildRow(
                    name = doc.name,
                    mimeType = doc.type,
                    documentId = runCatching { DocumentsContract.getDocumentId(doc.uri) }.getOrNull(),
                    lastModified = doc.lastModified(),
                )
            }
        }

        private fun queryPaged(
            resolver: ContentResolver,
            childrenUri: Uri,
            projection: Array<String>,
        ): List<LibraryChildRow> {
            val rows = mutableListOf<LibraryChildRow>()
            var offset = 0
            while (offset < MAX_CHILDREN) {
                val extras = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                }
                val batch = runCatching {
                    resolver.query(childrenUri, projection, extras, null)?.use { cursor ->
                        readRows(cursor)
                    }
                }.getOrNull() ?: return rows
                if (batch.isEmpty()) return rows
                rows.addAll(batch)
                if (batch.size < PAGE_SIZE) return rows
                offset += batch.size
            }
            return rows
        }

        private fun readRows(cursor: Cursor): List<LibraryChildRow> {
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val rows = mutableListOf<LibraryChildRow>()
            while (cursor.moveToNext()) {
                rows.add(
                    LibraryChildRow(
                        name = if (nameIdx >= 0) cursor.getString(nameIdx) else null,
                        mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx) else null,
                        documentId = if (idIdx >= 0) cursor.getString(idIdx) else null,
                        lastModified = if (modifiedIdx >= 0) cursor.getLong(modifiedIdx) else 0L,
                    ),
                )
            }
            return rows
        }

        private fun documentFile(context: Context, tree: Uri, row: LibraryChildRow): DocumentFile? {
            val id = row.documentId ?: return null
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            return DocumentFile.fromSingleUri(context, uri)
        }
    }
}
