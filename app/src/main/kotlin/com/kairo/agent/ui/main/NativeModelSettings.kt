package com.kairo.agent.ui.main

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.EngineBackendMode
import com.kairo.agent.engine.NativeLlama
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_MODEL_BYTES = 4L * 1024 * 1024 * 1024
private data class ImportedModel(val path: String, val name: String)

private suspend fun importGguf(context: Context, uri: Uri): ImportedModel = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var displayName = "Imported model.gguf"
    var expectedSize: Long? = null
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val nameColumn = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeColumn = c.getColumnIndex(OpenableColumns.SIZE)
            if (nameColumn >= 0 && !c.isNull(nameColumn)) displayName = c.getString(nameColumn)
            if (sizeColumn >= 0 && !c.isNull(sizeColumn)) expectedSize = c.getLong(sizeColumn)
        }
    }
    val dir = File(context.noBackupFilesDir, "gguf").apply { mkdirs() }
    expectedSize?.takeIf { it > 0 }?.let {
        require(it in 32L..MAX_MODEL_BYTES) { "Choose a GGUF smaller than 4 GiB." }
        require(dir.usableSpace > it + 64L * 1024 * 1024) { "Not enough free storage." }
    }
    val id = UUID.randomUUID().toString()
    val temporary = File(dir, "$id.part")
    val target = File(dir, "$id.gguf")
    try {
        val input = resolver.openInputStream(uri) ?: error("Unable to open model.")
        input.use { source ->
            val header = ByteArray(4)
            var read = 0
            while (read < header.size) {
                val n = source.read(header, read, header.size - read)
                require(n > 0) { "Empty or invalid model file." }
                read += n
            }
            require(header.contentEquals(byteArrayOf(71, 71, 85, 70))) { "This file is not a GGUF model." }
            temporary.outputStream().use { out ->
                out.write(header)
                val buffer = ByteArray(256 * 1024)
                var total = 4L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = source.read(buffer)
                    if (n < 0) break
                    total += n
                    require(total <= MAX_MODEL_BYTES) { "Model exceeds 4 GiB." }
                    out.write(buffer, 0, n)
                }
                require(total >= 32) { "Incomplete GGUF file." }
                expectedSize?.takeIf { it > 0 }?.let { require(it == total) { "Model copy is incomplete." } }
            }
        }
        check(temporary.renameTo(target)) { "Unable to finish importing model." }
        ImportedModel(target.absolutePath, displayName.take(120))
    } finally {
        temporary.delete()
    }
}

@Composable
internal fun NativeModelSettings(
    settings: AgentSettings,
    isGenerating: Boolean,
    onUpdateSettings: (AgentSettings) -> Unit
) {
    val context = LocalContext.current.applicationContext
    val currentSettings by rememberUpdatedState(settings)
    val update by rememberUpdatedState(onUpdateSettings)
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !importing) {
            importing = true
            errorMessage = null
            scope.launch {
                try {
                    val model = importGguf(context, uri)
                    val previous = currentSettings.nativeModelPath
                    update(currentSettings.copy(
                        nativeModelPath = model.path, nativeModelName = model.name, nativeModelEnabled = true,
                        backendMode = EngineBackendMode.AUTO, bypassCompatibility = true
                    ))
                    if (previous.isNotBlank() && previous != model.path) withContext(Dispatchers.IO) {
                        val old = File(previous)
                        if (old.parentFile == File(context.noBackupFilesDir, "gguf")) old.delete()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage ?: "Model import failed."
                } finally {
                    importing = false
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("On-device GGUF (experimental)", color = Color(0xFFA8C7FA), style = MaterialTheme.typography.labelLarge)
        Text("Runs llama.cpp inside Kairo. No Termux, HTTP server or root required. Text-only; import a single instruction-tuned GGUF, preferably 1–3B Q4.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9BDC4))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Use embedded model", color = Color(0xFFE3E3E3))
            Switch(
                checked = settings.usesNativeModel,
                enabled = settings.nativeModelPath.isNotBlank() && !importing && !isGenerating,
                onCheckedChange = { enabled ->
                    update(currentSettings.copy(nativeModelEnabled = enabled,
                        backendMode = if (enabled) EngineBackendMode.AUTO else currentSettings.backendMode,
                        bypassCompatibility = if (enabled) true else currentSettings.bypassCompatibility))
                    if (!enabled) scope.launch { NativeLlama.release() }
                }
            )
        }
        Text(settings.nativeModelName.ifBlank { "No GGUF imported" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9BDC4))
        Button(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !importing && !isGenerating, modifier = Modifier.fillMaxWidth()) {
            Text(if (importing) "Importing model…" else "Import GGUF model")
        }
        if (importing) CircularProgressIndicator()
        if (settings.nativeModelPath.isNotBlank()) OutlinedButton(
            enabled = !importing && !isGenerating,
            onClick = {
                val previous = currentSettings.nativeModelPath
                update(currentSettings.copy(nativeModelEnabled = false, nativeModelPath = "", nativeModelName = ""))
                scope.launch {
                    NativeLlama.release()
                    withContext(Dispatchers.IO) {
                        val old = File(previous)
                        if (old.parentFile == File(context.noBackupFilesDir, "gguf")) old.delete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Remove imported model") }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
