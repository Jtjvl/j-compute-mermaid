package com.jcompute.mermaid.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jcompute.mermaid.data.StoredDiagram
import java.io.File
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTopBar(
    selection: FileSelection,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            if (selection.active) {
                IconButton(onClick = onCancelSelection) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cancel selection")
                }
            }
        },
        title = {
            Text(
                text = if (selection.active) {
                    if (selection.count == 1) "1 selected" else "${selection.count} selected"
                } else {
                    "Files"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            if (selection.active) {
                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selection.count > 0,
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                }
            }
        },
    )
}

@Composable
fun BulkDeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete diagrams?") },
        text = {
            Text(
                if (count == 1) {
                    "Delete 1 saved diagram. This cannot be undone."
                } else {
                    "Delete $count saved diagrams. This cannot be undone."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesPane(
    files: List<StoredDiagram>,
    currentPath: String?,
    selection: FileSelection,
    onOpen: (File) -> Unit,
    onDelete: (File) -> Unit,
    onEnterSelection: (File) -> Unit,
    onToggleSelected: (File) -> Unit,
    onCancelSelection: () -> Unit,
    onPaste: () -> Unit,
    onOpenExternal: () -> Unit,
    onNew: () -> Unit,
) {
    BackHandler(enabled = selection.active) {
        onCancelSelection()
    }

    Column(Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Nothing saved yet. Paste from chat, then Save .mmd.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onPaste) { Text("Paste") }
                TextButton(onClick = onOpenExternal) { Text("Open…") }
                TextButton(onClick = onNew) { Text("New") }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(files, key = { it.file.absolutePath }) { diagram ->
                    FileListEntry(
                        diagram = diagram,
                        currentPath = currentPath,
                        selection = selection,
                        onOpen = onOpen,
                        onDelete = onDelete,
                        onEnterSelection = onEnterSelection,
                        onToggleSelected = onToggleSelected,
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FileListEntry(
    diagram: StoredDiagram,
    currentPath: String?,
    selection: FileSelection,
    onOpen: (File) -> Unit,
    onDelete: (File) -> Unit,
    onEnterSelection: (File) -> Unit,
    onToggleSelected: (File) -> Unit,
) {
    val selected = selection.contains(diagram.file.canonicalPath)
    val row = @Composable {
        FileRow(
            diagram = diagram,
            currentPath = currentPath,
            selecting = selection.active,
            selected = selected,
            onClick = {
                if (selection.active) {
                    onToggleSelected(diagram.file)
                } else {
                    onOpen(diagram.file)
                }
            },
            onLongClick = {
                if (selection.active) {
                    onToggleSelected(diagram.file)
                } else {
                    onEnterSelection(diagram.file)
                }
            },
        )
    }
    if (selection.active) {
        row()
        return
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart ||
                value == SwipeToDismissBoxValue.StartToEnd
            ) {
                onDelete(diagram.file)
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
    ) {
        row()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    diagram: StoredDiagram,
    currentPath: String?,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(diagram.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(diagram.updatedAtMillis)),
            )
        },
        leadingContent = {
            DiagramThumbnail(file = diagram.thumbnail)
        },
        trailingContent = {
            when {
                selecting -> {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                    )
                }
                diagram.file.canonicalPath == currentPath -> {
                    Text(
                        "Open",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    )
}

@Composable
private fun DiagramThumbnail(file: File?) {
    var bitmap by remember(file?.absolutePath, file?.lastModified()) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(file?.absolutePath, file?.lastModified()) {
        bitmap = withContext(Dispatchers.IO) {
            val path = file?.takeIf { it.isFile }?.absolutePath ?: return@withContext null
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    Surface(
        modifier = Modifier.size(width = 72.dp, height = 56.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        val preview = bitmap
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
