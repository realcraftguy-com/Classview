package com.realcraftguy.classview.server

import com.realcraftguy.classview.protocol.AVATAR_PRESETS
import com.realcraftguy.classview.protocol.ClassSnapshot
import com.realcraftguy.classview.protocol.CodeResponse
import com.realcraftguy.classview.protocol.CodeTtl
import com.realcraftguy.classview.protocol.CreateClassRequest
import com.realcraftguy.classview.protocol.CreateClassResponse
import com.realcraftguy.classview.protocol.DeviceDto
import com.realcraftguy.classview.protocol.ErrorCodes
import com.realcraftguy.classview.protocol.JoinRequest
import com.realcraftguy.classview.protocol.JoinResponse
import com.realcraftguy.classview.protocol.MeResponse
import com.realcraftguy.classview.protocol.RecoverRequest
import com.realcraftguy.classview.protocol.RecoverResponse
import com.realcraftguy.classview.protocol.RecoveryTokenResponse
import com.realcraftguy.classview.protocol.TeacherDto
import com.realcraftguy.classview.protocol.UpdateClassRequest
import com.realcraftguy.classview.protocol.UpdateDeviceRequest
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ApiException(val status: HttpStatusCode, val code: String, override val message: String, val retryAfterMs: Long = 0) :
    RuntimeException(message)

/**
 * Fachlogik für Klassen, Codes, Lehrer- und Geräte-Tokens. Alle Zugriffe laufen nacheinander über einen
 * Mutex; jede Änderung wird sofort gespeichert. Die Methoden prüfen Berechtigungen selbst, die Routen
 * reichen nur Token und IDs durch.
 */
class ClassService(
    private val store: DataStore,
    private val avatars: AvatarStore,
    private val images: ImageProcessor,
    private val clock: () -> Long = System::currentTimeMillis,
    private val secrets: Secrets = Secrets(),
) {
    private val mutex = Mutex()
    private val data: ServerData = store.load()
    val limiter = RateLimiter(clock)

    /** Online-Status kommt ab Phase 5 aus den WebSocket-Verbindungen. TODO: Phase 5 */
    var isOnline: (deviceId: String) -> Boolean = { false }

    private suspend fun <T> read(block: () -> T): T = mutex.withLock { block() }
    private suspend fun <T> write(block: () -> T): T = mutex.withLock {
        val result = block()
        store.save(data)
        result
    }

    // ------------------------------------------------------------------ Host

    /** Erzeugt einen neuen Host-Einladungsschlüssel; der alte wird ungültig. Nur für die Host-App. */
    suspend fun rotateInviteKey(): String = write {
        val key = secrets.token()
        data.inviteKeyHash = Secrets.hash(key)
        key
    }

    suspend fun hasInviteKey(): Boolean = read { data.inviteKeyHash != null }

    /** Für die Statusanzeige der Host-App: Anzahl Klassen und beigetretener Geräte. */
    suspend fun stats(): Pair<Int, Int> = read { data.classes.size to data.devices.count { it.removedAt == null } }

    /** Entfernt seit [maxIdleMs] unbenutzte Klassen samt Geräten und Bildern. */
    suspend fun cleanup(maxIdleMs: Long = 30L * 24 * 60 * 60 * 1000): Int = write {
        val cutoff = clock() - maxIdleMs
        val stale = data.classes.filter { it.lastActivityAt < cutoff }.map { it.id }.toSet()
        stale.forEach { deleteClassLocked(it) }
        data.devices.removeAll { it.removedAt != null && it.removedAt!! < cutoff }
        stale.size
    }

    // ------------------------------------------------------------------ Lehrkraft

    suspend fun createClass(req: CreateClassRequest, ip: String): CreateClassResponse {
        rateLimit("create:ip:$ip", Limits.CLASS_CREATIONS_PER_IP, Limits.HOUR)
        limiter.record("create:ip:$ip")
        return write {
            if (!Secrets.hashMatches(req.inviteKey, data.inviteKeyHash)) {
                throw ApiException(HttpStatusCode.Forbidden, ErrorCodes.INVITE_REQUIRED,
                    "Zum Anlegen einer Klasse brauchst du den Einladungslink aus der Host-App.")
            }
            val now = clock()
            val code = newUniqueCode()
            val teacherToken = secrets.token()
            val recoveryToken = secrets.token()
            val cls = ClassRecord(
                id = secrets.id(),
                name = cleanText(req.name, 60, "Klassenname"),
                teacherName = req.teacherName?.let { cleanOptional(it, 60) },
                code = code,
                codeCreatedAt = now,
                codeExpiresAt = req.codeTtl.millis?.let { now + it },
                codeTtl = req.codeTtl.name,
                recoveryTokenHash = Secrets.hash(recoveryToken),
                createdAt = now,
                lastActivityAt = now,
            )
            data.classes += cls
            data.teachers += TeacherCredentialRecord(
                id = secrets.id(), classId = cls.id, tokenHash = Secrets.hash(teacherToken),
                label = cleanOptional(req.deviceLabel ?: "", 60) ?: "Lehrer-Gerät", createdAt = now, lastUsedAt = now,
            )
            CreateClassResponse(cls.id, cls.name, code, cls.codeExpiresAt, teacherToken, recoveryToken)
        }
    }

    suspend fun recover(req: RecoverRequest, ip: String): RecoverResponse {
        rateLimit("recover-fail:ip:$ip", Limits.RECOVER_FAILURES_PER_IP, Limits.TEN_MINUTES)
        return write {
            val hash = Secrets.hash(req.recoveryToken.trim())
            val cls = data.classes.firstOrNull { java.security.MessageDigest.isEqual(it.recoveryTokenHash.toByteArray(), hash.toByteArray()) }
            if (cls == null) {
                limiter.record("recover-fail:ip:$ip")
                throw ApiException(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND,
                    "Dieser Wiederherstellungslink ist ungültig oder wurde erneuert.")
            }
            val now = clock()
            val token = secrets.token()
            data.teachers += TeacherCredentialRecord(
                id = secrets.id(), classId = cls.id, tokenHash = Secrets.hash(token),
                label = cleanOptional(req.deviceLabel ?: "", 60) ?: "Lehrer-Gerät", createdAt = now, lastUsedAt = now,
            )
            cls.lastActivityAt = now
            RecoverResponse(cls.id, cls.name, token)
        }
    }

    suspend fun snapshot(classId: String, teacherToken: String): ClassSnapshot = read {
        val (cls, _) = teacherLocked(classId, teacherToken)
        snapshotLocked(cls)
    }

    suspend fun updateClass(classId: String, teacherToken: String, req: UpdateClassRequest): ClassSnapshot = write {
        val (cls, _) = teacherLocked(classId, teacherToken)
        req.name?.let { cls.name = cleanText(it, 60, "Klassenname") }
        req.teacherName?.let { cls.teacherName = cleanOptional(it, 60) }
        req.joinPaused?.let { cls.joinPaused = it }
        req.codeTtl?.let { ttl ->
            cls.codeTtl = ttl.name
            cls.codeExpiresAt = ttl.millis?.let { clock() + it }
        }
        snapshotLocked(cls)
    }

    suspend fun rotateCode(classId: String, teacherToken: String): CodeResponse = write {
        val (cls, _) = teacherLocked(classId, teacherToken)
        val now = clock()
        cls.code = newUniqueCode()
        cls.codeCreatedAt = now
        cls.codeExpiresAt = CodeTtl.valueOf(cls.codeTtl).millis?.let { now + it }
        CodeResponse(cls.code!!, cls.codeExpiresAt)
    }

    suspend fun rotateRecovery(classId: String, teacherToken: String): RecoveryTokenResponse = write {
        val (cls, _) = teacherLocked(classId, teacherToken)
        val token = secrets.token()
        cls.recoveryTokenHash = Secrets.hash(token)
        RecoveryTokenResponse(token)
    }

    suspend fun teachers(classId: String, teacherToken: String): List<TeacherDto> = read {
        val (_, me) = teacherLocked(classId, teacherToken)
        data.teachers.filter { it.classId == classId && it.revokedAt == null }
            .map { TeacherDto(it.id, it.label, it.createdAt, it.lastUsedAt, current = it.id == me.id) }
    }

    suspend fun revokeTeacher(classId: String, teacherToken: String, teacherId: String) = write {
        val (_, me) = teacherLocked(classId, teacherToken)
        if (teacherId == me.id) {
            throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.INVALID_REQUEST, "Das eigene Gerät kann hier nicht abgemeldet werden.")
        }
        val target = data.teachers.firstOrNull { it.id == teacherId && it.classId == classId && it.revokedAt == null }
            ?: throw notFound()
        target.revokedAt = clock()
    }

    suspend fun removeDevice(classId: String, teacherToken: String, deviceId: String) = write {
        teacherLocked(classId, teacherToken)
        val device = data.devices.firstOrNull { it.id == deviceId && it.classId == classId && it.removedAt == null }
            ?: throw notFound()
        removeDeviceLocked(device, "removed")
    }

    suspend fun deleteClass(classId: String, teacherToken: String) = write {
        teacherLocked(classId, teacherToken)
        deleteClassLocked(classId)
    }

    suspend fun avatarForTeacher(classId: String, teacherToken: String, deviceId: String): ByteArray = read {
        teacherLocked(classId, teacherToken)
        val device = data.devices.firstOrNull { it.id == deviceId && it.classId == classId && it.removedAt == null }
            ?: throw notFound()
        device.avatarFile?.let { avatars.get(it) } ?: throw notFound()
    }

    // ------------------------------------------------------------------ Schülergerät

    suspend fun join(req: JoinRequest, ip: String): JoinResponse {
        rateLimit("join-fail:ip:$ip", Limits.JOIN_FAILURES_PER_IP, Limits.TEN_MINUTES)
        rateLimit("join:ip:$ip", Limits.JOIN_ATTEMPTS_PER_IP, Limits.MINUTE)
        rateLimit("join-fail:global", Limits.JOIN_FAILURES_GLOBAL, Limits.MINUTE)
        limiter.record("join:ip:$ip")

        val code = Secrets.normalizeCode(req.code)
        if (code != null) {
            rateLimit("join:code:$code", Limits.JOIN_ATTEMPTS_PER_CODE, Limits.MINUTE)
            limiter.record("join:code:$code")
        }
        val displayName = cleanText(req.displayName, 40, "Name")
        val preset = checkPreset(req.avatarPreset)

        return try {
            write {
                val now = clock()
                val cls = code?.let { c -> data.classes.firstOrNull { it.code == c } }
                    ?: throw ApiException(HttpStatusCode.NotFound, ErrorCodes.CODE_INVALID,
                        "Diesen Code gibt es nicht. Prüfe ihn noch einmal.")
                if (cls.codeExpiresAt != null && cls.codeExpiresAt!! <= now) {
                    throw ApiException(HttpStatusCode.Gone, ErrorCodes.CODE_EXPIRED, "Dieser Code ist abgelaufen. Frag nach einem neuen.")
                }
                if (cls.joinPaused) {
                    throw ApiException(HttpStatusCode.Forbidden, ErrorCodes.JOIN_PAUSED, "Der Beitritt ist gerade pausiert.")
                }
                if (data.devices.count { it.classId == cls.id && it.removedAt == null } >= cls.maxDevices) {
                    throw ApiException(HttpStatusCode.Conflict, ErrorCodes.CLASS_FULL, "Die Klasse ist voll.")
                }
                val token = secrets.token()
                val device = DeviceRecord(
                    id = secrets.id(), classId = cls.id, tokenHash = Secrets.hash(token),
                    displayName = displayName, avatarPreset = preset, joinedAt = now, lastSeenAt = now,
                )
                data.devices += device
                cls.lastActivityAt = now
                JoinResponse(device.id, token, cls.id, cls.name, cls.teacherName)
            }
        } catch (e: ApiException) {
            if (e.code == ErrorCodes.CODE_INVALID) {
                limiter.record("join-fail:ip:$ip")
                limiter.record("join-fail:global")
            }
            throw e
        }
    }

    suspend fun me(deviceToken: String): MeResponse = read {
        val device = deviceLocked(deviceToken)
        meLocked(device)
    }

    suspend fun updateDevice(deviceToken: String, req: UpdateDeviceRequest): MeResponse = write {
        val device = deviceLocked(deviceToken)
        req.displayName?.let { device.displayName = cleanText(it, 40, "Name") }
        req.avatarPreset?.let { p ->
            device.avatarPreset = if (p.isEmpty()) null else checkPreset(p)
            device.avatarVersion++
        }
        meLocked(device)
    }

    suspend fun setAvatar(deviceToken: String, bytes: ByteArray): MeResponse {
        val deviceId = read { deviceLocked(deviceToken).id }
        rateLimit("avatar:device:$deviceId", Limits.AVATAR_UPLOADS_PER_DEVICE, Limits.MINUTE)
        limiter.record("avatar:device:$deviceId")
        if (bytes.size > ImageLimits.MAX_UPLOAD_BYTES) {
            throw ApiException(HttpStatusCode.PayloadTooLarge, ErrorCodes.PAYLOAD_TOO_LARGE, "Das Bild ist größer als 1 MB.")
        }
        val processed = try {
            images.process(bytes)
        } catch (e: UnsupportedImageException) {
            throw ApiException(HttpStatusCode.UnsupportedMediaType, ErrorCodes.UNSUPPORTED_IMAGE,
                "Das Bild konnte nicht gelesen werden. Erlaubt sind JPEG, PNG und WebP.")
        }
        return write {
            val device = deviceLocked(deviceToken)
            val name = secrets.fileName()
            avatars.put(name, processed)
            device.avatarFile?.let { avatars.delete(it) }
            device.avatarFile = name
            device.avatarVersion++
            meLocked(device)
        }
    }

    suspend fun clearAvatar(deviceToken: String): MeResponse = write {
        val device = deviceLocked(deviceToken)
        device.avatarFile?.let { avatars.delete(it) }
        device.avatarFile = null
        device.avatarVersion++
        meLocked(device)
    }

    suspend fun leave(deviceToken: String) = write {
        removeDeviceLocked(deviceLocked(deviceToken), "left")
    }

    // ------------------------------------------------------------------ intern

    private fun teacherLocked(classId: String, token: String): Pair<ClassRecord, TeacherCredentialRecord> {
        val hash = Secrets.hash(token)
        val cred = data.teachers.firstOrNull { it.tokenHash == hash && it.revokedAt == null }
            ?: throw ApiException(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, "Nicht angemeldet.")
        if (cred.classId != classId) {
            throw ApiException(HttpStatusCode.Forbidden, ErrorCodes.FORBIDDEN, "Kein Zugriff auf diese Klasse.")
        }
        val cls = data.classes.firstOrNull { it.id == classId } ?: throw notFound()
        val now = clock()
        cred.lastUsedAt = now
        cls.lastActivityAt = now
        return cls to cred
    }

    private fun deviceLocked(token: String): DeviceRecord {
        val hash = Secrets.hash(token)
        val device = data.devices.firstOrNull { it.tokenHash == hash }
            ?: throw ApiException(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, "Dieses Gerät ist in keiner Klasse.")
        if (device.removedAt != null) {
            val msg = if (device.removedReason == "left") "Du hast die Klasse verlassen." else "Du wurdest aus der Klasse entfernt."
            throw ApiException(HttpStatusCode.Unauthorized, ErrorCodes.DEVICE_REMOVED, msg)
        }
        val now = clock()
        device.lastSeenAt = now
        data.classes.firstOrNull { it.id == device.classId }?.lastActivityAt = now
        return device
    }

    private fun snapshotLocked(cls: ClassRecord) = ClassSnapshot(
        id = cls.id, name = cls.name, teacherName = cls.teacherName,
        code = cls.code, codeExpiresAt = cls.codeExpiresAt, codeTtl = CodeTtl.valueOf(cls.codeTtl),
        joinPaused = cls.joinPaused, maxDevices = cls.maxDevices,
        devices = data.devices.filter { it.classId == cls.id && it.removedAt == null }.sortedBy { it.joinedAt }.map {
            DeviceDto(it.id, it.displayName, it.avatarPreset, it.avatarFile != null, it.avatarVersion,
                isOnline(it.id), it.lastSeenAt, it.joinedAt)
        },
    )

    private fun meLocked(device: DeviceRecord): MeResponse {
        val cls = data.classes.first { it.id == device.classId }
        return MeResponse(device.id, cls.id, cls.name, cls.teacherName, device.displayName,
            device.avatarPreset, device.avatarFile != null, device.avatarVersion)
    }

    private fun removeDeviceLocked(device: DeviceRecord, reason: String) {
        device.removedAt = clock()
        device.removedReason = reason
        device.avatarFile?.let { avatars.delete(it) }
        device.avatarFile = null
    }

    private fun deleteClassLocked(classId: String) {
        data.devices.filter { it.classId == classId }.forEach { d -> d.avatarFile?.let { avatars.delete(it) } }
        data.devices.removeAll { it.classId == classId }
        data.teachers.removeAll { it.classId == classId }
        data.classes.removeAll { it.id == classId }
    }

    private fun newUniqueCode(): String {
        val used = data.classes.mapNotNull { it.code }.toSet()
        repeat(100) {
            val c = secrets.classCode()
            if (c !in used) return c
        }
        error("Kein freier Code gefunden")
    }

    private fun rateLimit(key: String, limit: Int, windowMs: Long) {
        val wait = limiter.retryAfter(key, limit, windowMs)
        if (wait > 0) {
            throw ApiException(HttpStatusCode.TooManyRequests, ErrorCodes.RATE_LIMITED,
                "Zu viele Versuche. Bitte warte kurz und versuche es dann erneut.", wait)
        }
    }

    private fun checkPreset(preset: String?): String? {
        if (preset == null) return null
        if (preset !in AVATAR_PRESETS) throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.INVALID_REQUEST, "Unbekannter Avatar.")
        return preset
    }

    private fun notFound() = ApiException(HttpStatusCode.NotFound, ErrorCodes.NOT_FOUND, "Nicht gefunden.")

    companion object {
        /** Entfernt Steuerzeichen, fasst Leerraum zusammen und prüft die Länge. */
        fun cleanText(input: String, max: Int, field: String): String {
            val cleaned = input.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
            if (cleaned.isEmpty()) throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.INVALID_REQUEST, "$field darf nicht leer sein.")
            if (cleaned.length > max) throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.INVALID_REQUEST, "$field ist zu lang (max. $max Zeichen).")
            return cleaned
        }

        fun cleanOptional(input: String, max: Int): String? {
            val cleaned = input.filter { !it.isISOControl() }.trim().replace(Regex("\\s+"), " ")
            return cleaned.take(max).ifEmpty { null }
        }
    }
}
