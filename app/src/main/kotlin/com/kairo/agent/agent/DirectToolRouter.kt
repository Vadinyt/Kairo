package com.kairo.agent.agent

internal data class DirectTool(val name: String, val args: Map<String, Any> = emptyMap())

// Pure, anchored matching. No inferred arguments, model-generated names,
// shell commands, private data requests or third-party calls.
private val TIME = Regex("""^(?:what(?:'s| is) (?:the )?(?:current )?(?:time|date)(?: now| today)?|what time is it|what day is it|current (?:time|date)|today'?s date|quelle heure est-il|quelle est la date|quel jour sommes-nous)$""", RegexOption.IGNORE_CASE)
private val STORAGE = Regex("""^(?:(?:check|show(?: me)?|tell me|give me)\s+)?(?:my\s+|the\s+)?(?:(?:free|available)\s+)?(?:storage(?: space)?|ram|memory usage|storage\s*(?:&|and)\s*ram|ram\s*(?:&|and)\s*storage)(?:\s+left)?$""", RegexOption.IGNORE_CASE)
private val STORAGE_FR = Regex("""^(?:affiche|v[ée]rifie|donne-moi)\s+(?:le |la |mon |ma )?(?:stockage|ram|m[ée]moire disponible|stockage et ram)$""", RegexOption.IGNORE_CASE)
private val CALC = Regex("""^(?:(?:calculate|compute|calcule|combien font)\s+)?(-?\d{1,9}(?:\.\d{1,6})?\s*[+\-*/%]\s*-?\d{1,9}(?:\.\d{1,6})?)\s*=?$""", RegexOption.IGNORE_CASE)
private val CONVERSION = Regex("""^(?:(?:convert|convertis)\s+)?(-?\d{1,9}(?:\.\d{1,6})?\s*(?:°?[cf]|km|mi|kg|lbs?|cm|in)\s*(?:to|in)\s*(?:°?[cf]|km|mi|kg|lbs?|cm|in))$""", RegexOption.IGNORE_CASE)
private val TIMER = Regex("""^(?:set|start|create)\s+(?:a\s+)?(?:(\d{1,4})\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)\s+timer|timer\s+(?:for\s+)?(\d{1,4})\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?))$""", RegexOption.IGNORE_CASE)
private val TIMER_FR = Regex("""^(?:lance|d[ée]marre|mets)\s+(?:un\s+)?minuteur\s+(?:de\s+)?(\d{1,4})\s*(secondes?|minutes?|heures?)$""", RegexOption.IGNORE_CASE)
private val ALARM = Regex("""^(?:set|create)\s+(?:an?\s+)?alarm\s+(?:for|at)\s+(\d{1,2}):(\d{2})$""", RegexOption.IGNORE_CASE)
private val ALARM_FR = Regex("""^(?:mets|programme|cr[ée]e)\s+(?:une\s+)?alarme\s+[àa]\s+(\d{1,2})(?::|h)(\d{2})$""", RegexOption.IGNORE_CASE)
private val SETTINGS = Regex("""^open\s+(?:(wifi|wi-fi|bluetooth|display|sound|battery)\s+)?settings$""", RegexOption.IGNORE_CASE)
private val CLOCK = Regex("""^(?:open\s+(?:the\s+)?(?:clock|alarms)|ouvre\s+(?:l['’]horloge|les alarmes))$""", RegexOption.IGNORE_CASE)

internal fun directToolForQuery(query: String): DirectTool? {
    val text = query.trim().trimEnd('?', '.', '!').trim()
    if (text.isEmpty() || text.length > 180) return null
    if (isDirectDeviceStatusQuery(text)) return DirectTool("get_device_status")
    if (TIME.matches(text)) return DirectTool("get_current_time")
    if (STORAGE.matches(text) || STORAGE_FR.matches(text)) return DirectTool("get_storage_and_memory")
    if (CLOCK.matches(text)) return DirectTool("open_clock")
    SETTINGS.matchEntire(text)?.let {
        return DirectTool("open_settings", mapOf("setting" to it.groupValues[1].ifBlank { "general" }.lowercase()))
    }
    (CALC.matchEntire(text) ?: CONVERSION.matchEntire(text))?.let {
        return DirectTool("calculate_or_convert", mapOf("expression" to it.groupValues[1]))
    }
    val timer = TIMER.matchEntire(text)
    val frTimer = TIMER_FR.matchEntire(text)
    if (timer != null || frTimer != null) {
        val number = (frTimer?.groupValues?.get(1) ?: timer!!.groupValues[1].ifBlank { timer.groupValues[3] }).toIntOrNull() ?: return null
        val unit = (frTimer?.groupValues?.get(2) ?: timer!!.groupValues[2].ifBlank { timer.groupValues[4] }).lowercase()
        val multiplier = when {
            unit.startsWith("s") -> 1
            unit.startsWith("m") -> 60
            unit.startsWith("h") -> 3600
            else -> return null
        }
        val seconds = number * multiplier
        if (seconds !in 1..86400) return null
        return DirectTool("set_timer", mapOf("seconds" to seconds))
    }
    (ALARM.matchEntire(text) ?: ALARM_FR.matchEntire(text))?.let {
        val hour = it.groupValues[1].toInt()
        val minute = it.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return DirectTool("set_alarm", mapOf("hour" to hour, "minute" to minute))
    }
    return null
}
