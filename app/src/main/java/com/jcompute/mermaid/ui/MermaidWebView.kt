package com.jcompute.mermaid.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jcompute.mermaid.render.MermaidJsBridge
import com.jcompute.mermaid.render.MermaidRendererController

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MermaidEngine(
    source: String,
    darkTheme: Boolean,
    controller: MermaidRendererController,
    onRendered: (ok: Boolean, error: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paper = if (darkTheme) 0xFF121212.toInt() else 0xFFFAFAFA.toInt()
    val bridge = remember(controller) {
        MermaidJsBridge(
            onReady = { controller.markReady() },
            onRendered = { ok, error, _, _ -> onRendered(ok, error) },
            onExport = { kind, payload -> controller.deliverExport(kind, payload) },
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
                setBackgroundColor(paper)
                setInitialScale(100)
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isClickable = true
                isFocusable = false
                isFocusableInTouchMode = false
                isNestedScrollingEnabled = false
                setOnTouchListener { view, _ ->
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.blockNetworkLoads = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
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
            view.setBackgroundColor(paper)
        },
    )

    LaunchedEffect(source, darkTheme) {
        controller.render(source, darkTheme)
    }
}
