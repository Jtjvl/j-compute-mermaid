package com.jcompute.mermaidpocket.ui

/**
 * Second tap on View (already viewing the document) toggles immersive chrome.
 * Any other mode/screen change leaves full-view.
 */
data class ViewChrome(
    val mode: EditorMode,
    val screen: Screen,
    val immersive: Boolean,
) {
    companion object {
        fun onMode(
            currentMode: EditorMode,
            currentScreen: Screen,
            immersive: Boolean,
            requested: EditorMode,
        ): ViewChrome {
            val alreadyViewing =
                requested == EditorMode.View &&
                    currentMode == EditorMode.View &&
                    currentScreen == Screen.Document
            if (alreadyViewing) {
                return ViewChrome(
                    mode = EditorMode.View,
                    screen = Screen.Document,
                    immersive = !immersive,
                )
            }
            return ViewChrome(
                mode = requested,
                screen = Screen.Document,
                immersive = false,
            )
        }
    }
}

/**
 * Edit-view Save control.
 *
 * Stays visible for a blank (no linked file). The first tap creates a library
 * `.mmd` and links it; later taps overwrite that file. The glyph has no text label.
 */
data class EditSaveChrome(
    val contentDescription: String,
    val createsFile: Boolean,
) {
    companion object {
        const val SAVE = "Save"
        const val SAVE_AS = "Save as"

        fun visible(screen: Screen, mode: EditorMode): Boolean {
            return screen == Screen.Document && mode == EditorMode.Edit
        }

        fun of(linked: Boolean): EditSaveChrome {
            return EditSaveChrome(
                contentDescription = if (linked) SAVE else SAVE_AS,
                createsFile = !linked,
            )
        }
    }
}
