package com.kairo.agent.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectToolRouterTest {
    @Test fun exactReadOnlyRequests() {
        assertEquals("get_device_status", directToolForQuery("Check my battery & status")?.name)
        assertEquals("get_current_time", directToolForQuery("Quelle heure est-il ?")?.name)
        assertEquals("get_storage_and_memory", directToolForQuery("Check my storage and RAM")?.name)
        assertEquals(DirectTool("calculate_or_convert", mapOf("expression" to "12 * 8")), directToolForQuery("Calculate 12 * 8"))
    }

    @Test fun explicitBoundedActions() {
        assertEquals(DirectTool("set_timer", mapOf("seconds" to 300)), directToolForQuery("Set a 5 minute timer"))
        assertEquals(DirectTool("set_timer", mapOf("seconds" to 120)), directToolForQuery("Lance un minuteur de 2 minutes"))
        assertEquals(DirectTool("set_alarm", mapOf("hour" to 7, "minute" to 30)), directToolForQuery("Set an alarm at 07:30"))
        assertEquals(DirectTool("open_settings", mapOf("setting" to "wifi")), directToolForQuery("Open WiFi settings"))
    }

    @Test fun noInferredOrCompoundActions() {
        listOf("Set an alarm at 25:00", "Start a 9999 hours timer", "Set an alarm",
            "Check my battery and send a message", "Open WiFi settings then change the password",
            "Run a shell command", "Explain how batteries work", "How does a timer work?",
            "What is RAM?", "What time is it in Tokyo?", "get_device_status")
            .forEach { assertNull(it, directToolForQuery(it)) }
    }
}
