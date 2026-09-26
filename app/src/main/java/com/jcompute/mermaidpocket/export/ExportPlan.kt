package com.jcompute.mermaidpocket.export

import java.util.Locale
import kotlin.math.roundToInt

enum class ExportDelivery { Share, Save }

enum class ExportFormat(
    val mime: String,
    val extension: String,
    val menuLabel: String,
    val sized: Boolean,
) {
    Png("image/png", "png", "PNG", true),
    Jpeg("image/jpeg", "jpg", "JPEG", true),
    Svg("image/svg+xml", "svg", "SVG", false),
    Mmd("text/plain", "mmd", ".mmd", false),
}

/** Order shown when Save or Share is pressed. */
val ExportOptionOrder = listOf(
    ExportFormat.Png,
    ExportFormat.Svg,
    ExportFormat.Jpeg,
    ExportFormat.Mmd,
)

object ExportNames {
    fun fileName(sourceName: String, format: ExportFormat): String {
        val leaf = sourceName.substringAfterLast('/').substringAfterLast('\\').trim()
        val base = leaf
            .removeSuffix(".mmd")
            .removeSuffix(".mermaid")
            .removeSuffix(".txt")
            .removeSuffix(".md")
            .removeSuffix(".markdown")
            .removeSuffix(".svg")
            .removeSuffix(".png")
            .removeSuffix(".jpg")
            .removeSuffix(".jpeg")
            .trim()
            .ifBlank { "diagram" }
        return safeFileName("$base.${format.extension}")
    }

    fun safeFileName(name: String): String {
        val leaf = name.substringAfterLast('/').substringAfterLast('\\').trim()
        val cleaned = leaf.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_', '.')
        return cleaned.ifBlank { "diagram" }.take(80)
    }
}

/**
 * Raster export size. Width is what the user picks; height keeps the diagram
 * aspect. Both sides stay inside [MIN_WIDTH]..[MAX_WIDTH] so a tall diagram
 * cannot allocate a huge bitmap.
 *
 * The same fit is applied in `bridge.js` (`fitExportSize`).
 */
object ExportSize {
    const val DEFAULT_WIDTH = 1920
    const val MIN_WIDTH = 256
    const val MAX_WIDTH = 8192

    fun clampWidth(raw: Int): Int = raw.coerceIn(MIN_WIDTH, MAX_WIDTH)

    /** Empty input uses [DEFAULT_WIDTH]. Non-numeric input is rejected. */
    fun parseWidth(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DEFAULT_WIDTH
        val value = trimmed.toIntOrNull() ?: return null
        return clampWidth(value)
    }

    fun outputSize(requestedWidth: Int, sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        var width = clampWidth(requestedWidth)
        var height = if (sourceWidth > 0 && sourceHeight > 0) {
            (width.toDouble() * sourceHeight.toDouble() / sourceWidth.toDouble()).roundToInt()
        } else {
            width
        }
        if (height < 1) height = 1
        if (height > MAX_WIDTH) {
            val scale = MAX_WIDTH.toDouble() / height.toDouble()
            width = (width * scale).roundToInt().coerceAtLeast(1)
            height = (height * scale).roundToInt().coerceAtLeast(1)
            if (width > MAX_WIDTH) width = MAX_WIDTH
            if (height > MAX_WIDTH) height = MAX_WIDTH
        }
        return width to height
    }

    /**
     * Rough byte estimate from pixel count. PNG diagrams are mostly flat color
     * (~0.4 byte/pixel). JPEG at quality 0.92 is smaller (~0.2 byte/pixel).
     * SVG and `.mmd` are not sized this way.
     */
    fun estimateBytes(width: Int, height: Int, format: ExportFormat): Long? {
        if (!format.sized || width <= 0 || height <= 0) return null
        val pixels = width.toLong() * height.toLong()
        val numerator = if (format == ExportFormat.Jpeg) 1L else 2L
        return (pixels * numerator) / 5L
    }

    fun approxLabel(bytes: Long): String {
        val kb = bytes / 1024.0
        return if (bytes < 1024L * 1024L) {
            "About ${kb.roundToInt().coerceAtLeast(1)} KB"
        } else {
            "About ${String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0))} MB"
        }
    }
}
