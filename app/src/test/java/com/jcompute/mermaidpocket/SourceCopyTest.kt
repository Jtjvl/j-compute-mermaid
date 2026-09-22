package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.ui.SourceCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceCopyTest {
    @Test
    fun payloadIsTheEntireBuffer() {
        val source = "flowchart TB\n    A --> B\n    B --> C"
        assertEquals(source, SourceCopy.payload(source))
        assertEquals(source, SourceCopy.payload(source)?.take(source.length))
        assertEquals(source.length, SourceCopy.payload(source)?.length)
    }

    @Test
    fun emptyBufferHasNothingToCopy() {
        assertNull(SourceCopy.payload(""))
    }

    @Test
    fun whitespaceIsStillSource() {
        assertEquals(" \n", SourceCopy.payload(" \n"))
    }
}
