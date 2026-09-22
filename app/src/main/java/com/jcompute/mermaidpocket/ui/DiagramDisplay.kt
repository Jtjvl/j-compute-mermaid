package com.jcompute.mermaidpocket.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * View-mode zoom bounds and PNG bake scale.
 *
 * On-screen View is a live SVG (WebView). Clipboard export stays raster, so bake
 * at device pixel ratio and never at 1×.
 */
object DiagramDisplay {
    const val MIN_ZOOM = 0.5f
    const val MAX_ZOOM = 5f
    const val FIT_ZOOM = 1f

    fun clampZoom(scale: Float): Float = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun applyPinch(current: Float, zoom: Float): Float = clampZoom(current * zoom)

    /**
     * PNG export multiplier from [android.util.DisplayMetrics.density].
     * Never 1×; 3× on XXHDPI (density ≥ ~2.75) and denser.
     */
    fun pngScale(density: Float): Int {
        val rounded = density.coerceAtLeast(1f).roundToInt()
        return max(2, min(3, rounded))
    }

    fun fitOffset(): Offset = Offset.Zero
}
