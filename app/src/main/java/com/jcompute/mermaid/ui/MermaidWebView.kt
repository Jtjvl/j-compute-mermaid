package com.jcompute.mermaid.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jcompute.mermaid.render.MermaidJsBridge
import com.jcompute.mermaid.render.MermaidRendererController

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidWebView(
    source: String,
    darkTheme: Boolean,
    controller: MermaidRendererController,
    onReady: () -> Unit,
    onRendered: (ok: Boolean, error: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = remember(controller) {
        MermaidJsBridge(
            onReady = {
                controller.markReady()
                onReady()
            },
            onRendered = { ok, error, _, _ ->
                onRendered(ok, error)
            },
            onExport = { kind, payload ->
                controller.deliverExport(kind, payload)
            },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Bundled mermaid.min.js is a sibling under android_asset/.
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.blockNetworkLoads = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                isNestedScrollingEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                    ): Boolean = true
                }
                addJavascriptInterface(bridge, "MermaidBridge")
                controller.attach(this)
                loadUrl("file:///android_asset/mermaid/index.html")
            }
        },
        update = { view ->
            controller.attach(view)
        },
    )

    LaunchedEffect(source, darkTheme) {
        controller.render(source, darkTheme)
    }

    DisposableEffect(controller) {
        onDispose {
            // The AndroidView owns the WebView lifecycle; only drop the controller hook.
        }
    }
}
