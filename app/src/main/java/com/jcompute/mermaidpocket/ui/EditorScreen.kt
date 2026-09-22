package com.jcompute.mermaidpocket.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jcompute.mermaidpocket.R
import com.jcompute.mermaidpocket.data.Appearance
import com.jcompute.mermaidpocket.data.DiagramTheme
import com.jcompute.mermaidpocket.export.ExportDelivery
import com.jcompute.mermaidpocket.export.ExportFormat
import com.jcompute.mermaidpocket.export.ExportNames
import com.jcompute.mermaidpocket.intent.IncomingContent
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MermaidAppRoot(
    viewModel: EditorViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val paper = Color(state.diagramTheme.canvasArgb)
    var confirmBulkDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.selection.active) {
        if (!state.selection.active) confirmBulkDelete = false
    }

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

    var pendingRaster by remember { mutableStateOf<Pair<ExportFormat, ExportDelivery>?>(null) }
    var pendingRasterWidth by remember { mutableStateOf<Int?>(null) }

    val saveAs = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveExport(ExportFormat.Mmd, uri, widthPx = null)
    }
    val savePng = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveExport(ExportFormat.Png, uri, pendingRasterWidth)
    }
    val saveJpeg = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveExport(ExportFormat.Jpeg, uri, pendingRasterWidth)
    }
    val saveSvg = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/svg+xml"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveExport(ExportFormat.Svg, uri, widthPx = null)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbar.showSnackbar(event.text)
                is UiEvent.ShareFile -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = event.mime
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        clipData = ClipData.newUri(context.contentResolver, event.title, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(share, event.title))
                    }.onFailure {
                        viewModel.notify("Could not open the share sheet.")
                    }
                }
            }
        }
    }

    fun beginExport(format: ExportFormat, delivery: ExportDelivery) {
        if (format.sized) {
            pendingRaster = format to delivery
            return
        }
        if (delivery == ExportDelivery.Share) {
            viewModel.shareExport(format, widthPx = null)
            return
        }
        val name = ExportNames.fileName(state.fileName, format)
        when (format) {
            ExportFormat.Svg -> saveSvg.launch(name)
            ExportFormat.Mmd -> saveAs.launch(name)
            ExportFormat.Png, ExportFormat.Jpeg -> Unit
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
    val hideChrome =
        state.immersiveView &&
            state.screen == Screen.Document &&
            state.mode == EditorMode.View

    BackHandler(enabled = hideChrome) {
        viewModel.exitImmersiveView()
    }

    Scaffold(
        contentWindowInsets = if (hideChrome) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (!hideChrome) {
                if (state.screen == Screen.Files) {
                    FilesTopBar(
                        selection = state.selection,
                        onCancelSelection = viewModel::cancelSelection,
                        onDeleteSelected = { confirmBulkDelete = true },
                    )
                } else {
                    DocumentTopBar(
                        state = state,
                        onPaste = ::paste,
                        onCopyAll = viewModel::copyAllSource,
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                        onCopyImage = viewModel::copyImage,
                        onFit = viewModel.renderer::resetViewTransform,
                        onSave = viewModel::save,
                        onSaveAs = { beginExport(ExportFormat.Mmd, ExportDelivery.Save) },
                        onExport = ::beginExport,
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
                }
            }
        },
        bottomBar = {
            if (hideChrome) {
                ImmersiveViewBar(
                    mode = state.mode,
                    onMode = viewModel::setMode,
                )
            } else {
                DocumentBottomBar(
                    screen = state.screen,
                    mode = state.mode,
                    onFiles = viewModel::showFiles,
                    onMode = viewModel::setMode,
                    onNew = viewModel::newDiagram,
                )
            }
        },
        floatingActionButton = {
            if (showLiveDiagram && !hideChrome) {
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
                renderEpoch = state.renderEpoch,
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
                        selection = state.selection,
                        onOpen = viewModel::openStored,
                        onDelete = viewModel::deleteStored,
                        onEnterSelection = viewModel::enterSelection,
                        onToggleSelected = viewModel::toggleSelected,
                        onCancelSelection = viewModel::cancelSelection,
                        onPaste = ::paste,
                        onOpenExternal = {
                            openDocument.launch(arrayOf("text/plain", "text/markdown", "text/*", "*/*"))
                        },
                        onNew = viewModel::newDiagram,
                        onShare = viewModel::shareStored,
                    )
                }
            } else if (!showLiveDiagram) {
                DocumentPane(
                    state = state,
                    onSourceChange = viewModel::onSourceChange,
                    onPaste = ::paste,
                    onCopyAll = viewModel::copyAllSource,
                    onUndo = viewModel::undo,
                    onRedo = viewModel::redo,
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

    pendingRaster?.let { (format, delivery) ->
        ExportWidthDialog(
            format = format,
            initialWidth = viewModel.exportWidth(),
            sourceWidth = state.diagramWidth,
            sourceHeight = state.diagramHeight,
            onConfirm = { px ->
                pendingRasterWidth = px
                viewModel.stageExportWidth(px)
                pendingRaster = null
                if (delivery == ExportDelivery.Share) {
                    viewModel.shareExport(format, px)
                } else {
                    val name = ExportNames.fileName(state.fileName, format)
                    when (format) {
                        ExportFormat.Png -> savePng.launch(name)
                        ExportFormat.Jpeg -> saveJpeg.launch(name)
                        else -> Unit
                    }
                }
            },
            onDismiss = { pendingRaster = null },
        )
    }

    if (confirmBulkDelete && state.selection.active) {
        BulkDeleteDialog(
            count = state.selection.count,
            onConfirm = {
                confirmBulkDelete = false
                viewModel.deleteSelected()
            },
            onDismiss = { confirmBulkDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTopBar(
    state: EditorUiState,
    onPaste: () -> Unit,
    onCopyAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopyImage: () -> Unit,
    onFit: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onExport: (ExportFormat, ExportDelivery) -> Unit,
    onOpen: () -> Unit,
    onAppearance: (Appearance) -> Unit,
    onDiagramTheme: (DiagramTheme) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    val editing = state.mode == EditorMode.Edit && state.screen == Screen.Document
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
            if (editing) {
                IconButton(onClick = onCopyAll) {
                    Icon(Icons.Outlined.CopyAll, contentDescription = "Copy all")
                }
                IconButton(onClick = onUndo, enabled = state.canUndo) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = onRedo, enabled = state.canRedo) {
                    Icon(Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo")
                }
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
                        text = { Text("Save .mmd as…") },
                        onClick = {
                            menuOpen = false
                            onSaveAs()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share .mmd") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Mmd, ExportDelivery.Share)
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Share PNG") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Png, ExportDelivery.Share)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Save PNG…") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Png, ExportDelivery.Save)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share JPEG") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Jpeg, ExportDelivery.Share)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Save JPEG…") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Jpeg, ExportDelivery.Save)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share SVG") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Svg, ExportDelivery.Share)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Save SVG…") },
                        onClick = {
                            menuOpen = false
                            onExport(ExportFormat.Svg, ExportDelivery.Save)
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
                        text = { Text(stringResource(R.string.about)) },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            aboutOpen = true
                        },
                    )
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
    if (aboutOpen) {
        AboutDialog(onDismiss = { aboutOpen = false })
    }
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
private fun ImmersiveViewBar(
    mode: EditorMode,
    onMode: (EditorMode) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            SingleChoiceSegmentedButtonRow {
                listOf(EditorMode.Edit to "Edit", EditorMode.View to "View").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = mode == value,
                        onClick = { onMode(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentPane(
    state: EditorUiState,
    onSourceChange: (String) -> Unit,
    onPaste: () -> Unit,
    onCopyAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.mode,
        label = "edit-view",
        modifier = modifier,
    ) { mode ->
        if (mode == EditorMode.Edit) {
            Column(Modifier.fillMaxSize()) {
                EditHistoryStrip(
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo,
                )
                SourceEditor(
                    source = state.source,
                    onSourceChange = onSourceChange,
                    onCopyAll = onCopyAll,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
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
    onCopyAll: () -> Unit,
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
                .copyAllOnLongPress(onCopyAll)
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

@Composable
private fun EditHistoryStrip(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .semantics {
                contentDescription = "Edit history. Swipe right to undo, left to redo."
            }
            .pointerInput(canUndo, canRedo, threshold) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, amount ->
                        drag += amount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            drag >= threshold && canUndo -> onUndo()
                            drag <= -threshold && canRedo -> onRedo()
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Swipe right to undo · left to redo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_name)) },
        text = { Text(stringResource(R.string.about_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        },
    )
}

private fun Modifier.copyAllOnLongPress(onCopyAll: () -> Unit): Modifier = pointerInput(onCopyAll) {
    val slop = viewConfiguration.touchSlop
    val timeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
        val origin = down.position
        val released = withTimeoutOrNull(timeout) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                if (!change.pressed) return@withTimeoutOrNull true
                val delta = change.position - origin
                val distance = kotlin.math.hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                if (distance > slop) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE")
            true
        }
        if (released != null) return@awaitEachGesture
        onCopyAll()
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            change?.consume()
            if (change == null || !change.pressed) break
        }
    }
}
