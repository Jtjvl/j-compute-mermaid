package com.jcompute.mermaidpocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcompute.mermaidpocket.ui.EditorViewModel
import com.jcompute.mermaidpocket.ui.MermaidAppRoot
import com.jcompute.mermaidpocket.ui.theme.MermaidTheme

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
            MermaidTheme(appearance = state.appearance) {
                MermaidAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.handleIntent(intent)
    }
}
