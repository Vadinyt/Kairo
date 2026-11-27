package com.kairo.agent.engine

import android.graphics.Bitmap
import android.util.Base64
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.ModelReleaseStage
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.AiCoreModelProfile
import com.kairo.agent.data.EngineBackendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

enum class ModelAvailability {
    CHECKING,
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE
}

data class AiCoreModelOption(
    val profile: AiCoreModelProfile,
    val label: String,
    val baseModelName: String?,
    val availability: ModelAvailability
)

/**
 * Unified local inference engine supporting:
 * 1) Android AICore (Gemma 4 / Gemini Nano via ML Kit GenAI Prompt API, across Stable & Preview Fast/Full models)
 * 2) Local OpenAI-compatible HTTP endpoint (for non-AICore devices running local models like llama.cpp / Ollama)
 */
class GemmaAssistantEngine {

    private val clients = ConcurrentHashMap<AiCoreModelProfile, GenerativeModel>()

    @Volatile
    internal var nativeStats: NativeInferenceStats? = null
        private set

    private fun getOrCreateClient(profile: AiCoreModelProfile): GenerativeModel {
        return clients.getOrPut(profile) {
            when (profile) {
                AiCoreModelProfile.AUTO -> Generation.getClient()
                AiCoreModelProfile.STABLE_FULL -> Generation.getClient(
                    generationConfig {
                        modelConfig = modelConfig {
                            releaseStage = ModelReleaseStage.STABLE
                            preference = ModelPreference.FULL
                        }
                    }
                )
                AiCoreModelProfile.STABLE_FAST -> Generation.getClient(
                    generationConfig {
                        modelConfig = modelConfig {
                            releaseStage = ModelReleaseStage.STABLE
                            preference = ModelPreference.FAST
                        }
                    }
                )
                AiCoreModelProfile.PREVIEW_FULL -> Generation.getClient(
                    generationConfig {
                        modelConfig = modelConfig {
                            releaseStage = ModelReleaseStage.PREVIEW
                            preference = ModelPreference.FULL
                        }
                    }
                )
                AiCoreModelProfile.PREVIEW_FAST -> Generation.getClient(
                    generationConfig {
                        modelConfig = modelConfig {
                            releaseStage = ModelReleaseStage.PREVIEW
                            preference = ModelPreference.FAST
                        }
                    }
                )
            }
        }
    }

    private fun mapStatus(status: Int?): ModelAvailability = when (status) {
        FeatureStatus.AVAILABLE -> ModelAvailability.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> ModelAvailability.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> ModelAvailability.DOWNLOADING
        else -> ModelAvailability.UNAVAILABLE
    }

    suspend fun discoverAiCoreModels(): List<AiCoreModelOption> {
        val concreteProfiles = listOf(
            AiCoreModelProfile.STABLE_FULL,
            AiCoreModelProfile.STABLE_FAST,
            AiCoreModelProfile.PREVIEW_FULL,
            AiCoreModelProfile.PREVIEW_FAST
        )
        return concreteProfiles.map { profile ->
            val client = getOrCreateClient(profile)
            val status = runCatching { client.checkStatus() }.getOrNull()
            val availability = mapStatus(status)
            val baseName = if (availability != ModelAvailability.UNAVAILABLE) {
                runCatching { client.getBaseModelName() }.getOrNull()?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            AiCoreModelOption(
                profile = profile,
                label = profile.label,
                baseModelName = baseName,
                availability = availability
            )
        }
    }

    private suspend fun resolveClient(
        profile: AiCoreModelProfile,
        thinkHarder: Boolean = false
    ): Pair<GenerativeModel, Int?> {
        if (profile != AiCoreModelProfile.AUTO) {
            val client = getOrCreateClient(profile)
            val status = runCatching { client.checkStatus() }.getOrNull()
            return client to status
        }

        val priority = if (thinkHarder) {
            listOf(
                AiCoreModelProfile.STABLE_FULL,
                AiCoreModelProfile.PREVIEW_FULL,
                AiCoreModelProfile.STABLE_FAST,
                AiCoreModelProfile.PREVIEW_FAST
            )
        } else {
            listOf(
                AiCoreModelProfile.STABLE_FULL,
                AiCoreModelProfile.STABLE_FAST,
                AiCoreModelProfile.PREVIEW_FULL,
                AiCoreModelProfile.PREVIEW_FAST
            )
        }

        var bestFallback: Pair<GenerativeModel, Int?>? = null
        for (candidate in priority) {
            val client = getOrCreateClient(candidate)
            val status = runCatching { client.checkStatus() }.getOrNull()
            if (status == FeatureStatus.AVAILABLE) {
                return client to status
            }
            if (bestFallback == null && (status == FeatureStatus.DOWNLOADABLE || status == FeatureStatus.DOWNLOADING)) {
                bestFallback = client to status
            }
        }
        return bestFallback ?: (getOrCreateClient(AiCoreModelProfile.AUTO) to runCatching {
            getOrCreateClient(AiCoreModelProfile.AUTO).checkStatus()
        }.getOrNull())
    }

    suspend fun queryAvailability(profile: AiCoreModelProfile = AiCoreModelProfile.AUTO): ModelAvailability {
        if (profile != AiCoreModelProfile.AUTO) {
            val status = runCatching { getOrCreateClient(profile).checkStatus() }.getOrNull()
            return mapStatus(status)
        }
        val options = discoverAiCoreModels()
        return when {
            options.any { it.availability == ModelAvailability.AVAILABLE } -> ModelAvailability.AVAILABLE
            options.any { it.availability == ModelAvailability.DOWNLOADING } -> ModelAvailability.DOWNLOADING
            options.any { it.availability == ModelAvailability.DOWNLOADABLE } -> ModelAvailability.DOWNLOADABLE
            else -> mapStatus(runCatching { getOrCreateClient(AiCoreModelProfile.AUTO).checkStatus() }.getOrNull())
        }
    }

    fun downloadModel(profile: AiCoreModelProfile = AiCoreModelProfile.AUTO): Flow<DownloadStatus> {
        val target = if (profile == AiCoreModelProfile.AUTO) AiCoreModelProfile.STABLE_FULL else profile
        return getOrCreateClient(target).download()
    }

    suspend fun warmupModel(profile: AiCoreModelProfile = AiCoreModelProfile.AUTO) {
        val (client, _) = resolveClient(profile)
        runCatching { client.warmup() }
    }

    fun streamResponse(
        userQuery: String,
        image: Bitmap? = null,
        thinkHarder: Boolean = false,
        settings: AgentSettings = AgentSettings()
    ): Flow<String> {
        nativeStats = null
        if (settings.usesNativeModel) {
            if (image != null) return flow {
                throw IllegalArgumentException("The embedded GGUF engine is text-only. Disable it in Settings to use another image-capable backend.")
            }
            return NativeLlama.generate(
                modelPath = settings.nativeModelPath,
                prompt = userQuery,
                temperature = if (thinkHarder) 0.2f else 0.4f,
                threads = settings.nativeThreads,
                contextTokens = settings.nativeContextTokens,
                maxTokens = settings.nativeMaxTokens,
                onStats = { nativeStats = it }
            )
        }
        if (settings.backendMode == EngineBackendMode.CLOUD_AI) {
            return streamFromLocalHttp(
                userQuery = userQuery,
                image = image,
                endpointUrl = settings.cloudEndpointUrl,
                modelName = settings.cloudModelName,
                temperatureValue = if (thinkHarder) 0.2f else 0.4f,
                apiKey = settings.cloudApiKey
            )
        }

        val useLocalHttp = when (settings.backendMode) {
            EngineBackendMode.LOCAL_HTTP -> true
            EngineBackendMode.AICORE, EngineBackendMode.CLOUD_AI -> false
            EngineBackendMode.AUTO -> false // handled dynamically below
        }

        if (useLocalHttp) {
            return streamFromLocalHttp(
                userQuery = userQuery,
                image = image,
                endpointUrl = settings.localEndpointUrl,
                modelName = settings.localModelName,
                temperatureValue = if (thinkHarder) 0.2f else 0.4f
            )
        }

        return callbackFlow {
            val (activeClient, status) = resolveClient(
                profile = settings.aiCoreModelProfile,
                thinkHarder = thinkHarder
            )

            if (settings.backendMode == EngineBackendMode.AUTO && status != FeatureStatus.AVAILABLE) {
                // Fallback to local HTTP endpoint if AICore isn't available on this device
                val localJob = launch {
                    try {
                        streamFromLocalHttp(
                            userQuery = userQuery,
                            image = image,
                            endpointUrl = settings.localEndpointUrl,
                            modelName = settings.localModelName,
                            temperatureValue = if (thinkHarder) 0.2f else 0.4f
                        ).collect { trySend(it) }
                        close()
                    } catch (t: Throwable) {
                        close(t)
                    }
                }
                awaitClose { localJob.cancel() }
                return@callbackFlow
            }

            val temp = if (thinkHarder) 0.2f else 0.4f
            val promptRequest = if (image != null) {
                generateContentRequest(ImagePart(image), TextPart(userQuery)) {
                    temperature = temp
                    if (thinkHarder) enableThinking = true
                }
            } else {
                generateContentRequest(TextPart(userQuery)) {
                    temperature = temp
                    if (thinkHarder) enableThinking = true
                }
            }

            val callback = StreamingCallback { newText ->
                if (newText.isNotEmpty()) {
                    trySend(newText)
                }
            }
            val job = launch {
                try {
                    activeClient.generateContent(promptRequest, callback)
                    close()
                } catch (t: Throwable) {
                    val msg = t.message.orEmpty()
                    if (msg.contains("606") || msg.contains("FEATURE_NOT_FOUND")) {
                        close(
                            IllegalStateException(
                                "AICore model feature unavailable (${settings.aiCoreModelProfile.label}). " +
                                    "Try switching the AICore Model in the top model menu (e.g. Preview • Fast / Preview • Full) or use Local HTTP.\n\nDetails: $msg",
                                t
                            )
                        )
                    } else {
                        close(t)
                    }
                }
            }
            awaitClose { job.cancel() }
        }
    }

    private fun streamFromLocalHttp(
        userQuery: String,
        image: Bitmap?,
        endpointUrl: String,
        modelName: String,
        temperatureValue: Float,
        apiKey: String = ""
    ): Flow<String> = flow {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            // Slow local CPUs can spend more than a minute evaluating a prompt.
            readTimeout = if (apiKey.isBlank()) 180_000 else 60_000
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
        }

        try {
            val contentArray = JSONArray().apply {
                put(JSONObject().put("type", "text").put("text", userQuery))
                if (image != null) {
                    val baos = ByteArrayOutputStream()
                    image.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                    put(
                        JSONObject().put("type", "image_url")
                            .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64"))
                    )
                }
            }

            val payload = JSONObject().apply {
                put("model", modelName)
                put("stream", true)
                put("temperature", temperatureValue.toDouble())
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().put("role", "user").put("content", contentArray)
                    )
                )
            }

            conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                throw IllegalStateException("Local model endpoint error ($code): $err")
            }

            conn.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("data:")) continue
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val delta = runCatching {
                        JSONObject(data).optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                    }.getOrNull() ?: continue
                    val content = delta.opt("content") as? String ?: continue
                    if (content.isNotEmpty()) emit(content)
                }
            }
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    fun shutdown() {
        clients.values.forEach { runCatching { it.close() } }
        clients.clear()
    }
}
