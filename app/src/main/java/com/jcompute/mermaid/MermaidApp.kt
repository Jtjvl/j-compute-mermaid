package com.jcompute.mermaid

import android.app.Application
import com.jcompute.mermaid.data.DiagramRepository

class MermaidApp : Application() {
    lateinit var repository: DiagramRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DiagramRepository(this)
    }
}
