package com.jcompute.mermaid.ui

/**
 * View loading is driven by [epoch], not source equality.
 *
 * Pasting or re-opening the same Mermaid used to set `isRendering = true` while
 * `LaunchedEffect(source, theme)` did not rerun, so the WebView never called
 * back and the spinner never cleared. Each explicit open/ingest/theme change
 * must advance [epoch] so the engine invokes JS again (or the cached SVG path
 * notifies immediately).
 */
data class RenderPass(
    val epoch: Long,
    val showSpinner: Boolean,
) {
    companion object {
        fun next(previousEpoch: Long, source: String): RenderPass {
            return RenderPass(
                epoch = previousEpoch + 1L,
                showSpinner = source.isNotBlank(),
            )
        }
    }
}
