package com.realcraftguy.classview.server

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/** Erzeugt Tokens, Codes und IDs. Alles kryptografisch zufällig. */
class Secrets(private val random: SecureRandom = SecureRandom()) {

    /** 256 Bit Zufall, base64url ohne Padding (43 Zeichen). */
    fun token(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun id(): String {
        val bytes = ByteArray(16).also(random::nextBytes)
        // UUIDv4: Version- und Variant-Bits setzen
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val hi = bytes.copyOfRange(0, 8).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
        val lo = bytes.copyOfRange(8, 16).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
        return UUID(hi, lo).toString()
    }

    /** 6 Zeichen ohne verwechselbare Zeichen (kein 0/O, 1/I/L). */
    fun classCode(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
    }

    fun fileName(): String = token().take(24) + ".jpg"

    companion object {
        const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        const val CODE_LENGTH = 6

        fun hash(secret: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        /** Vergleich in konstanter Zeit, damit die Antwortzeit nichts über den Hash verrät. */
        fun hashMatches(secret: String, expectedHash: String?): Boolean {
            if (expectedHash == null) return false
            return MessageDigest.isEqual(hash(secret).toByteArray(), expectedHash.toByteArray())
        }

        /**
         * Normalisiert eine Code-Eingabe: Großbuchstaben, Leerzeichen und Trennzeichen entfernt.
         * Liefert null, wenn das Ergebnis kein gültig geformter Code ist.
         */
        fun normalizeCode(input: String): String? {
            val cleaned = input.uppercase().filter { !it.isWhitespace() && it != '-' && it != '·' && it != '.' }
            if (cleaned.length != CODE_LENGTH) return null
            return cleaned.takeIf { c -> c.all { it in CODE_ALPHABET } }
        }
    }
}
