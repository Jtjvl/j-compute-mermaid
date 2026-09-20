package com.jcompute.mermaid.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class DiagramRepository(context: Context) {
    private val storeFile = File(context.filesDir, "diagrams.json")
    private val prefs = context.getSharedPreferences("mermaid", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun loadAll(): List<Diagram> = withContext(Dispatchers.IO) {
        mutex.withLock { readLocked().diagrams.sortedByDescending { it.updatedAtMillis } }
    }

    suspend fun get(id: String): Diagram? = loadAll().firstOrNull { it.id == id }

    suspend fun upsert(diagram: Diagram): Diagram = withContext(Dispatchers.IO) {
        mutex.withLock {
            val store = readLocked()
            val next = store.diagrams.filterNot { it.id == diagram.id } + diagram
            writeLocked(store.copy(diagrams = next))
            prefs.edit().putString(KEY_LAST_ID, diagram.id).apply()
            diagram
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val store = readLocked()
            writeLocked(store.copy(diagrams = store.diagrams.filterNot { it.id == id }))
            if (prefs.getString(KEY_LAST_ID, null) == id) {
                val fallback = store.diagrams
                    .filterNot { it.id == id }
                    .maxByOrNull { it.updatedAtMillis }
                    ?.id
                prefs.edit().putString(KEY_LAST_ID, fallback).apply()
            }
        }
    }

    fun lastOpenedId(): String? = prefs.getString(KEY_LAST_ID, null)

    fun rememberLastOpened(id: String) {
        prefs.edit().putString(KEY_LAST_ID, id).apply()
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun readLocked(): Store {
        if (!storeFile.exists()) return Store()
        return runCatching {
            json.decodeFromString(Store.serializer(), storeFile.readText())
        }.getOrElse { Store() }
    }

    private fun writeLocked(store: Store) {
        storeFile.parentFile?.mkdirs()
        storeFile.writeText(json.encodeToString(store))
    }

    @Serializable
    private data class Store(
        val diagrams: List<Diagram> = emptyList(),
    )

    private companion object {
        const val KEY_LAST_ID = "last_opened_id"
    }
}
