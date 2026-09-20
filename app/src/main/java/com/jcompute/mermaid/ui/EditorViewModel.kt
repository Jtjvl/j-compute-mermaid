package com.jcompute.mermaid.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jcompute.mermaid.MermaidApp
import com.jcompute.mermaid.data.Diagram
import com.jcompute.mermaid.data.DiagramRepository
import com.jcompute.mermaid.data.Samples
import com.jcompute.mermaid.data.TitleParser
import com.jcompute.mermaid.export.ExportFormat
import com.jcompute.mermaid.export.ExportWriter
import com.jcompute.mermaid.intent.IncomingContent
import com.jcompute.mermaid.render.MermaidRendererController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorMode { Edit, View }

data class EditorUiState(
    val currentId: String = "",
    val source: String = "",
    val title: String = "Untitled diagram",
    val mode: EditorMode = EditorMode.View,
    val recents: List<Diagram> = emptyList(),
    val renderError: String? = null,
    val isRendering: Boolean = false,
    val recentsOpen: Boolean = false,
    val exportOpen: Boolean = false,
    val samplesOpen: Boolean = false,
    val isExporting: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface UiEvent {
    data class ShareFile(val uri: Uri, val mimeType: String, val title: String) : UiEvent
    data class ShareText(val text: String, val title: String) : UiEvent
    data class Message(val text: String) : UiEvent
}

class EditorViewModel(
    application: Application,
    private val repository: DiagramRepository,
    private val exportWriter: ExportWriter = ExportWriter(application),
) : AndroidViewModel(application) {

    val renderer = MermaidRendererController()

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var saveJob: Job? = null
    private var seeded = false

    init {
        viewModelScope.launch { restoreSession() }
    }

    fun onSourceChange(value: String) {
        _state.update {
            it.copy(
                source = value,
                title = TitleParser.fromSource(value, fileName = currentOriginName()),
            )
        }
        scheduleSave()
    }

    fun setMode(mode: EditorMode) {
        _state.update { it.copy(mode = mode) }
    }

    fun toggleMode() {
        setMode(if (_state.value.mode == EditorMode.Edit) EditorMode.View else EditorMode.Edit)
    }

    fun setRecentsOpen(open: Boolean) = _state.update { it.copy(recentsOpen = open) }
    fun setExportOpen(open: Boolean) = _state.update { it.copy(exportOpen = open) }
    fun setSamplesOpen(open: Boolean) = _state.update { it.copy(samplesOpen = open) }

    fun onRenderResult(ok: Boolean, error: String) {
        renderer.markRendered(ok, error)
        _state.update {
            it.copy(
                isRendering = false,
                renderError = error.takeIf { message -> !ok && message.isNotBlank() },
            )
        }
    }

    fun newDiagram(sampleSource: String = Samples.defaultSource) {
        viewModelScope.launch {
            persistCurrent()
            openNew(sampleSource, originName = null, mode = EditorMode.Edit)
            _state.update { it.copy(recentsOpen = false, samplesOpen = false) }
            _events.tryEmit(UiEvent.Message("New diagram"))
        }
    }

    fun openRecent(id: String) {
        viewModelScope.launch {
            persistCurrent()
            val diagram = repository.get(id) ?: return@launch
            applyDiagram(diagram, EditorMode.View)
            repository.rememberLastOpened(diagram.id)
            _state.update { it.copy(recentsOpen = false) }
        }
    }

    fun deleteRecent(id: String) {
        viewModelScope.launch {
            repository.delete(id)
            refreshRecents()
            if (_state.value.currentId == id) {
                val next = repository.loadAll().firstOrNull()
                if (next != null) {
                    applyDiagram(next, _state.value.mode)
                } else {
                    openNew(Samples.defaultSource, originName = null, mode = EditorMode.View)
                }
            }
        }
    }

    fun applySample(source: String) {
        onSourceChange(source)
        setMode(EditorMode.View)
        _state.update { it.copy(samplesOpen = false) }
    }

    fun importText(text: String, fileName: String?, preferEdit: Boolean) {
        viewModelScope.launch {
            persistCurrent()
            openNew(text, originName = fileName, mode = if (preferEdit) EditorMode.Edit else EditorMode.View)
            _events.tryEmit(UiEvent.Message("Opened ${fileName ?: "shared text"}"))
        }
    }

    fun handleIntent(intent: Intent) {
        if (!IncomingContent.shouldHandle(intent)) return
        IncomingContent.markHandled(intent)
        viewModelScope.launch {
            val document = runCatching {
                IncomingContent.read(intent, getApplication<Application>().contentResolver)
            }.getOrElse { error ->
                _events.tryEmit(UiEvent.Message(error.message ?: "Could not open that content."))
                return@launch
            } ?: return@launch
            importText(
                text = document.text,
                fileName = document.displayName,
                preferEdit = intent.action == Intent.ACTION_SEND,
            )
        }
    }

    fun notify(message: String) {
        _events.tryEmit(UiEvent.Message(message))
    }

    fun pasteFromClipboard(text: String?) {
        val clipped = text?.trim().orEmpty()
        if (clipped.isEmpty()) {
            _events.tryEmit(UiEvent.Message("Clipboard is empty."))
            return
        }
        importText(clipped, fileName = "Pasted text", preferEdit = true)
    }

    fun export(format: ExportFormat, dark: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportOpen = false) }
            runCatching {
                when (format) {
                    ExportFormat.MMD, ExportFormat.TEXT -> {
                        val bytes = _state.value.source.toByteArray(Charsets.UTF_8)
                        val uri = exportWriter.write(format, _state.value.title, bytes)
                        _events.emit(UiEvent.ShareFile(uri, format.mimeType, "${_state.value.title}.${format.extension}"))
                    }
                    ExportFormat.SVG -> {
                        val bytes = renderer.exportSvg()
                        val uri = exportWriter.write(format, _state.value.title, bytes)
                        _events.emit(UiEvent.ShareFile(uri, format.mimeType, "${_state.value.title}.svg"))
                    }
                    ExportFormat.PNG -> {
                        val background = if (dark) "#111113" else "#FFFFFF"
                        val bytes = renderer.exportPng(background = background)
                        val uri = exportWriter.write(format, _state.value.title, bytes)
                        _events.emit(UiEvent.ShareFile(uri, format.mimeType, "${_state.value.title}.png"))
                    }
                }
            }.onFailure { error ->
                _events.emit(UiEvent.Message(error.message ?: "Export failed."))
            }
            _state.update { it.copy(isExporting = false) }
        }
    }

    private suspend fun restoreSession() {
        val recents = repository.loadAll()
        val last = repository.lastOpenedId()?.let { id -> recents.firstOrNull { it.id == id } }
            ?: recents.firstOrNull()
        if (last != null) {
            applyDiagram(last, EditorMode.View)
        } else {
            openNew(Samples.defaultSource, originName = null, mode = EditorMode.View)
        }
        _state.update { it.copy(recents = recents, isLoading = false) }
        seeded = true
    }

    private suspend fun openNew(source: String, originName: String?, mode: EditorMode) {
        val now = System.currentTimeMillis()
        val diagram = Diagram(
            id = repository.newId(),
            title = TitleParser.fromSource(source, fileName = originName),
            source = source,
            updatedAtMillis = now,
            originName = originName,
        )
        repository.upsert(diagram)
        applyDiagram(diagram, mode)
        refreshRecents()
    }

    private fun applyDiagram(diagram: Diagram, mode: EditorMode) {
        _state.update {
            it.copy(
                currentId = diagram.id,
                source = diagram.source,
                title = diagram.title,
                mode = mode,
                isLoading = false,
                renderError = null,
                isRendering = diagram.source.isNotBlank(),
            )
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            persistCurrent()
            refreshRecents()
        }
    }

    private suspend fun persistCurrent() {
        val snapshot = _state.value
        if (!seeded || snapshot.currentId.isBlank()) return
        repository.upsert(
            Diagram(
                id = snapshot.currentId,
                title = TitleParser.fromSource(snapshot.source, fileName = currentOriginName()),
                source = snapshot.source,
                updatedAtMillis = System.currentTimeMillis(),
                originName = currentOriginName(),
            ),
        )
    }

    private suspend fun refreshRecents() {
        _state.update { it.copy(recents = repository.loadAll()) }
    }

    private fun currentOriginName(): String? {
        return _state.value.let { state ->
            state.recents.firstOrNull { it.id == state.currentId }?.originName
        }
    }

    class Factory(private val app: MermaidApp) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EditorViewModel(app, app.repository) as T
        }
    }
}
