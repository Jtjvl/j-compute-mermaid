package com.jcompute.mermaid.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.jcompute.mermaid.data.TitleParser
import java.io.File

class ExportWriter(private val context: Context) {
    fun write(format: ExportFormat, title: String, bytes: ByteArray): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val file = File(dir, "${TitleParser.fileSlug(title)}-$stamp.${format.extension}")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
