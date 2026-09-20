package com.jcompute.mermaid.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ExportWriter(private val context: Context) {
    fun writeClipboardPng(bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "clipboard.png")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
