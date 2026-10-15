package com.realcraftguy.classview.server

import kotlinx.serialization.Serializable

/**
 * Dauerhaft gespeicherter Zustand. Alles liegt im Speicher und wird nach jeder Änderung komplett als
 * JSON geschrieben (siehe [JsonFileDataStore]); bei ≤ 40 Schülern je Klasse sind das wenige KB.
 * Geheimnisse stehen hier nur als SHA-256-Hash.
 *
 * TODO: ShareRequest und StreamSession kommen in Phase 6 bzw. 7 dazu.
 */
@Serializable
data class ServerData(
    val schemaVersion: Int = 1,
    var inviteKeyHash: String? = null,
    val classes: MutableList<ClassRecord> = mutableListOf(),
    val teachers: MutableList<TeacherCredentialRecord> = mutableListOf(),
    val devices: MutableList<DeviceRecord> = mutableListOf(),
)

@Serializable
data class ClassRecord(
    val id: String,
    var name: String,
    var teacherName: String?,
    var code: String?,
    var codeCreatedAt: Long,
    var codeExpiresAt: Long?,
    var codeTtl: String,
    var joinPaused: Boolean = false,
    var maxDevices: Int = 40,
    var recoveryTokenHash: String,
    val createdAt: Long,
    var lastActivityAt: Long,
)

@Serializable
data class TeacherCredentialRecord(
    val id: String,
    val classId: String,
    val tokenHash: String,
    val label: String,
    val createdAt: Long,
    var lastUsedAt: Long? = null,
    var revokedAt: Long? = null,
)

@Serializable
data class DeviceRecord(
    val id: String,
    val classId: String,
    val tokenHash: String,
    var displayName: String,
    var avatarPreset: String?,
    var avatarFile: String? = null,
    var avatarVersion: Int = 1,
    var lastSeenAt: Long? = null,
    val joinedAt: Long,
    /** Gesetzt, wenn die Lehrkraft das Gerät entfernt hat oder der Schüler ausgetreten ist. */
    var removedAt: Long? = null,
    var removedReason: String? = null,
)
