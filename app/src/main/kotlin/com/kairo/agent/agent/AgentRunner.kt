package com.kairo.agent.agent

import android.graphics.Bitmap
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.EngineBackendMode
import com.kairo.agent.engine.GemmaAssistantEngine
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import kotlinx.coroutines.flow.flow

data class NerdStats(
    val tokensPerSec: Float = 0f,
    val ttftMs: Long = 0L,
    val ramUsedMb: Long = 0L,
    val directTool: Boolean = false
)

sealed interface AgentStreamEvent {
    data class PartialText(val text: String, val stats: NerdStats) : AgentStreamEvent
    data class RunningTool(val toolName: String) : AgentStreamEvent
    data class ToolCompleted(val summary: String, val success: Boolean) : AgentStreamEvent
    data class Finished(
        val finalText: String,
        val actionSummary: String?,
        val actionSuccess: Boolean,
        val stats: NerdStats,
        val attachedImage: Bitmap? = null
    ) : AgentStreamEvent
}

/**
 * Runs a multi-turn, two-step on-device agent loop with:
 * - Conversation history retention
 * - Persona switching
 * - Automatic <memory> extraction
 * - Live tok/s, TTFT, and RAM stats
 */
class AgentRunner(
    private val engine: GemmaAssistantEngine,
    private val tools: AndroidAgentTools
) {
    fun run(
        userPrompt: String,
        image: Bitmap? = null,
        screenText: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        persona: AgentPersona = AgentPersona.DEFAULT,
        thinkHarder: Boolean = false,
        settings: AgentSettings = AgentSettings(),
        incognito: Boolean = false,
        confirmDangerousTool: (suspend (toolName: String, detail: String) -> Boolean)? = null
    ): Flow<AgentStreamEvent> = flow {
        val startMs = System.currentTimeMillis()
        var firstTokenMs = 0L
        var chunkCount = 0

        // Deterministic dispatch is limited to complete, explicit supported requests.
        val direct = if (settings.usesNativeModel && image == null && screenText.isNullOrBlank() &&
            persona == AgentPersona.DEFAULT && !thinkHarder) directToolForQuery(userPrompt) else null
        if (direct != null) {
            emit(AgentStreamEvent.RunningTool(direct.name))
            val payload = JSONObject().put("tool", direct.name).put("args", JSONObject(direct.args))
            val result = tools.executeFromModelOutput(rawOutput = "<tool_call>" + payload + "</tool_call>")
            val summary = result.summary ?: "Action result unavailable"
            emit(AgentStreamEvent.ToolCompleted(summary, result.success))
            emit(AgentStreamEvent.Finished(
                finalText = result.resultData?.takeIf { it.isNotBlank() } ?: summary,
                actionSummary = summary,
                actionSuccess = result.success,
                stats = NerdStats(directTool = true)
            ))
            return@flow
        }

        val route = classifyIntent(userPrompt, !screenText.isNullOrBlank())
        val baseRoutedPrompt = when (route) {
            MicroAgentRoute.TOOL_ACTION -> "$userPrompt\n[Router: Focus on accurate JSON tool-calling.]"
            MicroAgentRoute.SUMMARIZE -> "$userPrompt\n[Router: Focus on concise, structured summarization.]"
            MicroAgentRoute.MEMORY_QUERY -> "$userPrompt\n[Router: Check local memory/ephemeral clipboard if relevant.]"
            MicroAgentRoute.GENERAL -> userPrompt
        }

        val isComplex = userPrompt.split(Regex("\\s+")).count { it.isNotEmpty() } >= 10 ||
            userPrompt.lowercase().containsAny("compare", "plan", "analyze", "step by step", "pros and cons")
        val routedPrompt = if (settings.multiAgentEnabled && isComplex) {
            emit(AgentStreamEvent.RunningTool("multi_agent_planner"))
            val plannerNotes = StringBuilder()
            engine.streamResponse(
                userQuery = "You are Kairo Planner Sub-Agent. Briefly list 3 concise steps or key angles to answer this user request accurately:\n$userPrompt",
                thinkHarder = false,
                settings = settings
            ).collect { chunk -> plannerNotes.append(chunk) }
            emit(AgentStreamEvent.ToolCompleted("Planner sub-agent shared 3-step brief", true))
            "$baseRoutedPrompt\n[Planner Sub-Agent Brief: ${plannerNotes.toString().take(500)}]"
        } else {
            baseRoutedPrompt
        }

        // Keep ambiguous or action-oriented requests on the existing full agent path.
        val localBackend = settings.backendMode == EngineBackendMode.AUTO ||
            settings.backendMode == EngineBackendMode.LOCAL_HTTP
        val lightweightChat = image == null && screenText.isNullOrBlank() &&
            persona == AgentPersona.DEFAULT && !thinkHarder && !settings.multiAgentEnabled &&
            !settings.rootPowerMode && settings.mcpServerUrls.isBlank() &&
            localBackend && route == MicroAgentRoute.GENERAL &&
            isLightweightChatQuery(userPrompt)
        val initialPrompt = tools.buildAgentPrompt(
            userMessage = routedPrompt,
            screenText = screenText,
            conversationHistory = conversationHistory,
            persona = persona,
            rootPowerMode = settings.rootPowerMode,
            preferredLanguage = settings.preferredLanguage,
            incognito = incognito,
            lightweightGreeting = lightweightChat
        )
        val firstTurnRaw = StringBuilder()

        engine.streamResponse(
            userQuery = initialPrompt,
            image = image,
            thinkHarder = thinkHarder,
            settings = settings
        ).collect { chunk ->
            if (firstTokenMs == 0L) firstTokenMs = System.currentTimeMillis()
            chunkCount += chunk.split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
            firstTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(firstTurnRaw.toString())
            if (visible.isNotEmpty() && !tools.hasToolCall(firstTurnRaw.toString())) {
                emit(AgentStreamEvent.PartialText(visible, computeStats(startMs, firstTokenMs, chunkCount)))
            }
        }

        val rawOutput = firstTurnRaw.toString()
        if (lightweightChat) {
            // A plain chat response must never execute tools or persist memory.
            val clean = tools.stripToolCallMarkup(rawOutput).ifBlank {
                "I didn't receive a response from the model. Try rephrasing or tapping Regenerate."
            }
            emit(AgentStreamEvent.Finished(
                finalText = clean,
                actionSummary = null,
                actionSuccess = true,
                stats = computeStats(startMs, firstTokenMs, chunkCount)
            ))
            return@flow
        }
        val autoSavedMemories = if (incognito) emptyList() else tools.extractAndSaveMemories(rawOutput)

        if (!tools.hasToolCall(rawOutput)) {
            val clean = tools.stripToolCallMarkup(rawOutput)
                .ifBlank {
                    if (autoSavedMemories.isNotEmpty()) "Got it — I've remembered that (${autoSavedMemories.first()})."
                    else "I didn't receive a response from the model for that prompt. Try rephrasing or tapping Regenerate."
                }
            val memSummary = if (autoSavedMemories.isNotEmpty()) "Remembered: ${autoSavedMemories.first()}" else null
            emit(
                AgentStreamEvent.Finished(
                    finalText = clean,
                    actionSummary = memSummary,
                    actionSuccess = true,
                    stats = computeStats(startMs, firstTokenMs, chunkCount)
                )
            )
            return@flow
        }

        val detectedTool = tools.extractToolName(rawOutput) ?: "action"
        if (detectedTool in DANGEROUS_TOOLS && confirmDangerousTool != null) {
            val friendly = tools.formatFriendlyToolDescription(detectedTool, rawOutput)
            val allowed = confirmDangerousTool(detectedTool, friendly)
            if (!allowed) {
                emit(
                    AgentStreamEvent.Finished(
                        finalText = "Cancelled `$detectedTool` before execution.",
                        actionSummary = "Cancelled: $detectedTool",
                        actionSuccess = false,
                        stats = computeStats(startMs, firstTokenMs, chunkCount)
                    )
                )
                return@flow
            }
        }
        emit(AgentStreamEvent.RunningTool(detectedTool))

        var actionResult = tools.executeFromModelOutput(
            rawOutput = rawOutput,
            attachedImage = image,
            rootPowerMode = settings.rootPowerMode
        )

        // Self-healing retry loop: if a tool call fails, feed the error back once so Kairo can course-correct
        if (!actionResult.success) {
            val retryPrompt = tools.buildFollowUpPrompt(
                userMessage = "$userPrompt\n[System: Previous tool `${actionResult.toolName ?: detectedTool}` failed with: ${actionResult.summary ?: actionResult.resultData}. Course-correct with another tool call if possible, or explain clearly.]",
                toolName = actionResult.toolName ?: detectedTool,
                toolResult = actionResult.resultData ?: actionResult.summary.orEmpty(),
                success = false,
                persona = persona
            )
            val retryRaw = StringBuilder()
            engine.streamResponse(
                userQuery = retryPrompt,
                thinkHarder = thinkHarder,
                settings = settings
            ).collect { chunk -> retryRaw.append(chunk) }
            val retryStr = retryRaw.toString()
            if (tools.hasToolCall(retryStr)) {
                val retryTool = tools.extractToolName(retryStr) ?: "action"
                if (retryTool !in DANGEROUS_TOOLS || confirmDangerousTool == null || confirmDangerousTool(retryTool, retryStr.take(240))) {
                    emit(AgentStreamEvent.RunningTool(retryTool))
                    actionResult = tools.executeFromModelOutput(
                        rawOutput = retryStr,
                        attachedImage = image,
                        rootPowerMode = settings.rootPowerMode
                    )
                }
            }
        }
        val summary = actionResult.summary
        if (summary != null) {
            emit(AgentStreamEvent.ToolCompleted(summary = summary, success = actionResult.success))
        }

        val followUpPrompt = tools.buildFollowUpPrompt(
            userMessage = userPrompt,
            toolName = actionResult.toolName ?: detectedTool,
            toolResult = actionResult.resultData ?: summary.orEmpty(),
            success = actionResult.success,
            persona = persona
        )

        val secondTurnRaw = StringBuilder()
        engine.streamResponse(
            userQuery = followUpPrompt,
            thinkHarder = thinkHarder,
            settings = settings
        ).collect { chunk ->
            if (firstTokenMs == 0L) firstTokenMs = System.currentTimeMillis()
            chunkCount += chunk.split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
            secondTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(secondTurnRaw.toString())
            if (visible.isNotEmpty()) {
                emit(AgentStreamEvent.PartialText(visible, computeStats(startMs, firstTokenMs, chunkCount)))
            }
        }

        val finalClean = tools.stripToolCallMarkup(secondTurnRaw.toString())
            .ifBlank { tools.stripToolCallMarkup(rawOutput) }
            .ifBlank { actionResult.resultData?.takeIf { it.isNotBlank() } ?: summary.orEmpty() }
            .ifBlank { "Done." }

        emit(
            AgentStreamEvent.Finished(
                finalText = finalClean,
                actionSummary = summary,
                actionSuccess = actionResult.success,
                stats = computeStats(startMs, firstTokenMs, chunkCount),
                attachedImage = actionResult.attachedBitmap
            )
        )
    }

    private fun computeStats(startMs: Long, firstTokenMs: Long, tokens: Int): NerdStats {
        val now = System.currentTimeMillis()
        // Text chunks are only a token-count estimate; exclude initial prompt latency.
        val generationSec = if (firstTokenMs > 0L) (now - firstTokenMs) / 1000f else 0f
        val ttft = if (firstTokenMs > 0L) (firstTokenMs - startMs).coerceAtLeast(0L) else 0L
        val rt = Runtime.getRuntime()
        val ramMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)
        engine.nativeStats?.let { native ->
            return NerdStats(
                tokensPerSec = native.tokensPerSecond,
                ttftMs = ttft,
                ramUsedMb = native.processPssMb
            )
        }
        return NerdStats(
            tokensPerSec = if (generationSec > 0f && tokens > 1) {
                (tokens - 1) / generationSec
            } else 0f,
            ttftMs = ttft,
            ramUsedMb = ramMb
        )
    }

    private fun classifyIntent(prompt: String, hasScreenText: Boolean): MicroAgentRoute {
        val lower = prompt.lowercase()
        return when {
            lower.containsAny("copied", "clipboard", "earlier", "remember", "forgot") -> MicroAgentRoute.MEMORY_QUERY
            lower.containsAny("summarize", "summary", "tldr", "tl;dr", "catch up") || hasScreenText -> MicroAgentRoute.SUMMARIZE
            lower.containsAny("call ", "text ", "open ", "alarm", "timer", "calculate", "python", "weather", "search", "time") -> MicroAgentRoute.TOOL_ACTION
            else -> MicroAgentRoute.GENERAL
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean = needles.any { this.contains(it) }

    private enum class MicroAgentRoute { TOOL_ACTION, SUMMARIZE, MEMORY_QUERY, GENERAL }

    private companion object {
        val DANGEROUS_TOOLS = setOf(
            "call_person", "dial_phone", "text_person", "send_sms",
            "send_whatsapp", "send_email", "run_shell_command",
            "set_ringer_mode", "forget_memory"
        )
    }
}


// Conservatively recognize plain factual questions in English and French.
private val LIGHTWEIGHT_GREETING = Regex("""^(hi|hello|hey|bonjour|salut|coucou|yo)[!?.\s]*$""", RegexOption.IGNORE_CASE)
private val LIGHTWEIGHT_QUESTION = Regex("""^(explain\b|define\b|what (?:is|are)\b|how (?:does|do)\b|why\b|describe\b|explique\b|définis\b|definis\b|qu['’]est-ce (?:que|qu['’])|c['’]est quoi\b|comment (?:fonctionne|marche)\b|pourquoi\b|décris\b|decris\b)""", RegexOption.IGNORE_CASE)
private val ACTION_OR_CONTEXT = Regex("""(?:^|\s)[/@][a-z_]{3,}\b|\b(?:my|me|mine|mon|ma|mes|moi|phone|téléphone|battery|batterie|notification[s]?|clipboard|presse-papiers|contact[s]?|weather|météo|today|aujourd'hui|currently|actuellement|latest|récent|remember|souviens|alarm|alarme|timer|minuteur|search|cherche|recherche|open|ouvre|send|envoie|call|appelle|install|installe|settings|paramètres|calendar|calendrier|location|localisation|gps|sms|whatsapp|email|root|shell|mcp)\b""", RegexOption.IGNORE_CASE)
internal fun isLightweightChatQuery(query: String): Boolean {
    val text = query.trim()
    if (LIGHTWEIGHT_GREETING.matches(text)) return true
    if (text.length > 500 || !LIGHTWEIGHT_QUESTION.containsMatchIn(text)) return false
    // "Explain to me" / "Explique-moi" are ordinary explanatory prefixes.
    val normalized = text.replace(Regex("""^(explain to me|explique[- ]moi)\b""", RegexOption.IGNORE_CASE), "explain")
    return !ACTION_OR_CONTEXT.containsMatchIn(normalized)
}


// Whole-message matches only; no model-generated tool names or write actions.
private val DIRECT_STATUS_EN = Regex("""^(?:please\s+)?(?:(?:check|show(?: me)?|tell me|give me|what is|what's)\s+)?(?:my\s+|the\s+)?(?:battery(?:\s+(?:level|percentage|status))?(?:\s*(?:&|and)\s*(?:device\s+)?status)?|(?:phone|device)\s+status)(?:\s+please)?[?.!]*$""", RegexOption.IGNORE_CASE)
private val DIRECT_STATUS_FR = Regex("""^(?:v[ée]rifie|affiche|donne-moi|quel est)\s+(?:mon|ma|le|la)\s+(?:batterie|niveau de batterie|(?:[ée]tat|statut) (?:du|de mon) t[ée]l[ée]phone)[?.!]*$""", RegexOption.IGNORE_CASE)
internal fun isDirectDeviceStatusQuery(query: String): Boolean {
    val text = query.trim().replace(Regex("""\s+([?.!])$"""), "$1")
    return DIRECT_STATUS_EN.matches(text) || DIRECT_STATUS_FR.matches(text)
}
