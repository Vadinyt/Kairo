package com.kairo.agent.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectDeviceStatusQueryTest {
    @Test fun explicitStatusRequests() {
        listOf("Check my battery & status", "Check my battery", "What's my battery level?",
            "Show me device status", "Vérifie ma batterie", "Quel est le niveau de batterie ?")
            .forEach { assertTrue(it, isDirectDeviceStatusQuery(it)) }
    }
    @Test fun unrelatedAndCompoundRequestsAreNotDispatched() {
        listOf("Explain how batteries work", "Check my battery and send a message",
            "Check my battery; open settings", "What is a battery?", "get_device_status",
            "Check my battery and run a shell command")
            .forEach { assertFalse(it, isDirectDeviceStatusQuery(it)) }
    }
}
