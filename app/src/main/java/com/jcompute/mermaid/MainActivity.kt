package com.jcompute.mermaid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcompute.mermaid.ui.EditorViewModel
import com.jcompute.mermaid.ui.MermaidAppRoot
import com.jcompute.mermaid.ui.theme.MermaidTheme
import com.jcompute.mermaid.ui.theme.resolvesDark

class MainActivity : ComponentActivity() {
    private val viewModel: EditorViewModel by viewModels {
        EditorViewModel.Factory(application as MermaidApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.handleIntent(intent)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = state.appearance.resolvesDark(systemDark)
            MermaidTheme(appearance = state.appearance) {
                MermaidAppRoot(viewModel = viewModel, darkTheme = dark)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}
