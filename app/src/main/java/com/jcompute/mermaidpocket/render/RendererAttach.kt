package com.jcompute.mermaidpocket.render

/**
 * A WebView document is only ready after **that** document's `onReady`.
 *
 * Rotation (and any AndroidView factory recreate) attaches a new WebView while
 * the controller still thought `ready == true`, so `render()` called
 * `evaluateJavascript` on a blank page. Native never got `onRendered`, View
 * stayed on the paper color — a white screen.
 */
object RendererAttach {
    fun isReady(sameView: Boolean, previouslyReady: Boolean): Boolean {
        return sameView && previouslyReady
    }
}
