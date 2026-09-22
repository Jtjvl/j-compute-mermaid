package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.ui.SourceHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceHistoryTest {
    @Test
    fun tinyKeystrokesInsideTheWindowAreOneUndoStep() {
        val history = SourceHistory(coalesceWindowMillis = 1_000)
        history.reset("")
        history.record("a", nowMillis = 0)
        history.record("ab", nowMillis = 100)
        history.record("abc", nowMillis = 200)

        assertEquals("abc", history.text)
        assertEquals("", history.undo())
        assertFalse(history.canUndo)
    }

    @Test
    fun keystrokesOutsideTheWindowAreSeparateSteps() {
        val history = SourceHistory(coalesceWindowMillis = 1_000)
        history.reset("")
        history.record("a", nowMillis = 0)
        history.record("ab", nowMillis = 5_000)

        assertEquals("a", history.undo())
        assertEquals("", history.undo())
    }

    @Test
    fun newlineBreaksTheCoalesceGroup() {
        val history = SourceHistory(coalesceWindowMillis = 1_000)
        history.reset("a")
        history.record("a\n", nowMillis = 10)
        history.record("a\nb", nowMillis = 20)

        assertEquals("a\n", history.undo())
        assertEquals("a", history.undo())
        assertFalse(history.canUndo)
    }

    @Test
    fun aPasteIsItsOwnCheckpoint() {
        val history = SourceHistory()
        history.reset("hi")
        history.record("hi\n" + "x".repeat(40), nowMillis = 10)

        assertEquals("hi", history.undo())
        assertFalse(history.canUndo)
    }

    @Test
    fun backspaceInsideTheWindowStaysInTheSameStep() {
        val history = SourceHistory(coalesceWindowMillis = 1_000)
        history.reset("")
        history.record("a", nowMillis = 0)
        history.record("ab", nowMillis = 10)
        history.record("a", nowMillis = 20)

        assertEquals("", history.undo())
    }

    @Test
    fun undoAndRedoRoundTripAndANewEditClearsRedo() {
        val history = SourceHistory(coalesceWindowMillis = 0)
        history.reset("a")
        history.checkpoint("b", nowMillis = 1)
        assertEquals("a", history.undo())
        assertTrue(history.canRedo)
        history.record("c", nowMillis = 2)
        assertFalse(history.canRedo)
        assertEquals("c", history.text)
        assertEquals("a", history.undo())
    }

    @Test
    fun redoRestoresTheLatestText() {
        val history = SourceHistory()
        history.reset("one")
        history.checkpoint("two", nowMillis = 1)
        history.undo()
        assertEquals("two", history.redo())
        assertEquals("two", history.text)
    }

    @Test
    fun checkpointDoesNotCoalesce() {
        val history = SourceHistory(coalesceWindowMillis = 10_000)
        history.reset("a")
        history.checkpoint("ab", nowMillis = 10)
        history.checkpoint("abc", nowMillis = 20)

        assertEquals("ab", history.undo())
        assertEquals("a", history.undo())
    }

    @Test
    fun identicalTextDoesNotPush() {
        val history = SourceHistory()
        history.reset("same")
        history.record("same", nowMillis = 5)
        history.checkpoint("same", nowMillis = 6)
        assertFalse(history.canUndo)
        assertNull(history.undo())
    }

    @Test
    fun entryCapDropsTheOldestCheckpoint() {
        val history = SourceHistory(maxEntries = 3, maxChars = 1_000_000, coalesceWindowMillis = 0)
        history.reset("0")
        repeat(10) { index ->
            history.checkpoint((index + 1).toString(), nowMillis = index.toLong())
        }
        var steps = 0
        while (history.canUndo) {
            history.undo()
            steps++
        }
        assertEquals(3, steps)
    }

    @Test
    fun charCapDropsOldSnapshotsButKeepsTheLatestText() {
        val history = SourceHistory(maxEntries = 50, maxChars = 1_000, coalesceWindowMillis = 0)
        history.reset("x")
        val latest = "y".repeat(200) + "END"
        repeat(20) { index ->
            history.checkpoint("y".repeat(200) + index, nowMillis = index.toLong())
        }
        history.checkpoint(latest, nowMillis = 100)
        assertEquals(latest, history.text)
        assertTrue(history.canUndo)
        history.undo()
        assertNotEquals(latest, history.text)
        assertEquals(latest, history.redo())
        var steps = 0
        while (history.canUndo) {
            history.undo()
            steps++
        }
        assertTrue(steps < 20)
    }

    @Test
    fun resetDropsThePreviousDocument() {
        val history = SourceHistory()
        history.reset("old")
        history.checkpoint("edited", nowMillis = 1)
        history.reset("fresh")
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals("fresh", history.text)
    }

    @Test
    fun singleCharacterReplaceIsTinyAndANewlineIsNot() {
        assertTrue(SourceHistory.isTinyKeystroke("cat", "car"))
        assertTrue(SourceHistory.isTinyKeystroke("cat", "ca"))
        assertFalse(SourceHistory.isTinyKeystroke("cat", "cat\n"))
        assertFalse(SourceHistory.isTinyKeystroke("a", "abc"))
    }
}
