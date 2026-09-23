package com.jcompute.mermaidpocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SemverReleaseScriptTest {
    private val repoRoot = File(".").let { dir ->
        generateSequence(dir.absoluteFile) { it.parentFile }
            .first { File(it, "scripts/ci/semver_release.py").isFile }
    }

    @Test
    fun envExportsVersionNameCodeAndTag() {
        val script = File(repoRoot, "scripts/ci/semver_release.py")
        val proc = ProcessBuilder("python3", script.absolutePath, "env")
            .directory(repoRoot)
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText()
        assertEquals(0, proc.waitFor())
        assertTrue(out.contains("VERSION="))
        assertTrue(out.contains("VERSION_CODE="))
        assertTrue(out.contains("TAG=v"))
        val version = Regex("""VERSION=([0-9]+\.[0-9]+\.[0-9]+)""").find(out)!!.groupValues[1]
        assertTrue(out.contains("TAG=v$version"))
    }

    @Test
    fun notesIncludeChangelogSectionAndVersionLine() {
        val script = File(repoRoot, "scripts/ci/semver_release.py")
        val proc = ProcessBuilder("python3", script.absolutePath, "notes")
            .directory(repoRoot)
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.bufferedReader().readText()
        assertEquals(0, proc.waitFor())
        val env = ProcessBuilder("python3", script.absolutePath, "env")
            .directory(repoRoot)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
        val version = Regex("""VERSION=([0-9]+\.[0-9]+\.[0-9]+)""").find(env)!!.groupValues[1]
        val code = Regex("""VERSION_CODE=([0-9]+)""").find(env)!!.groupValues[1]
        assertTrue(out.startsWith("## $version"))
        assertTrue(out.contains("`versionName` $version · `versionCode` $code"))
    }
}
