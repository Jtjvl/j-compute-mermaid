# Keep the JavaScript bridge used by the bundled mermaid renderer.
-keepclassmembers class com.jcompute.mermaid.render.MermaidJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
