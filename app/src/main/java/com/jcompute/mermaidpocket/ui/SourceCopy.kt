package com.jcompute.mermaidpocket.ui

object SourceCopy {
    /**
     * Clipboard payload for Copy all: the entire Edit buffer.
     * Empty source has nothing to copy.
     */
    fun payload(source: String): String? = source.takeIf { it.isNotEmpty() }
}
