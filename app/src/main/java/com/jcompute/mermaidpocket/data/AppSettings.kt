package com.jcompute.mermaidpocket.data

import android.content.Context
import com.jcompute.mermaidpocket.ai.AiDefaults
import com.jcompute.mermaidpocket.export.ExportSize

enum class Appearance {
    System,
    Light,
    Dark,
}

/**
 * Diagram (Mermaid) theme, independent of app chrome [Appearance].
 *
 * mermaid.live's pretty ER palette is **not** `theme: default` or `theme: dark`.
 * Mermaid 12 only stamps per-entity rainbow strokes when the active theme is
 * `redux-color` or `redux-dark-color` (see mermaid's `isColorTheme` set).
 */
enum class DiagramTheme {
    Default,
    Dark,
    ;

    /** Mermaid theme id passed to `mermaid.initialize`. */
    val mermaidId: String
        get() = when (this) {
            Default -> "redux-color"
            Dark -> "redux-dark-color"
        }

    val canvasHex: String
        get() = when (this) {
            Default -> "#ffffff"
            Dark -> "#111113"
        }

    val canvasArgb: Int
        get() = when (this) {
            Default -> 0xFFFFFFFF.toInt()
            Dark -> 0xFF111113.toInt()
        }

    val isDarkCanvas: Boolean
        get() = this == Dark

    val menuLabel: String
        get() = when (this) {
            Default -> "Default"
            Dark -> "Dark"
        }

    fun next(): DiagramTheme = when (this) {
        Default -> Dark
        Dark -> Default
    }

    companion object {
        fun fromStored(value: String?): DiagramTheme {
            if (value.isNullOrBlank()) return Default
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Default
        }
    }
}

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("mermaid", Context.MODE_PRIVATE)

    fun appearance(): Appearance {
        val stored = prefs.getString(KEY_APPEARANCE, Appearance.System.name)
        return runCatching { Appearance.valueOf(stored ?: Appearance.System.name) }
            .getOrDefault(Appearance.System)
    }

    fun setAppearance(value: Appearance) {
        prefs.edit().putString(KEY_APPEARANCE, value.name).apply()
    }

    fun diagramTheme(): DiagramTheme {
        return DiagramTheme.fromStored(prefs.getString(KEY_DIAGRAM_THEME, DiagramTheme.Default.name))
    }

    fun setDiagramTheme(value: DiagramTheme) {
        prefs.edit().putString(KEY_DIAGRAM_THEME, value.name).apply()
    }

    fun exportWidth(): Int {
        return ExportSize.clampWidth(prefs.getInt(KEY_EXPORT_WIDTH, ExportSize.DEFAULT_WIDTH))
    }

    fun setExportWidth(px: Int) {
        prefs.edit().putInt(KEY_EXPORT_WIDTH, ExportSize.clampWidth(px)).apply()
    }

    /** Tree URI from [android.content.Intent.ACTION_OPEN_DOCUMENT_TREE]. Empty when unset. */
    fun diagramTreeUri(): String? = prefs.getString(KEY_DIAGRAM_TREE, null)?.takeIf { it.isNotBlank() }

    fun setDiagramTreeUri(value: String) {
        prefs.edit().putString(KEY_DIAGRAM_TREE, value).apply()
    }

    fun chatModel(): String {
        return prefs.getString(KEY_CHAT_MODEL, null)?.trim()?.takeIf { it.isNotEmpty() }
            ?: AiDefaults.CHAT_MODEL
    }

    fun setChatModel(value: String) {
        val stored = value.trim().ifEmpty { AiDefaults.CHAT_MODEL }
        prefs.edit().putString(KEY_CHAT_MODEL, stored).apply()
    }

    fun classifierModel(): String {
        return prefs.getString(KEY_CLASSIFIER_MODEL, null)?.trim()?.takeIf { it.isNotEmpty() }
            ?: AiDefaults.CLASSIFIER_MODEL
    }

    fun setClassifierModel(value: String) {
        val stored = value.trim().ifEmpty { AiDefaults.CLASSIFIER_MODEL }
        prefs.edit().putString(KEY_CLASSIFIER_MODEL, stored).apply()
    }

    private companion object {
        const val KEY_APPEARANCE = "appearance"
        const val KEY_DIAGRAM_THEME = "diagram_theme"
        const val KEY_EXPORT_WIDTH = "export_width"
        const val KEY_DIAGRAM_TREE = "diagram_tree_uri"
        const val KEY_CHAT_MODEL = "chat_model"
        const val KEY_CLASSIFIER_MODEL = "classifier_model"
    }
}
