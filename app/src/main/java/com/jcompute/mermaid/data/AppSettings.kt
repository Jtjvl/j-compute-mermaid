package com.jcompute.mermaid.data

import android.content.Context

enum class Appearance {
    System,
    Light,
    Dark,
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

    private companion object {
        const val KEY_APPEARANCE = "appearance"
    }
}
