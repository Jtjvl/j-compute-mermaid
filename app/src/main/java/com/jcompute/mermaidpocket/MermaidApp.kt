package com.jcompute.mermaidpocket

import android.app.Application
import com.jcompute.mermaidpocket.data.AppSettings
import com.jcompute.mermaidpocket.data.DiagramStore
import com.jcompute.mermaidpocket.ai.AiSecrets
import com.jcompute.mermaidpocket.data.DiagramSync
import com.jcompute.mermaidpocket.data.DurableDocuments
import com.jcompute.mermaidpocket.data.LegacyDiagramMigration

class MermaidApp : Application() {
    lateinit var store: DiagramStore
        private set
    lateinit var settings: AppSettings
        private set
    lateinit var aiSecrets: AiSecrets
        private set
    lateinit var durable: DurableDocuments
        private set
    lateinit var sync: DiagramSync
        private set

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        aiSecrets = AiSecrets(this)
        store = DiagramStore(filesDir)
        durable = DurableDocuments(this, settings)
        sync = DiagramSync(store) { durable.remote() }
        LegacyDiagramMigration.migrate(filesDir, store)
        if (durable.isReady()) {
            sync.reconcile()
        }
    }
}
