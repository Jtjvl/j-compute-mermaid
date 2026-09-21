package com.jcompute.mermaid.ui

import android.content.ClipboardManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcompute.mermaid.data.Appearance
import com.jcompute.mermaid.data.DiagramTheme
import com.jcompute.mermaid.data.StoredDiagram
import com.jcompute.mermaid.intent.IncomingContent
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MermaidAppRoot(
    viewModel: EditorViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val paper = Color(state.diagramTheme.canvasArgb)

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = IncomingContent.readUri(context.contentResolver, uri)
            val name = IncomingContent.displayName(context.contentResolver, uri)
            viewModel.openImported(text, name)
        }.onFailure { error ->
            viewModel.notify(error.message ?: "Could not open that file.")
        }
    }

    val saveAs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveToUri(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    fun paste() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        viewModel.pasteFromClipboard(text)
    }

    val showLiveDiagram =
        state.screen == Screen.Document &&
            state.mode == EditorMode.View &&
            state.source.isNotBlank() &&
            state.renderError == null

    Scaffold(
        topBar = {
            DocumentTopBar(
                state = state,
                onPaste = ::paste,
                onCopyImage = viewModel::copyImage,
                onFit = viewModel.renderer::resetViewTransform,
                onSave = viewModel::save,
                onSaveAs = { saveAs.launch(state.fileName) },
                onOpen = {
                    openDocument.launch(
                        arrayOf(
                            "text/plain",
                            "text/markdown",
                            "text/*",
                            "*/*",
                        ),
                    )
                },
                onAppearance = viewModel::setAppearance,
                onDiagramTheme = viewModel::setDiagramTheme,
            )
        },
        bottomBar = {
            DocumentBottomBar(
                screen = state.screen,
                mode = state.mode,
                onFiles = viewModel::showFiles,
                onMode = viewModel::setMode,
                onNew = viewModel::newDiagram,
            )
        },
        floatingActionButton = {
            if (showLiveDiagram) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::copyImage,
                    icon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    text = { Text("Copy image") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .imePadding()
                .fillMaxSize(),
        ) {
            MermaidEngine(
                source = state.source,
                diagramTheme = state.diagramTheme,
                controller = viewModel.renderer,
                onRendered = viewModel::onRenderResult,
                modifier = Modifier
                    .fillMaxSize()
                    .background(paper)
                    .zIndex(if (showLiveDiagram) 1f else 0f)
                    .then(
                        if (showLiveDiagram) {
                            Modifier
                        } else {
                            Modifier.offset(x = (-5000).dp)
                        },
                    ),
            )
            if (state.screen == Screen.Files) {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    FilesPane(
                        files = state.files,
                        currentPath = state.currentFile?.canonicalPath,
                        onOpen = viewModel::openStored,
                        onDelete = viewModel::deleteStored,
                        onPaste = ::paste,
                        onOpenExternal = {
                            openDocument.launch(arrayOf("text/plain", "text/markdown", "text/*", "*/*"))
                        },
                        onNew = viewModel::newDiagram,
                    )
                }
            } else if (!showLiveDiagram) {
                DocumentPane(
                    state = state,
                    onSourceChange = viewModel::onSourceChange,
                    onPaste = ::paste,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                )
            }
            if (showLiveDiagram && state.isRendering) {
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .zIndex(3f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTopBar(
    state: EditorUiState,
    onPaste: () -> Unit,
    onCopyImage: () -> Unit,
    onFit: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onOpen: () -> Unit,
    onAppearance: (Appearance) -> Unit,
    onDiagramTheme: (DiagramTheme) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            Text(
                text = if (state.isDirty) "${state.fileName} ·" else state.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(onClick = onPaste) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste")
            }
            if (state.mode == EditorMode.View && state.screen == Screen.Document && state.source.isNotBlank()) {
                IconButton(onClick = onFit) {
                    Icon(Icons.Outlined.FitScreen, contentDescription = "Fit to screen")
                }
                if (state.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onCopyImage) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy image")
                    }
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Save .mmd") },
                        leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onSave()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Save as…") },
                        onClick = {
                            menuOpen = false
                            onSaveAs()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Open…") },
                        leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Diagram: ${state.diagramTheme.menuLabel}") },
                        leadingIcon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                        onClick = {
                            onDiagramTheme(state.diagramTheme.next())
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Chrome: ${state.appearance.name}") },
                        leadingIcon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                        onClick = {
                            val next = when (state.appearance) {
                                Appearance.System -> Appearance.Light
                                Appearance.Light -> Appearance.Dark
                                Appearance.Dark -> Appearance.System
                            }
                            onAppearance(next)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun DocumentBottomBar(
    screen: Screen,
    mode: EditorMode,
    onFiles: () -> Unit,
    onMode: (EditorMode) -> Unit,
    onNew: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = screen == Screen.Files,
            onClick = onFiles,
            icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = "Files") },
            label = { Text("Files") },
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 4.dp)) {
            listOf(EditorMode.Edit to "Edit", EditorMode.View to "View").forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = screen == Screen.Document && mode == value,
                    onClick = { onMode(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) {
                    Text(label)
                }
            }
        }
        NavigationBarItem(
            selected = false,
            onClick = onNew,
            icon = { Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = "New") },
            label = { Text("New") },
        )
    }
}

@Composable
private fun DocumentPane(
    state: EditorUiState,
    onSourceChange: (String) -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.mode,
        label = "edit-view",
        modifier = modifier,
    ) { mode ->
        if (mode == EditorMode.Edit) {
            SourceEditor(
                source = state.source,
                onSourceChange = onSourceChange,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ViewPane(
                state = state,
                onPaste = onPaste,
                modifier = Modifier.fillMaxSize(),
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
                .padding(20.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (source.isEmpty()) {
                        Text(
                            text = "flowchart TB\n    Start --> Stop",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
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
private fun ViewPane(
    state: EditorUiState,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paper = Color(state.diagramTheme.canvasArgb)
    Box(modifier) {
        when {
            state.source.isBlank() -> {
                Surface(Modifier.fillMaxSize(), color = paper) {
                    EmptyView(onPaste = onPaste, modifier = Modifier.fillMaxSize())
                }
            }
            state.renderError != null -> {
                Surface(Modifier.fillMaxSize(), color = paper) {
                    Text(
                        text = state.renderError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
            else -> {
                if (state.isRendering) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun EmptyView(
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Paste or share a Mermaid diagram.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Fenced markdown from chat is stripped automatically. You land in View.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )
        Button(onClick = onPaste, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Outlined.ContentPaste, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Paste")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesPane(
    files: List<StoredDiagram>,
    currentPath: String?,
    onOpen: (java.io.File) -> Unit,
    onDelete: (java.io.File) -> Unit,
    onPaste: () -> Unit,
    onOpenExternal: () -> Unit,
    onNew: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Saved .mmd",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
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
                            trailingContent = {
                                if (diagram.file.canonicalPath == currentPath) {
                                    Text(
                                        "Open",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(diagram.file) },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
