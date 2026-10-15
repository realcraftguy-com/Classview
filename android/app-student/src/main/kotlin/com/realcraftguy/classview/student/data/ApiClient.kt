package com.realcraftguy.classview.student.data

import com.realcraftguy.classview.protocol.ErrorCodes
import com.realcraftguy.classview.protocol.ErrorResponse
import com.realcraftguy.classview.protocol.InfoResponse
import com.realcraftguy.classview.protocol.JoinRequest
import com.realcraftguy.classview.protocol.JoinResponse
import com.realcraftguy.classview.protocol.MeResponse
import com.realcraftguy.classview.protocol.PROTOCOL_VERSION
import com.realcraftguy.classview.protocol.ProtocolJson
import com.realcraftguy.classview.protocol.UpdateDeviceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Fehler vom Server oder beim Verbinden; [message] ist ein deutscher Text zum Anzeigen. */
class ServerError(val status: Int, val code: String, override val message: String) : Exception(message) {
    val isNetwork get() = code == NETWORK
    /** Gerät ist nicht mehr in der Klasse (entfernt, ausgetreten, Klasse gelöscht). */
    val isMembershipGone get() = code == ErrorCodes.DEVICE_REMOVED || (status == 401 && code == ErrorCodes.UNAUTHORIZED)

    companion object {
        const val NETWORK = "network"
    }
}

/** REST-Zugriff der Schüler-App. Nutzt dieselben Nachrichtenklassen wie der Server (backend/protocol). */
class ApiClient(baseUrl: String, private val http: OkHttpClient = defaultHttp) {
    private val base = baseUrl.trimEnd('/')

    suspend fun info(): InfoResponse = call("GET", "/api/info")

    suspend fun join(req: JoinRequest): JoinResponse = call("POST", "/api/join", body = json(req))

    suspend fun me(token: String): MeResponse = call("GET", "/api/devices/me", token)

    suspend fun updateMe(token: String, req: UpdateDeviceRequest): MeResponse =
        call("PATCH", "/api/devices/me", token, json(req))

    suspend fun uploadAvatar(token: String, jpeg: ByteArray): MeResponse =
        call("PUT", "/api/devices/me/avatar", token, jpeg.toRequestBody("image/jpeg".toMediaType()))

    suspend fun clearAvatar(token: String): MeResponse = call("DELETE", "/api/devices/me/avatar", token)

    suspend fun leave(token: String) {
        call<Unit>("DELETE", "/api/devices/me", token)
    }

    private inline fun <reified T> json(value: T): RequestBody =
        ProtocolJson.encodeToString(serializer<T>(), value).toRequestBody(JSON)

    private suspend inline fun <reified T> call(method: String, path: String, token: String? = null, body: RequestBody? = null): T =
        execute(method, path, token, body, serializer<T>())

    private suspend fun <T> execute(method: String, path: String, token: String?, body: RequestBody?, ser: KSerializer<T>): T =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(base + path).method(method, body).apply {
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            try {
                http.newCall(request).execute().use { res ->
                    val text = res.body.string()
                    if (!res.isSuccessful) {
                        val err = runCatching { ProtocolJson.decodeFromString(ErrorResponse.serializer(), text) }.getOrNull()
                        throw ServerError(res.code, err?.error ?: "http_${res.code}", err?.message ?: "Fehler ${res.code} vom Server.")
                    }
                    @Suppress("UNCHECKED_CAST")
                    if (ser.descriptor.serialName == "kotlin.Unit") Unit as T else ProtocolJson.decodeFromString(ser, text)
                }
            } catch (e: IOException) {
                throw ServerError(0, ServerError.NETWORK, "Keine Verbindung zum Server. Bist du im Internet?")
            }
        }

    /** Prüft, ob unter der Adresse ein passender ClassroomView-Server läuft. */
    suspend fun check(): InfoResponse {
        val info = info()
        if (info.name != "ClassroomView") throw ServerError(0, "not_classview", "Unter dieser Adresse läuft kein ClassroomView-Server.")
        if (info.protocolVersion != PROTOCOL_VERSION) {
            throw ServerError(0, "version", "Server und App passen nicht zusammen. Bitte beide aktualisieren.")
        }
        return info
    }

    companion object {
        private val JSON = "application/json".toMediaType()

        val defaultHttp: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        /** "classview.tail123.ts.net" → "https://classview.tail123.ts.net". Leere Eingabe → null. */
        fun normalizeServerUrl(input: String): String? {
            val t = input.trim().trimEnd('/')
            if (t.isEmpty() || t.any { it.isWhitespace() }) return null
            return if (t.startsWith("https://") || t.startsWith("http://")) t else "https://$t"
        }
    }
}
