package com.kairo.agent.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightweightChatQueryTest {
    @Test fun standaloneChatUsesLightweightPrompt() {
        listOf("Hello", "Salut!", "Explique-moi comment fonctionne un processeur",
            "What is photosynthesis?", "How does a CPU work?", "Pourquoi le ciel est bleu ?")
            .forEach { assertTrue(it, isLightweightChatQuery(it)) }
    }

    @Test fun actionsAndPersonalContextKeepFullPrompt() {
        listOf("Open Spotify", "What is my battery level?", "Explique-moi ma batterie",
            "Quelle heure est-il ?", "What is the weather today?", "Explain /open_app",
            "Remember that I like coffee", "Envoie un SMS")
            .forEach { assertFalse(it, isLightweightChatQuery(it)) }
    }
}
