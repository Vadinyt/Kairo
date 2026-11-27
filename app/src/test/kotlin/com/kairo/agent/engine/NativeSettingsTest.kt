package com.kairo.agent.engine

import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.EngineBackendMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSettingsTest {
    @Test fun requiresEnabledAndImportedPath() {
        assertFalse(AgentSettings().usesNativeModel)
        assertFalse(AgentSettings(nativeModelEnabled = true).usesNativeModel)
        assertTrue(AgentSettings(nativeModelEnabled = true, nativeModelPath = "/test/model.gguf").usesNativeModel)
    }
    @Test fun explicitCloudAndAiCoreAreNotOverridden() {
        val s = AgentSettings(nativeModelEnabled = true, nativeModelPath = "/test/model.gguf")
        assertFalse(s.copy(backendMode = EngineBackendMode.CLOUD_AI).usesNativeModel)
        assertFalse(s.copy(backendMode = EngineBackendMode.AICORE).usesNativeModel)
        assertTrue(s.copy(backendMode = EngineBackendMode.LOCAL_HTTP).usesNativeModel)
    }
}
