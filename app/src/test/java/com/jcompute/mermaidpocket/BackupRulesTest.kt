package com.jcompute.mermaidpocket

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRulesTest {
    @Test
    fun rulesKeepPrivateFilesAndSkipThumbnails() {
        val module = File(System.getProperty("user.dir"))
        val cloud = File(module, "src/main/res/xml/data_extraction_rules.xml").readText()
        val legacy = File(module, "src/main/res/xml/backup_rules.xml").readText()
        for (text in listOf(cloud, legacy)) {
            assertTrue(text.contains("domain=\"file\""))
            assertTrue(text.contains("path=\".\""))
            assertTrue(text.contains("diagrams/.thumbs"))
            assertTrue(text.contains("sharedpref"))
            assertTrue(!text.contains("domain=\"external\""))
        }
        assertTrue(cloud.contains("<cloud-backup>"))
        assertTrue(cloud.contains("<device-transfer>"))
    }
}
