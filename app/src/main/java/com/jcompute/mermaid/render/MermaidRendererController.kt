package com.jcompute.mermaid.render

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import org.json.JSONObject
import java.util.ArrayDeque
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class MermaidRendererController {
    private val main = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var ready = false
    private var lastRenderOk = false
    private var lastError: String? = null
    private val pending = ArrayDeque<() -> Unit>()
    private val exportWaiters = ArrayDeque<(Result<Pair<String, String>>) -> Unit>()

    fun attach(view: WebView) {
        webView = view
    }

    fun detach(view: WebView) {
        if (webView === view) {
            webView = null
            ready = false
        }
    }

    fun markReady() {
        ready = true
        while (pending.isNotEmpty()) {
            pending.removeFirst().invoke()
        }
    }

    fun markRendered(ok: Boolean, error: String) {
        lastRenderOk = ok
        lastError = error.takeIf { it.isNotBlank() }
    }

    fun deliverExport(kind: String, payload: String) {
        val waiter = synchronized(exportWaiters) {
            if (exportWaiters.isEmpty()) null else exportWaiters.removeFirst()
        } ?: return
        if (kind == "error") {
            waiter(Result.failure(IllegalStateException(payload.ifBlank { "Export failed." })))
        } else {
            waiter(Result.success(kind to payload))
        }
    }

    fun render(source: String, dark: Boolean) {
        runWhenReady {
            val view = webView ?: return@runWhenReady
            val theme = if (dark) "dark" else "default"
            val script = "renderDiagram(${JSONObject.quote(source)}, ${JSONObject.quote(theme)});"
            view.evaluateJavascript(script, null)
        }
    }

    suspend fun exportSvg(): ByteArray {
        val payload = requestExport("exportSvg();")
        if (payload.isBlank()) error("Renderer returned an empty SVG.")
        return payload.toByteArray(Charsets.UTF_8)
    }

    suspend fun exportPng(scale: Double = 2.0, background: String): ByteArray {
        val payload = requestExport("exportPng($scale, ${JSONObject.quote(background)});")
        val data = payload.substringAfter("base64,", missingDelimiterValue = "")
        if (data.isBlank()) error("Renderer returned an empty PNG.")
        return Base64.decode(data, Base64.DEFAULT)
    }

    private suspend fun requestExport(script: String): String {
        return suspendCancellableCoroutine { cont ->
            val waiter: (Result<Pair<String, String>>) -> Unit = { result ->
                result.fold(
                    onSuccess = { cont.resume(it.second) },
                    onFailure = { cont.resumeWithException(it) },
                )
            }
            synchronized(exportWaiters) { exportWaiters.addLast(waiter) }
            cont.invokeOnCancellation {
                synchronized(exportWaiters) { exportWaiters.remove(waiter) }
            }
            runWhenReady {
                val view = webView
                if (view == null) {
                    deliverExport("error", "Renderer is not ready.")
                    return@runWhenReady
                }
                view.evaluateJavascript(script, null)
            }
        }
    }

    private fun runWhenReady(block: () -> Unit) {
        val action = { main.post(block) }
        if (ready && webView != null) {
            action()
        } else {
            pending.addLast(action)
        }
    }
}
