package com.realcraftguy.classview.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * REST-Nachrichten zwischen Server, Schüler-App und Lehrer-Web-App. Die Web-App spiegelt diese Typen in
 * TypeScript (web/src/api/types.ts). Zeitstempel sind Unix-Millisekunden (UTC).
 */
const val PROTOCOL_VERSION = 1

val ProtocolJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

/** Wie lange ein Beitrittscode gültig bleibt. */
@Serializable
enum class CodeTtl(val millis: Long?) {
    NEVER(null),
    HOUR(60 * 60 * 1000L),
    DAY(24 * 60 * 60 * 1000L),
    WEEK(7 * 24 * 60 * 60 * 1000L),
}

@Serializable
data class InfoResponse(
    val name: String = "ClassroomView",
    val protocolVersion: Int = PROTOCOL_VERSION,
    val serverVersion: String,
)

@Serializable
data class ErrorResponse(val error: String, val message: String)

/** Maschinenlesbare Fehlercodes; [ErrorResponse.message] enthält einen deutschen Text für Menschen. */
object ErrorCodes {
    const val INVALID_REQUEST = "invalid_request"
    const val UNAUTHORIZED = "unauthorized"
    const val FORBIDDEN = "forbidden"
    const val INVITE_REQUIRED = "invite_required"
    const val NOT_FOUND = "not_found"
    const val CODE_INVALID = "code_invalid"
    const val CODE_EXPIRED = "code_expired"
    const val JOIN_PAUSED = "join_paused"
    const val CLASS_FULL = "class_full"
    const val DEVICE_REMOVED = "device_removed"
    const val RATE_LIMITED = "rate_limited"
    const val PAYLOAD_TOO_LARGE = "payload_too_large"
    const val UNSUPPORTED_IMAGE = "unsupported_image"
}

// ------------------------------------------------------------------ Lehrkraft

@Serializable
data class CreateClassRequest(
    val name: String,
    val teacherName: String? = null,
    val codeTtl: CodeTtl = CodeTtl.DAY,
    val inviteKey: String,
    val deviceLabel: String? = null,
)

@Serializable
data class CreateClassResponse(
    val classId: String,
    val name: String,
    val code: String,
    val codeExpiresAt: Long?,
    val teacherToken: String,
    val recoveryToken: String,
)

@Serializable
data class RecoverRequest(val recoveryToken: String, val deviceLabel: String? = null)

@Serializable
data class RecoverResponse(val classId: String, val name: String, val teacherToken: String)

@Serializable
data class UpdateClassRequest(
    val name: String? = null,
    val teacherName: String? = null,
    val codeTtl: CodeTtl? = null,
    val joinPaused: Boolean? = null,
)

@Serializable
data class CodeResponse(val code: String, val codeExpiresAt: Long?)

@Serializable
data class RecoveryTokenResponse(val recoveryToken: String)

@Serializable
data class DeviceDto(
    val id: String,
    val displayName: String,
    val avatarPreset: String?,
    val hasImage: Boolean,
    val avatarVersion: Int,
    val online: Boolean,
    val lastSeenAt: Long?,
    val joinedAt: Long,
)

@Serializable
data class ClassSnapshot(
    val id: String,
    val name: String,
    val teacherName: String?,
    val code: String?,
    val codeExpiresAt: Long?,
    val codeTtl: CodeTtl,
    val joinPaused: Boolean,
    val maxDevices: Int,
    val devices: List<DeviceDto>,
)

@Serializable
data class TeacherDto(
    val id: String,
    val label: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val current: Boolean,
)

// ------------------------------------------------------------------ Schülergerät

@Serializable
data class JoinRequest(
    val code: String,
    val displayName: String,
    val avatarPreset: String? = null,
)

@Serializable
data class JoinResponse(
    val deviceId: String,
    val deviceToken: String,
    val classId: String,
    val className: String,
    val teacherName: String?,
)

@Serializable
data class UpdateDeviceRequest(
    val displayName: String? = null,
    /** Schlüssel eines eingebauten Avatars; "" entfernt den Avatar (dann Initiale bzw. Foto). */
    val avatarPreset: String? = null,
)

@Serializable
data class MeResponse(
    val deviceId: String,
    val classId: String,
    val className: String,
    val teacherName: String?,
    val displayName: String,
    val avatarPreset: String?,
    val hasImage: Boolean,
    val avatarVersion: Int,
)

/** Schlüssel der eingebauten Avatare (identisch in Web und Android). */
val AVATAR_PRESETS = setOf("welle", "berg", "sonne", "mond", "stern", "blatt", "wolke", "planet")
