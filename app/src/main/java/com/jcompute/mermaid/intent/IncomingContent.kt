package com.jcompute.mermaid.intent

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.jcompute.mermaid.data.MermaidIngest

data class IncomingDocument(
    val text: String,
    val displayName: String?,
)

object IncomingContent {
    const val EXTRA_HANDLED = "com.jcompute.mermaid.EXTRA_HANDLED"

    fun shouldHandle(intent: Intent?): Boolean {
        if (intent == null) return false
        if (intent.getBooleanExtra(EXTRA_HANDLED, false)) return false
        return when (intent.action) {
            Intent.ACTION_SEND,
            Intent.ACTION_VIEW,
            Intent.ACTION_EDIT,
            -> true
            else -> false
        }
    }

    fun markHandled(intent: Intent) {
        intent.putExtra(EXTRA_HANDLED, true)
    }

    fun read(intent: Intent, resolver: android.content.ContentResolver): IncomingDocument? {
        val document = when (intent.action) {
            Intent.ACTION_SEND -> readSend(intent, resolver)
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> readView(intent, resolver)
            else -> null
        } ?: return null
        return document.copy(text = MermaidIngest.extractSource(document.text))
    }

    private fun readSend(intent: Intent, resolver: android.content.ContentResolver): IncomingDocument? {
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        if (extraText != null) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            return IncomingDocument(extraText, subject)
        }
        val stream = streamUri(intent) ?: return null
        return IncomingDocument(readUri(resolver, stream), displayName(resolver, stream))
    }

    private fun readView(intent: Intent, resolver: android.content.ContentResolver): IncomingDocument? {
        val uri = intent.data ?: streamUri(intent) ?: return null
        return IncomingDocument(readUri(resolver, uri), displayName(resolver, uri))
    }

    private fun streamUri(intent: Intent): Uri? {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    fun readUri(resolver: android.content.ContentResolver, uri: Uri): String {
        resolver.takePersistableReadIfPossible(uri)
        return resolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.size > MAX_BYTES) {
                error("File is too large to open (limit ${MAX_BYTES / 1024} KB).")
            }
            bytes.toString(Charsets.UTF_8)
        } ?: error("Could not read the selected file.")
    }

    fun displayName(resolver: android.content.ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private fun android.content.ContentResolver.takePersistableReadIfPossible(uri: Uri) {
        runCatching {
            takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private const val MAX_BYTES = 1_000_000
}
