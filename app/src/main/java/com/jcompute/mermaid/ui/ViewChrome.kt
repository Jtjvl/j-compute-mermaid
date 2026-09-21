package com.jcompute.mermaid.ui

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
