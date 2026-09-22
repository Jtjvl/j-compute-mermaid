# Keep the JavaScript bridge used by the bundled mermaid renderer.
-keepclassmembers class com.jcompute.mermaidpocket.render.MermaidJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
