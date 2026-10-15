package com.realcraftguy.classview.student.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.realcraftguy.classview.student.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

private val Context.studentPrefs by preferencesDataStore(name = "student")

/** Eigenes Profil: Name, eingebauter Avatar oder Foto. Liegt nur auf diesem Gerät. */
data class Profile(
    val name: String = "",
    val preset: String? = null,
    /** Wird bei jeder Änderung des Fotos erhöht, damit die Oberfläche neu lädt. */
    val photoVersion: Int = 0,
    val hasPhoto: Boolean = false,
)

/** Die Klasse, in der dieses Gerät gerade ist. */
data class Membership(
    val serverUrl: String,
    val classId: String,
    val deviceId: String,
    val deviceToken: String,
    val className: String,
    val teacherName: String?,
)

data class Settings(
    val serverUrl: String,
    /** Profil wurde lokal geändert, aber noch nicht an den Server übertragen. */
    val profileDirty: Boolean,
    val hintsSeen: Boolean,
    val autostartDone: Boolean,
    val lockDone: Boolean,
)

/** Alle dauerhaft gespeicherten Werte der Schüler-App (DataStore, App-privat, ohne Backup). */
class StudentStore(private val context: Context) {
    private object K {
        val name = stringPreferencesKey("profile_name")
        val preset = stringPreferencesKey("profile_preset")
        val photoVersion = intPreferencesKey("profile_photo_version")
        val serverUrl = stringPreferencesKey("server_url")
        val classId = stringPreferencesKey("class_id")
        val deviceId = stringPreferencesKey("device_id")
        val deviceToken = stringPreferencesKey("device_token")
        val className = stringPreferencesKey("class_name")
        val teacherName = stringPreferencesKey("teacher_name")
        val membershipServer = stringPreferencesKey("membership_server")
        val profileDirty = booleanPreferencesKey("profile_dirty")
        val hintsSeen = booleanPreferencesKey("hints_seen")
        val autostartDone = booleanPreferencesKey("hint_autostart_done")
        val lockDone = booleanPreferencesKey("hint_lock_done")
        val uploadedPhotoVersion = intPreferencesKey("uploaded_photo_version")
    }

    val photoFile = File(context.filesDir, "profile.jpg")
    private val data = context.studentPrefs.data

    val profile: Flow<Profile> = data.map { p ->
        Profile(p[K.name].orEmpty(), p[K.preset], p[K.photoVersion] ?: 0, photoFile.exists())
    }

    val membership: Flow<Membership?> = data.map { p -> membershipOf(p) }

    val settings: Flow<Settings> = data.map { p ->
        Settings(
            serverUrl = p[K.serverUrl] ?: BuildConfig.DEFAULT_SERVER_URL,
            profileDirty = p[K.profileDirty] ?: false,
            hintsSeen = p[K.hintsSeen] ?: false,
            autostartDone = p[K.autostartDone] ?: false,
            lockDone = p[K.lockDone] ?: false,
        )
    }

    private fun membershipOf(p: Preferences): Membership? {
        val token = p[K.deviceToken] ?: return null
        return Membership(
            serverUrl = p[K.membershipServer] ?: p[K.serverUrl] ?: BuildConfig.DEFAULT_SERVER_URL,
            classId = p[K.classId] ?: return null,
            deviceId = p[K.deviceId] ?: return null,
            deviceToken = token,
            className = p[K.className].orEmpty(),
            teacherName = p[K.teacherName],
        )
    }

    suspend fun currentProfile() = profile.first()
    suspend fun currentMembership() = membership.first()
    suspend fun currentSettings() = settings.first()

    suspend fun saveProfile(name: String, preset: String?) = context.studentPrefs.edit {
        it[K.name] = name
        if (preset == null) it.remove(K.preset) else it[K.preset] = preset
    }

    /** Verkleinert das Foto aus dem Photo Picker auf max. 512 px und speichert es als JPEG. */
    suspend fun savePhoto(uri: Uri) {
        val jpeg = withContext(Dispatchers.IO) { downscaleToJpeg(context, uri) }
        withContext(Dispatchers.IO) { photoFile.writeBytes(jpeg) }
        context.studentPrefs.edit { it[K.photoVersion] = (it[K.photoVersion] ?: 0) + 1 }
    }

    suspend fun removePhoto() {
        withContext(Dispatchers.IO) { photoFile.delete() }
        context.studentPrefs.edit { it[K.photoVersion] = (it[K.photoVersion] ?: 0) + 1 }
    }

    suspend fun photoBytes(): ByteArray? = withContext(Dispatchers.IO) { photoFile.takeIf { it.exists() }?.readBytes() }

    suspend fun setServerUrl(url: String) = context.studentPrefs.edit { it[K.serverUrl] = url }

    suspend fun saveMembership(m: Membership) = context.studentPrefs.edit {
        it[K.membershipServer] = m.serverUrl
        it[K.classId] = m.classId
        it[K.deviceId] = m.deviceId
        it[K.deviceToken] = m.deviceToken
        it[K.className] = m.className
        if (m.teacherName == null) it.remove(K.teacherName) else it[K.teacherName] = m.teacherName
        it[K.profileDirty] = false
    }

    suspend fun updateClassInfo(className: String, teacherName: String?) = context.studentPrefs.edit {
        it[K.className] = className
        if (teacherName == null) it.remove(K.teacherName) else it[K.teacherName] = teacherName
    }

    suspend fun clearMembership() = context.studentPrefs.edit {
        listOf(K.membershipServer, K.classId, K.deviceId, K.deviceToken, K.className, K.teacherName, K.profileDirty, K.uploadedPhotoVersion)
            .forEach { k -> it.remove(k) }
    }

    suspend fun uploadedPhotoVersion(): Int = data.first()[K.uploadedPhotoVersion] ?: -1
    suspend fun setUploadedPhotoVersion(v: Int) = context.studentPrefs.edit { it[K.uploadedPhotoVersion] = v }

    suspend fun setProfileDirty(dirty: Boolean) = context.studentPrefs.edit { it[K.profileDirty] = dirty }
    suspend fun setHintsSeen() = context.studentPrefs.edit { it[K.hintsSeen] = true }
    suspend fun setAutostartDone(done: Boolean) = context.studentPrefs.edit { it[K.autostartDone] = done }
    suspend fun setLockDone(done: Boolean) = context.studentPrefs.edit { it[K.lockDone] = done }

    companion object {
        const val MAX_SIDE = 512

        /** ImageDecoder beachtet die EXIF-Drehung; das Ergebnis enthält keine Metadaten mehr. */
        fun downscaleToJpeg(context: Context, uri: Uri): ByteArray {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(w, h))
                decoder.setTargetSize(maxOf(1, (w * scale).toInt()), maxOf(1, (h * scale).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val out = ByteArrayOutputStream()
            // Transparente Bilder auf Weiß legen, JPEG kennt keine Transparenz.
            val opaque = if (bitmap.hasAlpha()) {
                Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { b ->
                    android.graphics.Canvas(b).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
                }
            } else bitmap
            opaque.compress(Bitmap.CompressFormat.JPEG, 85, out)
            return out.toByteArray()
        }
    }
}
