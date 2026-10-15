package com.realcraftguy.classview.student.data

import android.net.Uri
import com.realcraftguy.classview.protocol.JoinRequest
import com.realcraftguy.classview.protocol.UpdateDeviceRequest

/** Ergebnis einer Prüfung der bestehenden Mitgliedschaft. */
sealed interface Status {
    data object NotJoined : Status
    data object Connected : Status
    /** Server gerade nicht erreichbar – Mitgliedschaft bleibt, es wird später erneut versucht. */
    data class Offline(val message: String) : Status
    /** Aus der Klasse entfernt, ausgetreten oder Klasse gelöscht; die lokale Mitgliedschaft ist gelöscht. */
    data class Removed(val message: String) : Status
}

/**
 * Fachlogik der Schüler-App ohne Oberfläche: Beitreten, Wiederverbinden, Profil synchronisieren, Verlassen.
 * TODO: Phase 5 – Verbindung und Online-Status laufen dann über den WebSocket im ClassroomService.
 */
class StudentRepository(
    val store: StudentStore,
    private val apiFor: (String) -> ApiClient = { ApiClient(it) },
) {
    /** Prüft die Adresse und speichert sie. Liefert die normalisierte URL. */
    suspend fun setServer(input: String): String {
        val url = ApiClient.normalizeServerUrl(input) ?: throw ServerError(0, "invalid", "Bitte eine Adresse eingeben, z. B. classview.tail1234.ts.net")
        apiFor(url).check()
        store.setServerUrl(url)
        return url
    }

    suspend fun join(code: String): Membership {
        val profile = store.currentProfile()
        if (profile.name.isBlank()) throw ServerError(0, "no_name", "Lege zuerst in „Mein Profil“ deinen Namen fest.")
        val serverUrl = store.currentSettings().serverUrl
        if (serverUrl.isBlank()) throw ServerError(0, "no_server", "Es ist noch keine Server-Adresse eingetragen.")
        val api = apiFor(serverUrl)
        val res = api.join(JoinRequest(code, profile.name, profile.preset))
        val m = Membership(serverUrl, res.classId, res.deviceId, res.deviceToken, res.className, res.teacherName)
        store.saveMembership(m)
        if (profile.hasPhoto) syncProfile()
        return m
    }

    /** Fragt beim Server nach, ob dieses Gerät noch in der Klasse ist, und überträgt offene Profiländerungen. */
    suspend fun refresh(): Status {
        val m = store.currentMembership() ?: return Status.NotJoined
        return try {
            val me = apiFor(m.serverUrl).me(m.deviceToken)
            if (me.className != m.className || me.teacherName != m.teacherName) store.updateClassInfo(me.className, me.teacherName)
            if (store.currentSettings().profileDirty) syncProfile()
            Status.Connected
        } catch (e: ServerError) {
            when {
                e.isMembershipGone -> {
                    store.clearMembership()
                    Status.Removed(e.message)
                }
                else -> Status.Offline(e.message)
            }
        }
    }

    suspend fun saveProfile(name: String, preset: String?) {
        store.saveProfile(name.trim().replace(Regex("\\s+"), " "), preset)
        syncProfile()
    }

    suspend fun setPhoto(uri: Uri) {
        store.savePhoto(uri)
        syncProfile()
    }

    suspend fun removePhoto() {
        store.removePhoto()
        syncProfile()
    }

    /**
     * Überträgt Name, Avatar und ggf. Foto an den Server, falls dieses Gerät in einer Klasse ist.
     * Ohne Verbindung wird die Änderung vorgemerkt und beim nächsten [refresh] nachgeholt.
     */
    suspend fun syncProfile(): Boolean {
        val m = store.currentMembership() ?: return true
        val profile = store.currentProfile()
        val api = apiFor(m.serverUrl)
        return try {
            val me = api.updateMe(m.deviceToken, UpdateDeviceRequest(displayName = profile.name, avatarPreset = profile.preset ?: ""))
            if (profile.hasPhoto && store.uploadedPhotoVersion() != profile.photoVersion) {
                store.photoBytes()?.let { api.uploadAvatar(m.deviceToken, it) }
                store.setUploadedPhotoVersion(profile.photoVersion)
            } else if (!profile.hasPhoto && me.hasImage) {
                api.clearAvatar(m.deviceToken)
                store.setUploadedPhotoVersion(profile.photoVersion)
            }
            store.setProfileDirty(false)
            true
        } catch (e: ServerError) {
            if (e.isMembershipGone) store.clearMembership() else store.setProfileDirty(true)
            false
        }
    }

    /** Verlässt die Klasse. Ist der Server nicht erreichbar, wird nur lokal abgemeldet. */
    suspend fun leave(): Boolean {
        val m = store.currentMembership() ?: return true
        val ok = try {
            apiFor(m.serverUrl).leave(m.deviceToken)
            true
        } catch (e: ServerError) {
            e.isMembershipGone
        }
        store.clearMembership()
        return ok
    }

    companion object {
        const val CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

        /**
         * Bereinigt eine Code-Eingabe: Großbuchstaben, nur erlaubte Zeichen, max. 6. Liefert zusätzlich einen
         * Hinweis, wenn verwechselbare Zeichen getippt wurden, die es in Codes nicht gibt.
         */
        fun sanitizeCode(raw: String): Pair<String, String?> {
            val upper = raw.uppercase()
            val confusing = upper.any { it in "01OIL" }
            val clean = upper.filter { it in CODE_ALPHABET }.take(6)
            val hint = if (confusing) "Codes enthalten kein 0, O, 1, I oder L." else null
            return clean to hint
        }
    }
}
