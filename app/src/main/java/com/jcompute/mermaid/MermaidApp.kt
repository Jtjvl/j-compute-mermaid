package com.jcompute.mermaid

import android.app.Application
import com.jcompute.mermaid.data.AppSettings
import com.jcompute.mermaid.data.DiagramStore

class MermaidApp : Application() {
    lateinit var store: DiagramStore
        private set
    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        store = DiagramStore(filesDir)
        settings = AppSettings(this)
    }
}
