package com.jcompute.mermaid.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jcompute.mermaid.MermaidApp
import com.jcompute.mermaid.data.Appearance
import com.jcompute.mermaid.data.AppSettings
import com.jcompute.mermaid.data.DiagramTheme
import com.jcompute.mermaid.data.DiagramStore
import com.jcompute.mermaid.data.MermaidIngest
import com.jcompute.mermaid.data.StoredDiagram
import com.jcompute.mermaid.data.TitleParser
import com.jcompute.mermaid.export.ExportWriter
import com.jcompute.mermaid.intent.IncomingContent
import com.jcompute.mermaid.render.MermaidRendererController
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorMode { Edit, View }

enum class Screen { Files, Document }

data class EditorUiState(
    val screen: Screen = Screen.Document,
    val currentFile: File? = null,
    val source: String = "",
    val savedSource: String = "",
    val title: String = "Untitled",
    val mode: EditorMode = EditorMode.View,
    val files: List<StoredDiagram> = emptyList(),
    val renderError: String? = null,
    val isRendering: Boolean = false,
    val previewPng: ByteArray? = null,
    val isExporting: Boolean = false,
    val appearance: Appearance = Appearance.System,
    val diagramTheme: DiagramTheme = DiagramTheme.Default,
    val renderEpoch: Long = 0,
    val selection: FileSelection = FileSelection(),
) {
    val isDirty: Boolean get() = source != savedSource
    val fileName: String get() = currentFile?.name ?: "Untitled.mmd"
}

sealed interface UiEvent {
    data class Message(val text: String) : UiEvent
}

class EditorViewModel(
    application: Application,
    private val store: DiagramStore,
    private val settings: AppSettings,
    private val exportWriter: ExportWriter = ExportWriter(application),
) : AndroidViewModel(application) {

    val renderer = MermaidRendererController()

    private val _state = MutableStateFlow(
        EditorUiState(
            appearance = settings.appearance(),
            diagramTheme = settings.diagramTheme(),
        ),
    )
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        refreshFiles()
    }

    fun onSourceChange(value: String) {
        _state.update {
            it.copy(
                source = value,
                title = displayTitle(value, it.currentFile?.name),
            )
        }
    }

    fun setMode(mode: EditorMode) {
        _state.update { it.copy(mode = mode, screen = Screen.Document, selection = FileSelection()) }
    }

    fun showFiles() {
        _state.update { state ->
            val files = store.list()
            val existing = files.map { it.file.canonicalPath }.toSet()
            state.copy(
                screen = Screen.Files,
                files = files,
                selection = state.selection.prune(existing),
            )
        }
    }

    fun showDocument() {
        _state.update { it.copy(screen = Screen.Document, selection = FileSelection()) }
    }

    fun setAppearance(value: Appearance) {
        settings.setAppearance(value)
        _state.update { it.copy(appearance = value) }
    }

    fun setDiagramTheme(value: DiagramTheme) {
        if (_state.value.diagramTheme == value) return
        settings.setDiagramTheme(value)
        _state.update {
            val pass = RenderPass.next(it.renderEpoch, it.source)
            it.copy(
                diagramTheme = value,
                previewPng = null,
                renderEpoch = pass.epoch,
                isRendering = pass.showSpinner && it.mode == EditorMode.View,
            )
        }
    }

    fun onRenderResult(ok: Boolean, error: String) {
        renderer.markRendered(ok, error)
        if (!ok) {
            _state.update {
                it.copy(
                    isRendering = false,
                    renderError = error.takeIf { message -> message.isNotBlank() },
                    previewPng = null,
                )
            }
            return
        }
        _state.update { it.copy(isRendering = false, renderError = null) }
        val source = _state.value.source
        if (source.isBlank()) {
            _state.update { it.copy(previewPng = null) }
            return
        }
        viewModelScope.launch {
            val background = _state.value.diagramTheme.canvasHex
            val density = getApplication<Application>().resources.displayMetrics.density
            val scale = DiagramDisplay.pngScale(density).toDouble()
            val bytes = runCatching { renderer.exportPng(scale = scale, background = background) }.getOrNull()
            _state.update { it.copy(previewPng = bytes) }
            val file = _state.value.currentFile
            if (file != null && bytes != null) {
                withContext(Dispatchers.IO) { store.writeThumbnail(file, bytes) }
                refreshFiles()
            }
        }
    }

    fun newDiagram() {
        _state.update {
            val pass = RenderPass.next(it.renderEpoch, "")
            it.copy(
                screen = Screen.Document,
                currentFile = null,
                source = "",
                savedSource = "",
                title = "Untitled",
                mode = EditorMode.Edit,
                renderError = null,
                previewPng = null,
                renderEpoch = pass.epoch,
                isRendering = false,
                selection = FileSelection(),
            )
        }
    }

    fun ingest(text: String, fileName: String? = null) {
        val source = MermaidIngest.extractSource(text)
        _state.update {
            val pass = RenderPass.next(it.renderEpoch, source)
            it.copy(
                screen = Screen.Document,
                source = source,
                title = displayTitle(source, fileName),
                mode = EditorMode.View,
                renderEpoch = pass.epoch,
                isRendering = pass.showSpinner,
                renderError = null,
                previewPng = null,
                selection = FileSelection(),
            )
        }
        if (source.isBlank()) {
            notify("Nothing to paste.")
        }
    }

    fun pasteFromClipboard(text: String?) {
        val clipped = text.orEmpty()
        if (clipped.isBlank()) {
            notify("Clipboard is empty.")
            return
        }
        ingest(clipped, fileName = null)
    }

    fun handleIntent(intent: Intent) {
        if (!IncomingContent.shouldHandle(intent)) return
        IncomingContent.markHandled(intent)
        viewModelScope.launch {
            val document = runCatching {
                IncomingContent.read(intent, getApplication<Application>().contentResolver)
            }.getOrElse { error ->
                notify(error.message ?: "Could not open that content.")
                return@launch
            } ?: return@launch
            ingest(document.text, fileName = document.displayName)
        }
    }

    fun openStored(file: File) {
        viewModelScope.launch {
            val source = withContext(Dispatchers.IO) { store.read(file) }
            _state.update {
                val pass = RenderPass.next(it.renderEpoch, source)
                it.copy(
                    screen = Screen.Document,
                    currentFile = file,
                    source = source,
                    savedSource = source,
                    title = displayTitle(source, file.name),
                    mode = EditorMode.View,
                    renderEpoch = pass.epoch,
                    isRendering = pass.showSpinner,
                    previewPng = null,
                    renderError = null,
                    selection = FileSelection(),
                )
            }
        }
    }

    fun openImported(text: String, fileName: String?) {
        ingest(text, fileName)
    }

    fun enterSelection(file: File) {
        _state.update { it.copy(selection = it.selection.enter(file.canonicalPath)) }
    }

    fun toggleSelected(file: File) {
        _state.update { it.copy(selection = it.selection.toggle(file.canonicalPath)) }
    }

    fun cancelSelection() {
        _state.update { it.copy(selection = FileSelection()) }
    }

    fun deleteStored(file: File) {
        val removedCurrent = _state.value.currentFile?.canonicalPath == file.canonicalPath
        store.delete(file)
        afterDeletes(removedCurrent, deletedCount = 1)
    }

    fun deleteSelected() {
        val snapshot = _state.value
        val toDelete = snapshot.files
            .filter { snapshot.selection.contains(it.file.canonicalPath) }
            .map { it.file }
        if (toDelete.isEmpty()) {
            cancelSelection()
            return
        }
        val removedCurrent = toDelete.any { it.canonicalPath == snapshot.currentFile?.canonicalPath }
        val count = store.deleteAll(toDelete)
        afterDeletes(removedCurrent, deletedCount = count)
    }

    fun save() {
        viewModelScope.launch {
            val snapshot = _state.value
            val file = withContext(Dispatchers.IO) {
                val target = snapshot.currentFile ?: store.uniqueFile(TitleParser.fileSlug(snapshot.title))
                store.write(target, snapshot.source)
                snapshot.previewPng?.let { store.writeThumbnail(target, it) }
                target
            }
            _state.update {
                it.copy(
                    currentFile = file,
                    savedSource = snapshot.source,
                    title = displayTitle(snapshot.source, file.name),
                )
            }
            refreshFiles()
            notify("Saved ${file.name}")
        }
    }

    fun saveToUri(uri: Uri) {
        viewModelScope.launch {
            val source = _state.value.source
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(source.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not write that file.")
                }
            }.onSuccess {
                notify("Exported ${IncomingContent.displayName(getApplication<Application>().contentResolver, uri) ?: "diagram.mmd"}")
            }.onFailure { error ->
                notify(error.message ?: "Could not export.")
            }
        }
    }

    fun copyImage() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            runCatching {
                val background = _state.value.diagramTheme.canvasHex
                val density = getApplication<Application>().resources.displayMetrics.density
                val scale = DiagramDisplay.pngScale(density).toDouble()
                val bytes = _state.value.previewPng
                    ?: renderer.exportPng(scale = scale, background = background)
                val uri = exportWriter.writeClipboardPng(bytes)
                val context = getApplication<Application>()
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                val clip = ClipData("image/png", arrayOf("image/png"), ClipData.Item(uri))
                clipboard.setPrimaryClip(clip)
                notify("Image copied")
            }.onFailure { error ->
                notify(error.message ?: "Could not copy image. Render a diagram first.")
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    fun notify(message: String) {
        _events.tryEmit(UiEvent.Message(message))
    }

    private fun afterDeletes(removedCurrent: Boolean, deletedCount: Int) {
        _state.update { state ->
            val cleared = if (removedCurrent) {
                val pass = RenderPass.next(state.renderEpoch, "")
                state.copy(
                    currentFile = null,
                    source = "",
                    savedSource = "",
                    title = "Untitled",
                    mode = EditorMode.Edit,
                    renderError = null,
                    previewPng = null,
                    renderEpoch = pass.epoch,
                    isRendering = false,
                )
            } else {
                state
            }
            cleared.copy(
                screen = Screen.Files,
                files = store.list(),
                selection = FileSelection(),
            )
        }
        if (deletedCount <= 0) return
        notify(if (deletedCount == 1) "Deleted 1 diagram" else "Deleted $deletedCount diagrams")
    }

    private fun refreshFiles() {
        _state.update { state ->
            val files = store.list()
            val existing = files.map { it.file.canonicalPath }.toSet()
            state.copy(files = files, selection = state.selection.prune(existing))
        }
    }

    private fun displayTitle(source: String, fileName: String?): String {
        return TitleParser.fromSource(source, fileName = fileName, fallback = "Untitled")
    }

    class Factory(private val app: MermaidApp) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(app, app.store, app.settings) as T
        }
    }
}
