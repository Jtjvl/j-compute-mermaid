package com.jcompute.mermaidpocket.ui

/**
 * Undo/redo of full source strings for the Edit buffer.
 *
 * Tiny keystrokes inside [coalesceWindowMillis] stay on the current entry so a
 * word is one step. A newline or a larger paste is its own checkpoint.
 * The stack is capped by entry count and by total retained characters.
 */
class SourceHistory(
    private val maxEntries: Int = 40,
    private val maxChars: Int = 750_000,
    private val coalesceWindowMillis: Long = 800L,
) {
    private val undo = ArrayDeque<String>()
    private val redo = ArrayDeque<String>()
    private var current: String = ""
    private var lastEditAt: Long = Long.MIN_VALUE
    private var grouping: Boolean = false

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val text: String get() = current

    fun reset(source: String) {
        undo.clear()
        redo.clear()
        current = source
        grouping = false
        lastEditAt = Long.MIN_VALUE
    }

    fun record(next: String, nowMillis: Long) {
        if (next == current) return
        val tiny = isTinyKeystroke(current, next)
        val within = grouping && nowMillis - lastEditAt <= coalesceWindowMillis
        if (tiny && within) {
            current = next
            lastEditAt = nowMillis
            redo.clear()
            trim()
            return
        }
        pushUndo(current)
        current = next
        redo.clear()
        lastEditAt = nowMillis
        grouping = tiny
        trim()
    }

    /** Non-coalesced replacement, such as paste or share into the buffer. */
    fun checkpoint(next: String, nowMillis: Long) {
        if (next == current) return
        pushUndo(current)
        current = next
        redo.clear()
        lastEditAt = nowMillis
        grouping = false
        trim()
    }

    fun undo(nowMillis: Long = 0L): String? {
        if (undo.isEmpty()) return null
        redo.addLast(current)
        current = undo.removeLast()
        grouping = false
        lastEditAt = nowMillis
        trim()
        return current
    }

    fun redo(nowMillis: Long = 0L): String? {
        if (redo.isEmpty()) return null
        undo.addLast(current)
        current = redo.removeLast()
        grouping = false
        lastEditAt = nowMillis
        trim()
        return current
    }

    private fun pushUndo(value: String) {
        undo.addLast(value)
        while (undo.size > maxEntries) undo.removeFirst()
    }

    private fun trim() {
        while (undo.size > maxEntries) undo.removeFirst()
        while (redo.size > maxEntries) redo.removeFirst()
        var total = retainedChars()
        while (total > maxChars && undo.isNotEmpty()) {
            total -= undo.removeFirst().length
        }
        while (total > maxChars && redo.isNotEmpty()) {
            total -= redo.removeFirst().length
        }
    }

    private fun retainedChars(): Int {
        var total = current.length
        for (entry in undo) total += entry.length
        for (entry in redo) total += entry.length
        return total
    }

    companion object {
        fun isTinyKeystroke(before: String, after: String): Boolean {
            val delta = after.length - before.length
            if (delta !in -1..1) return false
            val changed = singleCharChange(before, after) ?: return false
            return changed != '\n' && changed != '\r'
        }

        private fun singleCharChange(before: String, after: String): Char? {
            val delta = after.length - before.length
            if (delta == 0) {
                var found: Char? = null
                for (index in before.indices) {
                    if (before[index] != after[index]) {
                        if (found != null) return null
                        found = after[index]
                    }
                }
                return found
            }
            val shorter: String
            val longer: String
            if (delta == 1) {
                shorter = before
                longer = after
            } else {
                shorter = after
                longer = before
            }
            var i = 0
            var j = 0
            var skipped: Char? = null
            while (i < shorter.length && j < longer.length) {
                if (shorter[i] == longer[j]) {
                    i++
                    j++
                } else {
                    if (skipped != null) return null
                    skipped = longer[j]
                    j++
                }
            }
            if (skipped == null) {
                if (i == shorter.length && j < longer.length) skipped = longer[j++]
                else return null
            }
            if (i != shorter.length || j != longer.length) return null
            return skipped
        }
    }
}
