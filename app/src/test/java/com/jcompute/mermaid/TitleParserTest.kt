package com.jcompute.mermaid

import com.jcompute.mermaid.data.TitleParser
import org.junit.Assert.assertEquals
import org.junit.Test

class TitleParserTest {
    @Test
    fun prefersCommentTitle() {
        val source = """
            %% Checkout
            flowchart TD
                A --> B
        """.trimIndent()
        assertEquals("Checkout", TitleParser.fromSource(source, fileName = "ignored.mmd"))
    }

    @Test
    fun usesFileNameWhenNoComment() {
        val source = "flowchart TD\n    A --> B"
        assertEquals("order-flow", TitleParser.fromSource(source, fileName = "order-flow.mmd"))
    }

    @Test
    fun fallsBackToFirstLine() {
        val source = "sequenceDiagram\n    A->>B: hi"
        assertEquals("sequenceDiagram", TitleParser.fromSource(source))
    }

    @Test
    fun slugsExportNames() {
        assertEquals("checkout-flow", TitleParser.fileSlug("Checkout flow"))
        assertEquals("diagram", TitleParser.fileSlug("***"))
    }
}
