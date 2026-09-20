package com.jcompute.mermaid.export

enum class ExportFormat(
    val label: String,
    val mimeType: String,
    val extension: String,
) {
    PNG("PNG image", "image/png", "png"),
    SVG("SVG vector", "image/svg+xml", "svg"),
    MMD("Mermaid source", "text/plain", "mmd"),
    TEXT("Plain text", "text/plain", "txt"),
}
