package com.jcompute.mermaid

import com.jcompute.mermaid.data.MermaidIngest
import org.junit.Assert.assertEquals
import org.junit.Test

class MermaidIngestTest {
    @Test
    fun extractsTaggedMermaidFence() {
        val raw = """
            Notes from the agent

            ```mermaid
            flowchart TD
                A --> B
            ```

            extra
        """.trimIndent()
        assertEquals("flowchart TD\n    A --> B", MermaidIngest.extractSource(raw))
    }

    @Test
    fun unwrapsWholeDocumentFence() {
        val raw = """
            ```
            sequenceDiagram
                A->>B: hi
            ```
        """.trimIndent()
        assertEquals("sequenceDiagram\n    A->>B: hi", MermaidIngest.extractSource(raw))
    }

    @Test
    fun leavesRawSourceAlone() {
        val raw = "flowchart TB\n    Start --> Stop"
        assertEquals(raw, MermaidIngest.extractSource(raw))
    }

    @Test
    fun prefersMermaidFenceOverOtherFences() {
        val raw = """
            # Spec

            ```json
            {"ok": true}
            ```

            ```mermaid
            classDiagram
                Foo <|-- Bar
            ```
        """.trimIndent()
        assertEquals("classDiagram\n    Foo <|-- Bar", MermaidIngest.extractSource(raw))
    }
}
