package com.jcompute.mermaidpocket.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jcompute.mermaidpocket.ai.AiConfig
import com.jcompute.mermaidpocket.ai.AiDefaults

/**
 * Settings chrome. Muses may later tweak copy/placement.
 * TODO(Muses): lock section labels and field order if needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: AiConfig,
    onSave: (AiConfig) -> Unit,
    onBack: () -> Unit,
) {
    var openRouter by remember { mutableStateOf(initial.openRouterKey) }
    var brave by remember { mutableStateOf(initial.braveSearchKey) }
    var chatModel by remember { mutableStateOf(initial.chatModel) }
    var classifier by remember { mutableStateOf(initial.classifierModel) }

    fun persist() {
        onSave(
            AiConfig(
                openRouterKey = openRouter,
                braveSearchKey = brave,
                chatModel = chatModel,
                classifierModel = classifier,
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            persist()
                            onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Settings") },
                actions = {
                    TextButton(
                        onClick = {
                            persist()
                            onBack()
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("AI", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Optional. Editing stays on-device. Keys are stored in EncryptedSharedPreferences (Android Keystore), never in plaintext prefs. Network is used only when you send a voice request.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecretField(
                value = openRouter,
                onValueChange = { openRouter = it },
                label = "OpenRouter API key",
                supporting = "Required for voice → diagram. Create a key at openrouter.ai",
            )
            OutlinedTextField(
                value = chatModel,
                onValueChange = { chatModel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Chat model") },
                supportingText = { Text("OpenRouter model id. Default ${AiDefaults.CHAT_MODEL}") },
                singleLine = true,
            )
            OutlinedTextField(
                value = classifier,
                onValueChange = { classifier = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Classifier model") },
                supportingText = {
                    Text(
                        "Jev on OpenRouter Decisions API. Default ${AiDefaults.CLASSIFIER_MODEL}. Alias ${AiDefaults.CLASSIFIER_MODEL_ALIAS} tracks latest.",
                    )
                },
                singleLine = true,
            )
            SecretField(
                value = brave,
                onValueChange = { brave = it },
                label = "Brave Search API key",
                supporting = "Optional. web_lookup uses Brave Search JSON (X-Subscription-Token). Skip for layout-only edits. Get a key at api-dashboard.search.brave.com",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Speech uses Android SpeechRecognizer (free, no extra account). Microphone permission is requested when you tap the mic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supporting: String,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        supportingText = { Text(supporting) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key",
                )
            }
        },
    )
}
