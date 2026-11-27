package com.kairo.agent.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.DownloadStatus
import com.kairo.agent.agent.AgentPersona
import com.kairo.agent.agent.AgentRunner
import com.kairo.agent.agent.AgentStreamEvent
import com.kairo.agent.agent.AndroidAgentTools
import com.kairo.agent.agent.NerdStats
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.AgentSettingsStore
import com.kairo.agent.data.ChatHistoryStore
import com.kairo.agent.data.ChatRecord
import com.kairo.agent.data.MemoryItem
import com.kairo.agent.data.MemoryStore
import com.kairo.agent.engine.AiCoreModelOption
import com.kairo.agent.engine.CompatibilityChecker
import com.kairo.agent.engine.CompatibilityReport
import com.kairo.agent.engine.GemmaAssistantEngine
import com.kairo.agent.engine.ModelAvailability
import com.kairo.agent.update.GitHubReleaseNote
import com.kairo.agent.update.UpdateCheckResult
import com.kairo.agent.update.UpdateChecker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationTurn(
    val prompt: String,
    val response: String,
    val image: Bitmap? = null,
    val responseImage: Bitmap? = null,
    val actionSummary: String? = null,
    val actionSuccess: Boolean = true,
    val isStreaming: Boolean = false,
    val versions: List<String> = if (response.isNotEmpty()) listOf(response) else emptyList(),
    val selectedVersionIndex: Int = 0
)

data class PendingToolConfirmation(
    val toolName: String,
    val detail: String,
    val onDecision: (Boolean) -> Unit
)

data class GemmAHomeState(
    val availability: ModelAvailability = ModelAvailability.CHECKING,
    val availableAiCoreModels: List<AiCoreModelOption> = emptyList(),
    val downloadProgressBytes: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val isWarmingUp: Boolean = false,
    val statusDetail: String? = null,
    val recentChats: List<ChatRecord> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val activeTurns: List<ConversationTurn> = emptyList(),
    val isGenerating: Boolean = false,
    val selectedPersona: AgentPersona = AgentPersona.DEFAULT,
    val nerdStats: NerdStats? = null,
    val settings: AgentSettings = AgentSettings(),
    val compatibilityReport: CompatibilityReport? = null,
    val isCheckingCompatibility: Boolean = false,
    val healthConnectGranted: Boolean = false,
    val updateStatus: UpdateCheckResult? = null,
    val isCheckingUpdates: Boolean = false,
    val releaseNotes: List<GitHubReleaseNote> = emptyList(),
    val isLoadingReleaseNotes: Boolean = false,
    val releaseNotesError: String? = null,
    val pendingToolConfirmation: PendingToolConfirmation? = null,
    val incognitoMode: Boolean = false,
    val deviceTempC: Float = 0f,
    val thermalLabel: String = "Nominal",
    val isThermallyThrottled: Boolean = false
)

class MainScreenViewModel(
    private val chatHistoryStore: ChatHistoryStore? = null,
    private val memoryStore: MemoryStore? = null,
    private val agentTools: AndroidAgentTools? = null,
    private val settingsStore: AgentSettingsStore? = null,
    private val compatibilityChecker: CompatibilityChecker? = null,
    private val updateChecker: UpdateChecker? = null,
    private val engine: GemmaAssistantEngine = GemmaAssistantEngine()
) : ViewModel() {

    private val _state = MutableStateFlow(
        GemmAHomeState(settings = settingsStore?.load() ?: AgentSettings())
    )
    val state: StateFlow<GemmAHomeState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var activeSessionId: String = java.util.UUID.randomUUID().toString()

    init {
        refreshStatus()
        refreshChats()
        refreshMemories()
    }

    fun selectPersona(persona: AgentPersona) {
        _state.update { it.copy(selectedPersona = persona) }
    }

    fun updateSettings(newSettings: AgentSettings) {
        val profileChanged = newSettings.aiCoreModelProfile != _state.value.settings.aiCoreModelProfile ||
            newSettings.usesNativeModel != _state.value.settings.usesNativeModel
        settingsStore?.save(newSettings)
        _state.update { it.copy(settings = newSettings) }
        if (profileChanged) {
            refreshStatus()
        }
    }

    fun runCompatibilityChecks() {
        val checker = compatibilityChecker ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingCompatibility = true) }
            val currentAvailability = _state.value.availability
            val report = checker.runChecks(
                aiCoreAvailability = currentAvailability,
                localEndpointUrl = _state.value.settings.localEndpointUrl
            )
            _state.update {
                it.copy(
                    compatibilityReport = report,
                    healthConnectGranted = report.healthConnectGranted,
                    isCheckingCompatibility = false
                )
            }
        }
    }

    fun checkForUpdates() {
        val checker = updateChecker ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingUpdates = true) }
            val result = checker.checkForUpdates()
            _state.update { it.copy(isCheckingUpdates = false, updateStatus = result) }
        }
    }

    fun fetchReleaseNotes() {
        val checker = updateChecker ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingReleaseNotes = true, releaseNotesError = null) }
            checker.fetchReleaseNotes()
                .onSuccess { notes ->
                    _state.update { it.copy(isLoadingReleaseNotes = false, releaseNotes = notes) }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isLoadingReleaseNotes = false,
                            releaseNotesError = err.localizedMessage ?: "Failed to load release notes."
                        )
                    }
                }
        }
    }

    fun completeOnboarding(userName: String = "") {
        if (userName.isNotBlank()) {
            addMemory("The user's name is ${userName.trim()}")
        }
        updateSettings(_state.value.settings.copy(hasCompletedOnboarding = true))
    }

    fun refreshChats() {
        val chats = chatHistoryStore?.loadChats().orEmpty()
        _state.update { it.copy(recentChats = chats) }
    }

    fun deleteChat(id: String) {
        chatHistoryStore?.deleteChat(id)
        refreshChats()
    }

    fun clearChats() {
        chatHistoryStore?.clearChats()
        _state.update { it.copy(recentChats = emptyList()) }
    }

    fun refreshMemories() {
        val items = memoryStore?.loadMemories().orEmpty()
        _state.update { it.copy(memories = items) }
    }

    fun addMemory(fact: String) {
        if (memoryStore?.addMemory(fact) == true) {
            refreshMemories()
        }
    }

    fun deleteMemory(id: String) {
        memoryStore?.deleteMemory(id)
        refreshMemories()
    }

    fun clearMemories() {
        memoryStore?.clearMemories()
        _state.update { it.copy(memories = emptyList()) }
    }

    fun toggleIncognitoMode() {
        _state.update { it.copy(incognitoMode = !it.incognitoMode) }
    }

    fun startNewChat(incognito: Boolean = false) {
        activeJob?.cancel()
        activeSessionId = java.util.UUID.randomUUID().toString()
        _state.update {
            it.copy(
                activeTurns = emptyList(),
                isGenerating = false,
                statusDetail = null,
                nerdStats = null,
                incognitoMode = incognito
            )
        }
    }

    fun selectTurnVersion(turnIndex: Int, versionIndex: Int) {
        _state.update { state ->
            val updated = state.activeTurns.toMutableList()
            val turn = updated.getOrNull(turnIndex) ?: return@update state
            val clamped = versionIndex.coerceIn(0, (turn.versions.size - 1).coerceAtLeast(0))
            val text = turn.versions.getOrElse(clamped) { turn.response }
            updated[turnIndex] = turn.copy(response = text, selectedVersionIndex = clamped)
            state.copy(activeTurns = updated)
        }
    }

    fun consumeOverlayHandoff() {
        val handoff = com.kairo.agent.data.OverlayHandoffHolder.consumeTurns() ?: return
        activeJob?.cancel()
        activeSessionId = java.util.UUID.randomUUID().toString()
        _state.update {
            it.copy(
                activeTurns = handoff.map { h ->
                    ConversationTurn(
                        prompt = h.prompt,
                        response = h.response,
                        image = h.image,
                        actionSummary = h.actionSummary,
                        actionSuccess = true,
                        isStreaming = false
                    )
                },
                isGenerating = false,
                statusDetail = null
            )
        }
    }

    fun regenerateTurn(turnIndex: Int) {
        val turn = _state.value.activeTurns.getOrNull(turnIndex) ?: return
        sendPrompt(userPrompt = turn.prompt, image = turn.image, replaceTurnIndex = turnIndex)
    }

    fun loadChatIntoView(record: ChatRecord) {
        activeJob?.cancel()
        activeSessionId = record.id
        val loadedTurns = if (record.turns.isNotEmpty()) {
            record.turns.map { t ->
                ConversationTurn(
                    prompt = t.prompt,
                    response = t.response,
                    actionSummary = t.actionSummary,
                    actionSuccess = true,
                    isStreaming = false
                )
            }
        } else {
            listOf(
                ConversationTurn(
                    prompt = record.prompt,
                    response = record.response,
                    actionSummary = record.actionSummary,
                    actionSuccess = true,
                    isStreaming = false
                )
            )
        }
        _state.update {
            it.copy(
                activeTurns = loadedTurns,
                isGenerating = false,
                statusDetail = null
            )
        }
    }

    fun sendPrompt(
        userPrompt: String,
        image: Bitmap? = null,
        thinkHarder: Boolean = false,
        replaceTurnIndex: Int? = null
    ) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() && image == null) return
        val effectivePrompt = trimmed.ifEmpty { "Describe this image." }

        // Fun local Easter eggs
        val lower = effectivePrompt.lowercase()
        val easterReply = when (lower) {
            "/about" -> "✦ **Kairo** — 100% on-device AI agent powered by Gemma 4 via Android AICore.\nCreated by **DaManMikey** (`https://damanmikey.me/`)."
            "konami", "up up down down left right left right b a" -> "🎮 **30 extra local neurons unlocked!** Still 100% private, still zero cloud servers. Shoutout to **DaManMikey**!"
            "/ping" -> "⚡ **Pong! 0ms** — running locally on your device's silicon."
            "/matrix" -> "🕶️ **There is no cloud.** Only local weights, right here on your phone."
            "/coffee" -> "☕ **Local inference runs on battery, not caffeine.**"
            else -> null
        }
        if (easterReply != null) {
            _state.update {
                it.copy(
                    activeTurns = it.activeTurns + ConversationTurn(
                        prompt = effectivePrompt,
                        response = easterReply,
                        actionSummary = "Easter egg unlocked",
                        actionSuccess = true,
                        isStreaming = false
                    )
                )
            }
            return
        }

        activeJob?.cancel()
        val existingTurns = _state.value.activeTurns
        val isRegenerate = replaceTurnIndex != null && replaceTurnIndex in existingTurns.indices
        val historySnapshot = (if (isRegenerate) existingTurns.take(replaceTurnIndex!!) else existingTurns)
            .filter { !it.isStreaming && it.response.isNotBlank() }
            .map { it.prompt to it.response }
        val turnIndex = if (isRegenerate) replaceTurnIndex!! else existingTurns.size
        val currentPersona = _state.value.selectedPersona
        val currentSettings = _state.value.settings
        val isIncognito = _state.value.incognitoMode

        _state.update {
            val updatedList = if (isRegenerate) {
                it.activeTurns.toMutableList().apply {
                    this[turnIndex] = this[turnIndex].copy(
                        response = "",
                        isStreaming = true,
                        actionSummary = null
                    )
                }
            } else {
                it.activeTurns + ConversationTurn(
                    prompt = effectivePrompt,
                    response = "",
                    image = image,
                    isStreaming = true
                )
            }
            it.copy(
                activeTurns = updatedList,
                isGenerating = true,
                statusDetail = null
            )
        }

        val runner = agentTools?.let { AgentRunner(engine, it) }

        activeJob = viewModelScope.launch {
            if (runner == null) {
                val rawBuilder = StringBuilder()
                engine.streamResponse(userQuery = effectivePrompt, image = image, thinkHarder = thinkHarder, settings = currentSettings)
                    .catch { error ->
                        _state.update { state ->
                            val updatedTurns = state.activeTurns.toMutableList()
                            if (turnIndex in updatedTurns.indices) {
                                updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                    isStreaming = false,
                                    response = updatedTurns[turnIndex].response.ifEmpty {
                                        error.localizedMessage ?: "Failed to generate response."
                                    }
                                )
                            }
                            state.copy(activeTurns = updatedTurns, isGenerating = false, statusDetail = error.localizedMessage)
                        }
                    }
                    .onCompletion { cause ->
                        if (cause == null) {
                            val finalReply = rawBuilder.toString().trim()
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = finalReply, isStreaming = false)
                                }
                                state.copy(activeTurns = updatedTurns, isGenerating = false)
                            }
                            chatHistoryStore?.saveChat(prompt = effectivePrompt, response = finalReply, actionSummary = null)
                            refreshChats()
                        }
                    }
                    .collect { chunk ->
                        rawBuilder.append(chunk)
                        _state.update { state ->
                            val updatedTurns = state.activeTurns.toMutableList()
                            if (turnIndex in updatedTurns.indices) {
                                updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = rawBuilder.toString())
                            }
                            state.copy(activeTurns = updatedTurns)
                        }
                    }
                return@launch
            }

            runner.run(
                userPrompt = effectivePrompt,
                image = image,
                conversationHistory = historySnapshot,
                persona = currentPersona,
                thinkHarder = thinkHarder,
                settings = currentSettings,
                incognito = isIncognito,
                confirmDangerousTool = { toolName, detail ->
                    val deferred = CompletableDeferred<Boolean>()
                    _state.update {
                        it.copy(
                            pendingToolConfirmation = PendingToolConfirmation(
                                toolName = toolName,
                                detail = detail,
                                onDecision = { allowed ->
                                    _state.update { s -> s.copy(pendingToolConfirmation = null) }
                                    deferred.complete(allowed)
                                }
                            )
                        )
                    }
                    deferred.await()
                }
            )
                .catch { error ->
                    _state.update { state ->
                        val updatedTurns = state.activeTurns.toMutableList()
                        if (turnIndex in updatedTurns.indices) {
                            updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                isStreaming = false,
                                response = updatedTurns[turnIndex].response.ifEmpty {
                                    error.localizedMessage ?: "Failed to generate response."
                                }
                            )
                        }
                        state.copy(
                            activeTurns = updatedTurns,
                            isGenerating = false,
                            statusDetail = error.localizedMessage
                        )
                    }
                }
                .collect { event ->
                    when (event) {
                        is AgentStreamEvent.PartialText -> {
                            val hw = compatibilityChecker?.getHardwareState()
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = event.text)
                                }
                                state.copy(
                                    activeTurns = updatedTurns,
                                    nerdStats = event.stats,
                                    deviceTempC = hw?.batteryTempC ?: state.deviceTempC,
                                    thermalLabel = hw?.thermalLabel ?: state.thermalLabel,
                                    isThermallyThrottled = hw?.isThermallyThrottled ?: state.isThermallyThrottled
                                )
                            }
                        }
                        is AgentStreamEvent.RunningTool -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                        actionSummary = "Running ${event.toolName.replace('_', ' ')}...",
                                        actionSuccess = true
                                    )
                                }
                                state.copy(activeTurns = updatedTurns)
                            }
                        }
                        is AgentStreamEvent.ToolCompleted -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                        actionSummary = event.summary,
                                        actionSuccess = event.success
                                    )
                                }
                                state.copy(activeTurns = updatedTurns)
                            }
                        }
                        is AgentStreamEvent.Finished -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    val prevTurn = updatedTurns[turnIndex]
                                    val newVersions = (prevTurn.versions + event.finalText).distinct()
                                    updatedTurns[turnIndex] = prevTurn.copy(
                                        response = event.finalText,
                                        responseImage = event.attachedImage,
                                        actionSummary = event.actionSummary,
                                        actionSuccess = event.actionSuccess,
                                        isStreaming = false,
                                        versions = newVersions,
                                        selectedVersionIndex = (newVersions.size - 1).coerceAtLeast(0)
                                    )
                                }
                                state.copy(
                                    activeTurns = updatedTurns,
                                    isGenerating = false,
                                    nerdStats = event.stats
                                )
                            }
                            if (!_state.value.incognitoMode) {
                                chatHistoryStore?.saveChat(
                                    prompt = effectivePrompt,
                                    response = event.finalText,
                                    actionSummary = event.actionSummary,
                                    sessionId = activeSessionId
                                )
                                refreshChats()
                                refreshMemories()
                            }
                        }
                    }
                }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            if (_state.value.settings.usesNativeModel) {
                _state.update { it.copy(availability = ModelAvailability.UNAVAILABLE, availableAiCoreModels = emptyList(), statusDetail = null) }
                return@launch
            }
            _state.update { it.copy(availability = ModelAvailability.CHECKING, statusDetail = null) }
            val profile = _state.value.settings.aiCoreModelProfile
            val models = engine.discoverAiCoreModels()
            val status = engine.queryAvailability(profile)
            if (_state.value.settings.usesNativeModel) return@launch
            _state.update { it.copy(availability = status, availableAiCoreModels = models) }
            if (status == ModelAvailability.AVAILABLE) {
                engine.warmupModel(profile)
            }
            runCompatibilityChecks()
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            val profile = _state.value.settings.aiCoreModelProfile
            _state.update { it.copy(availability = ModelAvailability.DOWNLOADING, statusDetail = "Starting download...") }
            engine.downloadModel(profile)
                .catch { error ->
                    _state.update {
                        it.copy(
                            availability = ModelAvailability.UNAVAILABLE,
                            statusDetail = error.localizedMessage ?: "Download failed."
                        )
                    }
                }
                .collect { downloadStatus ->
                    when (downloadStatus) {
                        is DownloadStatus.DownloadStarted -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.DOWNLOADING,
                                    totalDownloadBytes = downloadStatus.bytesToDownload,
                                    statusDetail = "Downloading Gemma 4..."
                                )
                            }
                        }
                        is DownloadStatus.DownloadProgress -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.DOWNLOADING,
                                    downloadProgressBytes = downloadStatus.totalBytesDownloaded
                                )
                            }
                        }
                        DownloadStatus.DownloadCompleted -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.AVAILABLE,
                                    statusDetail = "Gemma 4 is ready on-device."
                                )
                            }
                        }
                        is DownloadStatus.DownloadFailed -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.UNAVAILABLE,
                                    statusDetail = downloadStatus.e.localizedMessage ?: "Download failed."
                                )
                            }
                        }
                    }
                }
        }
    }

    fun warmupModel() {
        viewModelScope.launch {
            _state.update { it.copy(isWarmingUp = true, statusDetail = "Warming up Gemma 4 in AICore...") }
            engine.warmupModel(_state.value.settings.aiCoreModelProfile)
            _state.update { it.copy(isWarmingUp = false, statusDetail = "Model warmed up and ready.") }
        }
    }

    override fun onCleared() {
        activeJob?.cancel()
        engine.shutdown()
        super.onCleared()
    }
}
