package com.jcompute.mermaidpocket

import android.app.Application
import com.jcompute.mermaidpocket.data.AppSettings
import com.jcompute.mermaidpocket.data.DiagramStore
import com.jcompute.mermaidpocket.data.LegacyDiagramMigration

class MermaidApp : Application() {
    lateinit var store: DiagramStore
        private set
    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        store = DiagramStore(filesDir)
        LegacyDiagramMigration.migrate(filesDir, store)
        settings = AppSettings(this)
    }
}
