package com.jcompute.mermaidpocket.render

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
    private val parseWaiters = ArrayDeque<(Result<Unit>) -> Unit>()

    fun attach(view: WebView) {
        ready = RendererAttach.isReady(sameView = webView === view, previouslyReady = ready)
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

    fun deliverParse(ok: Boolean, error: String) {
        val waiter = synchronized(parseWaiters) {
            if (parseWaiters.isEmpty()) null else parseWaiters.removeFirst()
        } ?: return
        if (ok) {
            waiter(Result.success(Unit))
        } else {
            waiter(Result.failure(IllegalArgumentException(error.ifBlank { "Invalid Mermaid." })))
        }
    }

    /**
     * Parse-only check. Does not replace the on-screen SVG (verify-before-apply).
     */
    suspend fun parse(source: String, mermaidTheme: String): Result<Unit> {
        return suspendCancellableCoroutine { cont ->
            val waiter: (Result<Unit>) -> Unit = { result ->
                if (cont.isActive) cont.resume(result)
            }
            synchronized(parseWaiters) { parseWaiters.addLast(waiter) }
            cont.invokeOnCancellation {
                synchronized(parseWaiters) { parseWaiters.remove(waiter) }
            }
            runWhenReady {
                val view = webView
                if (view == null) {
                    deliverParse(false, "Renderer is not ready.")
                    return@runWhenReady
                }
                val script = "parseDiagram(${JSONObject.quote(source)}, ${JSONObject.quote(mermaidTheme)});"
                view.evaluateJavascript(script, null)
            }
        }
    }

    fun render(source: String, mermaidTheme: String) {
        runWhenReady {
            val view = webView ?: return@runWhenReady
            val script = "renderDiagram(${JSONObject.quote(source)}, ${JSONObject.quote(mermaidTheme)});"
            view.evaluateJavascript(script, null)
        }
    }

    fun setViewTransform(scale: Float, xCss: Float, yCss: Float) {
        runWhenReady {
            val view = webView ?: return@runWhenReady
            val script =
                "setViewTransform(${scale.toDouble()}, ${xCss.toDouble()}, ${yCss.toDouble()});"
            view.evaluateJavascript(script, null)
        }
    }

    fun resetViewTransform() {
        runWhenReady {
            val view = webView ?: return@runWhenReady
            view.evaluateJavascript("resetViewTransform();", null)
        }
    }

    /** Refit the current SVG to the WebView bounds without a mermaid re-render. */
    fun relayout() {
        runWhenReady {
            val view = webView ?: return@runWhenReady
            view.evaluateJavascript("relayoutView();", null)
        }
    }

    suspend fun exportSvg(): ByteArray {
        val payload = requestExport("exportSvg();")
        if (payload.isBlank()) error("Renderer returned an empty SVG.")
        return payload.toByteArray(Charsets.UTF_8)
    }

    suspend fun exportPng(scale: Double = 2.0, background: String): ByteArray {
        val payload = requestExport("exportPng($scale, ${JSONObject.quote(background)});")
        return decodeDataUrl(payload, "Renderer returned an empty PNG.")
    }

    /** PNG or JPEG baked to [widthPx]. Height is chosen in JS to keep aspect. */
    suspend fun exportRaster(widthPx: Int, jpeg: Boolean, background: String): ByteArray {
        val mime = if (jpeg) "image/jpeg" else "image/png"
        val payload = requestExport(
            "exportRaster($widthPx, ${JSONObject.quote(mime)}, ${JSONObject.quote(background)});",
        )
        return decodeDataUrl(payload, "Renderer returned an empty image.")
    }

    private fun decodeDataUrl(payload: String, emptyMessage: String): ByteArray {
        val data = payload.substringAfter("base64,", missingDelimiterValue = "")
        if (data.isBlank()) error(emptyMessage)
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
        val action: () -> Unit = { main.post(block) }
        if (ready && webView != null) {
            action()
        } else {
            pending.addLast(action)
        }
    }
}
