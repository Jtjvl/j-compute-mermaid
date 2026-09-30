package com.jcompute.mermaidpocket

import com.jcompute.mermaidpocket.ai.AgentOutcome
import com.jcompute.mermaidpocket.ai.AgentRequest
import com.jcompute.mermaidpocket.ai.AiAvailability
import com.jcompute.mermaidpocket.ai.AiDefaults
import com.jcompute.mermaidpocket.ai.ApplyStrategy
import com.jcompute.mermaidpocket.ai.BraveSearchClient
import com.jcompute.mermaidpocket.ai.DiagramAgent
import com.jcompute.mermaidpocket.ai.JevClassifier
import com.jcompute.mermaidpocket.ai.JsonHttp
import com.jcompute.mermaidpocket.ai.MermaidProposal
import com.jcompute.mermaidpocket.ai.MermaidValidation
import com.jcompute.mermaidpocket.ai.MermaidValidator
import com.jcompute.mermaidpocket.ai.OpenRouterClient
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JevClassifierTest {
    @Test
    fun parsesChoicePatch() {
        val json = """
            {"answers":{"apply_strategy":{"type":"choice","choice":"patch","confidence":0.91}}}
        """.trimIndent()
        assertEquals(ApplyStrategy.Patch, JevClassifier.parse(json))
    }

    @Test
    fun parsesDecisionRewrite() {
        val json = """
            {"decisions":{"apply_strategy":{"type":"choice","decision":"rewrite"}}}
        """.trimIndent()
        assertEquals(ApplyStrategy.Rewrite, JevClassifier.parse(json))
    }

    @Test
    fun unknownPayloadIsNull() {
        assertEquals(null, JevClassifier.parse("""{"answers":{}}"""))
    }

    @Test
    fun heuristicPatchesShortLocalTweaks() {
        val current = "flowchart TD\n  A-->B"
        assertEquals(ApplyStrategy.Patch, JevClassifier.heuristic("make the boxes blue", current))
        assertEquals(ApplyStrategy.Patch, JevClassifier.heuristic("rename Start to Begin", current))
    }

    @Test
    fun heuristicRewritesNewDiagrams() {
        val current = "flowchart TD\n  A-->B"
        assertEquals(ApplyStrategy.Rewrite, JevClassifier.heuristic("draw a sequence diagram of checkout", current))
        assertEquals(ApplyStrategy.Rewrite, JevClassifier.heuristic("anything", ""))
    }
}

class MermaidProposalTest {
    @Test
    fun extractsFencedSource() {
        val raw = """
            Sure.

            ```mermaid
            flowchart TD
                A --> B
            ```
        """.trimIndent()
        assertEquals("flowchart TD\n    A --> B", MermaidProposal.extract(raw))
        assertTrue(MermaidProposal.looksLikeMermaid(raw))
    }

    @Test
    fun extractsJsonToolArgs() {
        val raw = """{"source":"sequenceDiagram\n    A->>B: hi"}"""
        assertEquals("sequenceDiagram\n    A->>B: hi", MermaidProposal.extract(raw))
        assertTrue(MermaidProposal.looksLikeMermaid(raw))
    }

    @Test
    fun rejectsPlainProse() {
        assertFalse(MermaidProposal.looksLikeMermaid("please add a legend"))
        assertEquals("please add a legend", MermaidProposal.extract("please add a legend"))
    }

    @Test
    fun acceptsDirectiveThenFlowchart() {
        val raw = "%%{init: {'theme':'dark'}}%%\nflowchart LR\n  A-->B"
        assertTrue(MermaidProposal.looksLikeMermaid(raw))
    }
}

class AiAvailabilityTest {
    @Test
    fun micNeedsKeyAndNetwork() {
        assertFalse(AiAvailability.voiceEnabled(hasOpenRouterKey = false, hasNetwork = true))
        assertFalse(AiAvailability.voiceEnabled(hasOpenRouterKey = true, hasNetwork = false))
        assertTrue(AiAvailability.voiceEnabled(hasOpenRouterKey = true, hasNetwork = true))
        assertEquals(
            AiAvailability.NEEDS_NETWORK_AND_KEY,
            AiAvailability.disabledReason(hasOpenRouterKey = false, hasNetwork = false),
        )
    }
}

class DiagramAgentTest {
    private val valid = "flowchart TD\n    A --> B"
    private val invalid = "flowchart TD\n    A -->"

    @Test
    fun happyPathGetThenSetAppliesValidMermaid() = runBlocking {
        val http = ScriptedHttp()
        http.enqueueTool(toolCall("get_diagram", "{}"))
        http.enqueueTool(toolCall("set_diagram", """{"source":${jsonString(valid)}}"""))
        val agent = DiagramAgent(
            openRouter = OpenRouterClient(http),
            search = BraveSearchClient(http),
            validator = RecordingValidator(ok = setOf(valid)),
        )
        val outcome = agent.run(request(currentSource = "flowchart TD\n    A --> C"))
        assertTrue(outcome is AgentOutcome.Applied)
        val applied = outcome as AgentOutcome.Applied
        assertEquals(valid, applied.source)
        assertEquals(ApplyStrategy.Patch, applied.strategy)
        assertFalse(applied.usedWeb)
    }

    @Test
    fun invalidSetIsRejectedAndLastGoodStaysWithCaller() = runBlocking {
        val http = ScriptedHttp()
        http.enqueueTool(toolCall("set_diagram", """{"source":${jsonString(invalid)}}"""))
        http.enqueueText("I cannot fix it")
        val agent = DiagramAgent(
            openRouter = OpenRouterClient(http),
            search = BraveSearchClient(http),
            validator = RecordingValidator(ok = emptySet()),
        )
        val outcome = agent.run(request(currentSource = valid))
        assertTrue(outcome is AgentOutcome.Invalid)
        assertEquals(invalid, (outcome as AgentOutcome.Invalid).attempted)
    }

    @Test
    fun webLookupIsUsedWhenModelAsks() = runBlocking {
        val http = ScriptedHttp()
        http.enqueueTool(toolCall("web_lookup", """{"query":"OSI model layers"}"""))
        http.onGet = {
            """{"web":{"results":[{"title":"OSI","url":"https://example.com","description":"7 layers"}]}}"""
        }
        http.enqueueTool(toolCall("set_diagram", """{"source":${jsonString(valid)}}"""))
        val agent = DiagramAgent(
            openRouter = OpenRouterClient(http),
            search = BraveSearchClient(http),
            validator = RecordingValidator(ok = setOf(valid)),
        )
        val outcome = agent.run(request(currentSource = "", searchKey = "brave-test", strategy = ApplyStrategy.Rewrite))
        assertTrue(outcome is AgentOutcome.Applied)
        assertTrue((outcome as AgentOutcome.Applied).usedWeb)
        assertTrue(http.getUrls.any { it.contains("api.search.brave.com") })
    }

    @Test
    fun patchPromptMentionsMinimalEdit() {
        val prompt = DiagramAgent.systemPrompt(ApplyStrategy.Patch, searchEnabled = false)
        assertTrue(prompt.contains("PATCH"))
        assertTrue(prompt.contains("set_diagram"))
        assertFalse(prompt.contains("web_lookup when you need facts"))
    }

    private fun request(
        currentSource: String,
        searchKey: String = "",
        strategy: ApplyStrategy = ApplyStrategy.Patch,
    ): AgentRequest {
        return AgentRequest(
            transcript = "make A go to B",
            currentSource = currentSource,
            strategy = strategy,
            chatModel = AiDefaults.CHAT_MODEL,
            apiKey = "sk-or-test",
            searchKey = searchKey,
        )
    }
}

class VoiceChromeTest {
    @Test
    fun editAndViewKeepAVisibleMic() {
        val screen = source("app/src/main/java/com/jcompute/mermaidpocket/ui/EditorScreen.kt")
        val bar = screen.substringAfter("private fun DocumentTopBar").substringBefore("internal fun PocketOverflowMenu")
        assertTrue(bar.contains("Icons.Outlined.Mic"))
        assertTrue(bar.contains("enabled = state.aiEnabled"))
        assertTrue(bar.contains("state.voiceControlDescription"))
        assertTrue(screen.contains("VoiceTranscriptSheet"))
        assertTrue(screen.contains("SettingsScreen"))
        val overflow = screen.substringAfter("fun PocketOverflowMenu").substringBefore("private fun DocumentBottomBar")
        assertTrue(overflow.contains("Text(\"Settings\")"))
    }

    @Test
    fun webViewStillBlocksNetworkLoads() {
        val engine = source("app/src/main/java/com/jcompute/mermaidpocket/ui/MermaidWebView.kt")
        assertTrue(engine.contains("settings.blockNetworkLoads = true"))
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.RECORD_AUDIO"))
        assertTrue(manifest.contains("android.speech.RecognitionService"))
    }

    @Test
    fun keysAreNotPlaintextPrefs() {
        val secrets = source("app/src/main/java/com/jcompute/mermaidpocket/ai/AiSecrets.kt")
        assertTrue(secrets.contains("EncryptedSharedPreferences"))
        assertTrue(secrets.contains("MasterKeys"))
        assertTrue(secrets.contains("AiDefaults.SECRETS_PREFS"))
        val settings = source("app/src/main/java/com/jcompute/mermaidpocket/data/AppSettings.kt")
        assertFalse(settings.contains("openrouter_api_key"))
        assertFalse(settings.contains("brave_search_api_key"))
    }

    @Test
    fun classifierDefaultIsJevSlug() {
        assertEquals("typesafe/jev-1.13", AiDefaults.CLASSIFIER_MODEL)
        assertEquals("openai/gpt-4o-mini", AiDefaults.CHAT_MODEL)
        val defaults = source("app/src/main/java/com/jcompute/mermaidpocket/ai/AiDefaults.kt")
        assertTrue(defaults.contains("openrouter.ai/api/alpha/decisions"))
        assertTrue(defaults.contains("api.search.brave.com"))
    }

    private fun source(relative: String): String {
        val candidates = listOf(File(relative), File("../$relative"))
        return candidates.first { it.exists() }.readText()
    }
}

private class RecordingValidator(private val ok: Set<String>) : MermaidValidator {
    override suspend fun validate(source: String): MermaidValidation {
        return if (source in ok) MermaidValidation(ok = true) else MermaidValidation(ok = false, error = "parse failed")
    }
}

private class ScriptedHttp : JsonHttp {
    private val chatQueue = ArrayDeque<String>()
    val getUrls = mutableListOf<String>()
    var onGet: (() -> String)? = null

    fun enqueueTool(messageJson: String) {
        chatQueue.addLast("""{"choices":[{"finish_reason":"tool_calls","message":$messageJson}]}""")
    }

    fun enqueueText(content: String) {
        chatQueue.addLast(
            """{"choices":[{"finish_reason":"stop","message":{"role":"assistant","content":${jsonString(content)}}}]}""",
        )
    }

    override suspend fun request(method: String, url: String, headers: Map<String, String>, body: String?): String {
        if (method == "GET") {
            getUrls.add(url)
            return onGet?.invoke() ?: error("No GET script for $url")
        }
        check(chatQueue.isNotEmpty()) { "Unexpected POST $url $body" }
        return chatQueue.removeFirst()
    }
}

private fun toolCall(name: String, arguments: String): String {
    return """{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"$name","arguments":${jsonString(arguments)}}}]}"""
}

private fun jsonString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}
