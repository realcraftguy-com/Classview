package com.realcraftguy.classview.server

/**
 * Gleitendes Zeitfenster pro Schlüssel (z. B. "join-fail:ip:1.2.3.4"). Liegt nur im Speicher und beginnt
 * nach einem Neustart neu – für einen Prototyp ausreichend, siehe docs/security.md.
 */
class RateLimiter(private val clock: () -> Long) {
    private val hits = HashMap<String, ArrayDeque<Long>>()

    /** Anzahl Treffer im Fenster. */
    @Synchronized
    fun count(key: String, windowMs: Long): Int {
        val q = hits[key] ?: return 0
        prune(q, windowMs)
        return q.size
    }

    /** Zeichnet einen Treffer auf. */
    @Synchronized
    fun record(key: String) {
        hits.getOrPut(key) { ArrayDeque() }.addLast(clock())
        if (hits.size > 20_000) cleanup()
    }

    /** Millisekunden, bis wieder ein Treffer unter [limit] liegt; 0, wenn das Limit nicht erreicht ist. */
    @Synchronized
    fun retryAfter(key: String, limit: Int, windowMs: Long): Long {
        val q = hits[key] ?: return 0
        prune(q, windowMs)
        if (q.size < limit) return 0
        return (q.elementAt(q.size - limit) + windowMs - clock()).coerceAtLeast(1)
    }

    private fun prune(q: ArrayDeque<Long>, windowMs: Long) {
        val cutoff = clock() - windowMs
        while (q.isNotEmpty() && q.first() <= cutoff) q.removeFirst()
    }

    private fun cleanup() {
        val cutoff = clock() - 60 * 60 * 1000L
        hits.entries.removeAll { (_, q) -> q.isEmpty() || q.last() <= cutoff }
    }
}

/** Alle Grenzwerte an einer Stelle. Alle Geräte einer Schule teilen sich meist eine öffentliche IP. */
object Limits {
    const val MINUTE = 60_000L
    const val TEN_MINUTES = 10 * MINUTE
    const val HOUR = 60 * MINUTE

    /**
     * Fehlgeschlagene Beitritte je IP, gegen Durchprobieren. Nicht zu streng: Tippfehler einer ganzen Klasse
     * hinter einer Schul-IP zählen zusammen. Bei 31⁶ ≈ 887 Mio. Codes sind 30 Versuche je 10 Minuten
     * trotzdem aussichtslos.
     */
    const val JOIN_FAILURES_PER_IP = 30          // pro 10 Minuten
    /** Beitrittsversuche je IP insgesamt – großzügig, weil eine ganze Klasse hinter einer IP sitzen kann. */
    const val JOIN_ATTEMPTS_PER_IP = 60          // pro Minute
    /** Beitrittsversuche je Code. */
    const val JOIN_ATTEMPTS_PER_CODE = 60        // pro Minute (eine volle Klasse tritt oft gleichzeitig bei)
    /** Fehlgeschlagene Beitritte über alle IPs – Notbremse bei verteiltem Durchprobieren. */
    const val JOIN_FAILURES_GLOBAL = 300         // pro Minute
    const val CLASS_CREATIONS_PER_IP = 10        // pro Stunde
    const val RECOVER_FAILURES_PER_IP = 10       // pro 10 Minuten
    const val AVATAR_UPLOADS_PER_DEVICE = 10     // pro Minute
}
