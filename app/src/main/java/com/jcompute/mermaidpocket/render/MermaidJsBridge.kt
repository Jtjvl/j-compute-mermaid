package com.jcompute.mermaidpocket.render

import android.webkit.JavascriptInterface

class MermaidJsBridge(
    private val onReady: () -> Unit,
    private val onRendered: (ok: Boolean, error: String, width: Int, height: Int) -> Unit,
    private val onExport: (kind: String, payload: String) -> Unit,
) {
    @JavascriptInterface
    fun onReady() = onReady.invoke()

    @JavascriptInterface
    fun onRendered(ok: Boolean, error: String, width: Int, height: Int) {
        onRendered.invoke(ok, error, width, height)
    }

    @JavascriptInterface
    fun onExport(kind: String, payload: String) {
        onExport.invoke(kind, payload)
    }
}
