package com.kairo.agent.engine

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class NativeInferenceStats(
    val promptTokens: Int,
    val promptMs: Double,
    val generatedTokens: Int,
    val generationMs: Double,
    val processPssMb: Long
) {
    val tokensPerSecond: Float
        get() = if (generationMs > 0) (generatedTokens * 1000.0 / generationMs).toFloat() else 0f
}

internal object NativeLlama {
    private val lock = Mutex()
    private val libraryLoaded by lazy {
        System.loadLibrary("kairo_llama")
        true
    }

    private external fun nativeLoad(path: String, threads: Int, contextTokens: Int)
    private external fun nativePrepare(prompt: ByteArray, temperature: Float, maxTokens: Int)
    private external fun nativeNext(): ByteArray?
    private external fun nativeMetrics(): DoubleArray?
    private external fun nativeUnload()

    fun generate(
        modelPath: String,
        prompt: String,
        temperature: Float,
        threads: Int,
        contextTokens: Int,
        maxTokens: Int,
        onStats: (NativeInferenceStats) -> Unit
    ): Flow<String> = flow {
        lock.withLock {
            check(File(modelPath).isFile) { "Imported GGUF not found. Import again in Settings." }
            try {
                check(libraryLoaded)
            } catch (e: UnsatisfiedLinkError) {
                throw IllegalStateException("Embedded llama.cpp is unavailable in this APK.", e)
            }
            currentCoroutineContext().ensureActive()
            nativeLoad(modelPath, threads.coerceIn(2, 6).coerceAtMost(Runtime.getRuntime().availableProcessors().coerceAtLeast(1)),
                if (contextTokens == 2048) 2048 else 4096)
            currentCoroutineContext().ensureActive()
            nativePrepare(prompt.toByteArray(Charsets.UTF_8), temperature, maxTokens.coerceIn(128, 512))
            currentCoroutineContext().ensureActive()
            val pssMb = Debug.getPss() / 1024L
            val output = ByteArrayOutputStream()
            var emitted = 0
            var chunks = 0
            val generationStartNs = SystemClock.elapsedRealtimeNanos()
            fun report(): NativeInferenceStats? {
                val m = nativeMetrics() ?: return null
                if (m.size != 4) return null
                val elapsedMs = (SystemClock.elapsedRealtimeNanos() - generationStartNs) / 1_000_000.0
                val generationMs = if (m[3] > 0.0 && m[2].toInt() == chunks) m[3] else elapsedMs
                return NativeInferenceStats(m[0].toInt(), m[1], chunks, generationMs, pssMb).also(onStats)
            }
            report()
            // Tokens can split UTF-8 characters; only emit completed text.
            while (true) {
                currentCoroutineContext().ensureActive()
                val piece = nativeNext() ?: break
                output.write(piece)
                chunks++
                if (chunks == 1 || chunks % 8 == 0) report()
                val text = output.toByteArray().toString(Charsets.UTF_8).trimEnd('\uFFFD')
                if (text.length > emitted) {
                    emit(text.substring(emitted))
                    emitted = text.length
                }
            }
            val tail = output.toByteArray().toString(Charsets.UTF_8)
            if (tail.length > emitted) emit(tail.substring(emitted))
            report()?.let {
                Log.i("KairoNative", "prompt=" + it.promptTokens + " tokens / " + it.promptMs +
                    " ms; generation=" + it.generatedTokens + " tokens / " + it.generationMs +
                    " ms; " + it.tokensPerSecond + " tok/s; PSS=" + it.processPssMb + " MB")
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun release() = withContext(Dispatchers.IO) {
        lock.withLock {
            if (android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a")) {
                runCatching { if (libraryLoaded) nativeUnload() }
            }
            Unit
        }
    }
}
