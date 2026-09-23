package com.jcompute.mermaidpocket.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jcompute.mermaidpocket.data.RemoteFile

/**
 * In-app Open list of the granted library tree.
 *
 * System SAF Back walks parent folders. This dialog's Back / dismiss returns
 * to the app. Browse storage… still uses the system picker for files outside
 * the granted tree.
 */
@Composable
fun LibraryOpenDialog(
    files: List<RemoteFile>,
    onPick: (RemoteFile) -> Unit,
    onBrowseStorage: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (files.isEmpty()) {
                    Text("No .mmd files in the diagram folder yet. Browse storage to open one.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(files, key = { it.name + "\n" + it.relativePath }) { file ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = file.relativePath,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(file) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onBrowseStorage) { Text("Browse storage…") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
