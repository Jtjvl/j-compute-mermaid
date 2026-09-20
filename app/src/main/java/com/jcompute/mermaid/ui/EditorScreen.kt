package com.jcompute.mermaid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcompute.mermaid.data.Diagram
import com.jcompute.mermaid.data.Samples
import com.jcompute.mermaid.export.ExportFormat
import com.jcompute.mermaid.intent.IncomingContent
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MermaidAppRoot(
    viewModel: EditorViewModel,
    darkTheme: Boolean,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = IncomingContent.readUri(context.contentResolver, uri)
            val name = IncomingContent.displayName(context.contentResolver, uri)
            viewModel.importText(text, name, preferEdit = true)
        }.onFailure { error ->
            viewModel.notify(error.message ?: "Could not open that file.")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
                is UiEvent.ShareFile -> shareFile(context, event.uri, event.mimeType, event.title)
                is UiEvent.ShareText -> shareText(context, event.text, event.title)
            }
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = state.title,
                mode = state.mode,
                exporting = state.isExporting,
                onModeChange = viewModel::setMode,
                onRecents = { viewModel.setRecentsOpen(true) },
                onExport = { viewModel.setExportOpen(true) },
                onNew = { viewModel.newDiagram() },
                onOpen = {
                    openDocument.launch(
                        arrayOf(
                            "text/plain",
                            "text/*",
                            "text/x-mermaid",
                            "application/octet-stream",
                            "*/*",
                        ),
                    )
                },
                onPaste = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val text = clipboard.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                    viewModel.pasteFromClipboard(text)
                },
                onSamples = { viewModel.setSamplesOpen(true) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        EditorCanvas(
            state = state,
            darkTheme = darkTheme,
            viewModel = viewModel,
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize(),
        )
    }

    if (state.recentsOpen) {
        RecentsSheet(
            recents = state.recents,
            currentId = state.currentId,
            onDismiss = { viewModel.setRecentsOpen(false) },
            onOpen = viewModel::openRecent,
            onDelete = viewModel::deleteRecent,
            onNew = { viewModel.newDiagram() },
        )
    }
    if (state.exportOpen) {
        ExportSheet(
            onDismiss = { viewModel.setExportOpen(false) },
            onExport = { format -> viewModel.export(format, darkTheme) },
        )
    }
    if (state.samplesOpen) {
        SamplesSheet(
            onDismiss = { viewModel.setSamplesOpen(false) },
            onPick = { viewModel.applySample(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    mode: EditorMode,
    exporting: Boolean,
    onModeChange: (EditorMode) -> Unit,
    onRecents: () -> Unit,
    onExport: () -> Unit,
    onNew: () -> Unit,
    onOpen: () -> Unit,
    onPaste: () -> Unit,
    onSamples: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxWidth()) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        navigationIcon = {
            IconButton(onClick = onRecents) {
                Icon(Icons.Outlined.History, contentDescription = "Recents")
            }
        },
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            if (exporting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp,
                )
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Outlined.IosShare, contentDescription = "Export")
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("New diagram") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onNew()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Open file") },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Paste from clipboard") },
                        leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onPaste()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Samples") },
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSamples()
                        },
                    )
                }
            }
        },
    )
        ModeToggle(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider()
    }
    }
}

@Composable
private fun ModeToggle(
    mode: EditorMode,
    onModeChange: (EditorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(EditorMode.Edit to "Edit", EditorMode.View to "View")
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onModeChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {
                    Icon(
                        imageVector = if (value == EditorMode.Edit) Icons.Outlined.Code else Icons.Outlined.Visibility,
                        contentDescription = null,
                    )
                },
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun EditorCanvas(
    state: EditorUiState,
    darkTheme: Boolean,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val widthDp = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val offsetFraction by animateFloatAsState(
            targetValue = if (state.mode == EditorMode.View) -1f else 0f,
            animationSpec = tween(durationMillis = 280),
            label = "edit-view-slide",
        )

        Row(
            modifier = Modifier
                .width(widthDp * 2)
                .fillMaxHeight()
                .offset { IntOffset(x = (offsetFraction * widthPx).roundToInt(), y = 0) },
        ) {
            SourceEditor(
                source = state.source,
                onSourceChange = viewModel::onSourceChange,
                modifier = Modifier
                    .width(widthDp)
                    .fillMaxHeight(),
            )
            DiagramPane(
                source = state.source,
                darkTheme = darkTheme,
                isLoading = state.isLoading,
                viewModel = viewModel,
                modifier = Modifier
                    .width(widthDp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SourceEditor(
    source: String,
    onSourceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        BasicTextField(
            value = source,
            onValueChange = onSourceChange,
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scroll)
                .padding(16.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (source.isEmpty()) {
                        Text(
                            text = "flowchart TD\n    Start --> End",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            ),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun DiagramPane(
    source: String,
    darkTheme: Boolean,
    isLoading: Boolean,
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!isLoading) {
                MermaidWebView(
                    source = source,
                    darkTheme = darkTheme,
                    controller = viewModel.renderer,
                    onReady = {},
                    onRendered = viewModel::onRenderResult,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentsSheet(
    recents: List<Diagram>,
    currentId: String,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = "Recents",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        TextButton(
            onClick = onNew,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("New diagram")
        }
        if (recents.isEmpty()) {
            Text(
                text = "No saved diagrams yet. New work is stored on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(recents, key = { it.id }) { diagram ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = diagram.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = relativeTime(diagram.updatedAtMillis),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        trailingContent = {
                            Row {
                                if (diagram.id == currentId) {
                                    Text(
                                        text = "Open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }
                                IconButton(onClick = { onDelete(diagram.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(diagram.id) }
                            .padding(horizontal = 4.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportSheet(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Export",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Text(
            text = "Share through the Android sharesheet as PNG, SVG, or Mermaid source.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        ExportFormat.entries.forEach { format ->
            ListItem(
                headlineContent = { Text(format.label) },
                supportingContent = { Text(".${format.extension}  ·  ${format.mimeType}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExport(format) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SamplesSheet(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Samples",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Samples.all.forEach { sample ->
            ListItem(
                headlineContent = { Text(sample.name) },
                supportingContent = {
                    Text(
                        text = sample.source.lineSequence().first { it.isNotBlank() },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    TextButton(onClick = { onPick(sample.source) }) { Text("Use") }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun relativeTime(millis: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
}

private fun shareFile(
    context: android.content.Context,
    uri: Uri,
    mimeType: String,
    title: String,
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export diagram"))
}

private fun shareText(context: android.content.Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, title)
    }
    context.startActivity(Intent.createChooser(intent, "Share diagram"))
}
