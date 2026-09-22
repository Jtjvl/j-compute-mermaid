package com.jcompute.mermaidpocket.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ExportWriter(private val context: Context) {
    fun writeClipboardPng(bytes: ByteArray): Uri {
        return writeCacheFile(bytes, "clipboard.png")
    }

    /** Cache file handed to the share sheet. Not the user’s saved copy. */
    fun writeShare(bytes: ByteArray, fileName: String): Uri {
        val safe = ExportNames.safeFileName(fileName)
        return writeCacheFile(bytes, "${System.currentTimeMillis()}-$safe")
    }

    private fun writeCacheFile(bytes: ByteArray, fileName: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
