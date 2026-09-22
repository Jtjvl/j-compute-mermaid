package com.jcompute.mermaidpocket

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAssetTest {
    @Test
    fun mipmapsMatchTheMusesManifestAndAreSquare() {
        val manifest = read("design/launcher/v2/manifest.json")
        assertTrue(manifest.contains("\"version\": \"v2-adaptive-square\""))
        assertTrue(manifest.contains("\"locked_by\": \"Muses\""))
        val entries = Regex(
            """"path": "([^"]+)",\s*"sha256": "([0-9a-f]+)",\s*"bytes": (\d+)""",
        ).findAll(manifest).map { match ->
            Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3].toLong())
        }.toList()
        val expected = mapOf(
            "mipmap-mdpi/ic_launcher.png" to (48 to 48),
            "mipmap-mdpi/ic_launcher_foreground.png" to (108 to 108),
            "mipmap-mdpi/ic_launcher_background.png" to (108 to 108),
            "mipmap-hdpi/ic_launcher.png" to (72 to 72),
            "mipmap-hdpi/ic_launcher_foreground.png" to (162 to 162),
            "mipmap-hdpi/ic_launcher_background.png" to (162 to 162),
            "mipmap-xhdpi/ic_launcher.png" to (96 to 96),
            "mipmap-xhdpi/ic_launcher_foreground.png" to (216 to 216),
            "mipmap-xhdpi/ic_launcher_background.png" to (216 to 216),
            "mipmap-xxhdpi/ic_launcher.png" to (144 to 144),
            "mipmap-xxhdpi/ic_launcher_foreground.png" to (324 to 324),
            "mipmap-xxhdpi/ic_launcher_background.png" to (324 to 324),
            "mipmap-xxxhdpi/ic_launcher.png" to (192 to 192),
            "mipmap-xxxhdpi/ic_launcher_foreground.png" to (432 to 432),
            "mipmap-xxxhdpi/ic_launcher_background.png" to (432 to 432),
        )
        var rasterCount = 0
        assertTrue(entries.size >= expected.size)
        for ((path, digest, bytes) in entries) {
            val packed = file("design/launcher/v2/$path")
            assertEquals(digest, sha256(packed))
            assertEquals(bytes, packed.length())
            val size = expected[path] ?: continue
            rasterCount++
            val shipped = file("app/src/main/res/$path")
            assertEquals(digest, sha256(shipped))
            assertEquals(size, pngSize(shipped))
        }
        assertEquals(expected.size, rasterCount)
        assertFalse(file("app/src/main/res/drawable/ic_launcher_foreground.png").exists())
        assertFalse(file("app/src/main/res/drawable-xxhdpi/ic_launcher_foreground.png").exists())
        assertFalse(file("app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png").exists())
        val adaptive = read("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue(adaptive.contains("@mipmap/ic_launcher_background"))
        assertTrue(adaptive.contains("@mipmap/ic_launcher_foreground"))
    }

    private fun pngSize(file: File): Pair<Int, Int> {
        val bytes = file.readBytes()
        assertTrue(bytes.size > 24)
        val width = ((bytes[16].toInt() and 0xff) shl 24) or
            ((bytes[17].toInt() and 0xff) shl 16) or
            ((bytes[18].toInt() and 0xff) shl 8) or
            (bytes[19].toInt() and 0xff)
        val height = ((bytes[20].toInt() and 0xff) shl 24) or
            ((bytes[21].toInt() and 0xff) shl 16) or
            ((bytes[22].toInt() and 0xff) shl 8) or
            (bytes[23].toInt() and 0xff)
        return width to height
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun read(relative: String) = file(relative).readText()

    private fun file(relative: String): File {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.firstOrNull { it.exists() } ?: candidates.last()
    }
}
