package com.realcraftguy.classview.server

import com.realcraftguy.classview.protocol.CreateClassRequest
import com.realcraftguy.classview.protocol.ErrorCodes
import com.realcraftguy.classview.protocol.ErrorResponse
import com.realcraftguy.classview.protocol.InfoResponse
import com.realcraftguy.classview.protocol.JoinRequest
import com.realcraftguy.classview.protocol.ProtocolJson
import com.realcraftguy.classview.protocol.RecoverRequest
import com.realcraftguy.classview.protocol.UpdateClassRequest
import com.realcraftguy.classview.protocol.UpdateDeviceRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException

/** Einstellungen, die sich zwischen Host-App und PC unterscheiden. */
data class ServerConfig(
    val serverVersion: String,
    /** true, wenn ein lokaler Proxy (Tailscale-Brücke) davorsitzt und X-Forwarded-For setzt. */
    val trustLocalProxy: Boolean,
    val web: WebContent,
)

fun Application.classviewModule(service: ClassService, config: ServerConfig) {
    install(ContentNegotiation) { json(ProtocolJson) }
    install(SecurityHeaders)
    install(StatusPages) {
        exception<ApiException> { call, e ->
            if (e.retryAfterMs > 0) call.response.header(HttpHeaders.RetryAfter, ((e.retryAfterMs + 999) / 1000).toString())
            call.respond(e.status, ErrorResponse(e.code, e.message))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.INVALID_REQUEST, "Die Anfrage ist ungültig."))
        }
        exception<SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(ErrorCodes.INVALID_REQUEST, "Die Anfrage ist ungültig."))
        }
        exception<Throwable> { call, e ->
            call.application.environment.log.error("Unerwarteter Fehler bei ${call.request.uri}", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal", "Interner Fehler im Server."))
        }
    }

    routing {
        get("/api/info") { call.respond(InfoResponse(serverVersion = config.serverVersion)) }

        // ---------------------------------------------------------- Lehrkraft
        post("/api/classes") {
            val req = call.receive<CreateClassRequest>()
            call.respond(HttpStatusCode.Created, service.createClass(req, call.clientIp(config)))
        }
        post("/api/classes/recover") {
            call.respond(service.recover(call.receive<RecoverRequest>(), call.clientIp(config)))
        }
        route("/api/classes/{classId}") {
            get { call.respond(service.snapshot(call.classId(), call.bearer())) }
            patch { call.respond(service.updateClass(call.classId(), call.bearer(), call.receive<UpdateClassRequest>())) }
            delete {
                service.deleteClass(call.classId(), call.bearer())
                call.respond(HttpStatusCode.NoContent)
            }
            post("/code") { call.respond(service.rotateCode(call.classId(), call.bearer())) }
            post("/recovery") { call.respond(service.rotateRecovery(call.classId(), call.bearer())) }
            get("/teachers") { call.respond(service.teachers(call.classId(), call.bearer())) }
            delete("/teachers/{teacherId}") {
                service.revokeTeacher(call.classId(), call.bearer(), call.param("teacherId"))
                call.respond(HttpStatusCode.NoContent)
            }
            delete("/devices/{deviceId}") {
                service.removeDevice(call.classId(), call.bearer(), call.param("deviceId"))
                call.respond(HttpStatusCode.NoContent)
            }
            get("/devices/{deviceId}/avatar") {
                val bytes = service.avatarForTeacher(call.classId(), call.bearer(), call.param("deviceId"))
                call.response.header(HttpHeaders.CacheControl, "private, max-age=86400")
                call.respondBytes(bytes, ContentType.Image.JPEG)
            }
        }

        // ---------------------------------------------------------- Schülergerät
        post("/api/join") {
            call.respond(HttpStatusCode.Created, service.join(call.receive<JoinRequest>(), call.clientIp(config)))
        }
        route("/api/devices/me") {
            get { call.respond(service.me(call.bearer())) }
            patch { call.respond(service.updateDevice(call.bearer(), call.receive<UpdateDeviceRequest>())) }
            delete {
                service.leave(call.bearer())
                call.respond(HttpStatusCode.NoContent)
            }
            put("/avatar") { call.respond(service.setAvatar(call.bearer(), call.receiveLimited(ImageLimits.MAX_UPLOAD_BYTES))) }
            delete("/avatar") { call.respond(service.clearAvatar(call.bearer())) }
        }

        route("/api/{...}") {
            handle { call.respond(HttpStatusCode.NotFound, ErrorResponse(ErrorCodes.NOT_FOUND, "Unbekannte Adresse.")) }
        }

        // ---------------------------------------------------------- Web-App
        get("/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/").orEmpty()
            val asset = config.web.get(path) ?: config.web.get("index.html")
            if (asset == null) {
                call.respond(HttpStatusCode.NotFound, "Nicht gefunden")
            } else {
                if (asset.contentType.match(ContentType.Text.Html)) {
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                } else if (path.startsWith("assets/")) {
                    call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
                }
                call.respondBytes(asset.bytes, asset.contentType)
            }
        }
    }
}

private val SecurityHeaders = createApplicationPlugin("SecurityHeaders") {
    onCall { call ->
        val h = call.response.headers
        h.append("X-Content-Type-Options", "nosniff")
        h.append("Referrer-Policy", "no-referrer")
        h.append("X-Frame-Options", "DENY")
        h.append(
            "Content-Security-Policy",
            "default-src 'self'; img-src 'self' blob: data:; style-src 'self' 'unsafe-inline'; " +
                "connect-src 'self' wss: ws:; frame-ancestors 'none'; base-uri 'none'; form-action 'self'",
        )
        if (call.request.headers["X-Forwarded-Proto"] == "https") {
            h.append("Strict-Transport-Security", "max-age=31536000")
        }
    }
}

private fun ApplicationCall.bearer(): String {
    val header = request.headers[HttpHeaders.Authorization]
    if (header == null || !header.startsWith("Bearer ", ignoreCase = true)) {
        throw ApiException(HttpStatusCode.Unauthorized, ErrorCodes.UNAUTHORIZED, "Nicht angemeldet.")
    }
    return header.substring(7).trim()
}

private fun ApplicationCall.param(name: String): String =
    parameters[name] ?: throw ApiException(HttpStatusCode.BadRequest, ErrorCodes.INVALID_REQUEST, "Parameter $name fehlt.")

private fun ApplicationCall.classId(): String = param("classId")

/**
 * Absender-IP für Rate-Limits. Hinter der Tailscale-Brücke kommt jede Verbindung von 127.0.0.1; dann gilt
 * der erste Eintrag aus X-Forwarded-For, den nur die Brücke setzt (sie entfernt eingehende Werte).
 */
internal fun ApplicationCall.clientIp(config: ServerConfig): String {
    val remote = request.local.remoteAddress
    if (config.trustLocalProxy && (remote == "127.0.0.1" || remote == "::1" || remote == "localhost")) {
        request.headers["X-Forwarded-For"]?.split(',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    }
    return remote
}

private suspend fun ApplicationCall.receiveLimited(max: Int): ByteArray {
    val length = request.contentLength()
    if (length != null && length > max) {
        throw ApiException(HttpStatusCode.PayloadTooLarge, ErrorCodes.PAYLOAD_TOO_LARGE, "Das Bild ist größer als 1 MB.")
    }
    val bytes = receiveChannel().readRemaining((max + 1).toLong()).readByteArray()
    if (bytes.size > max) {
        throw ApiException(HttpStatusCode.PayloadTooLarge, ErrorCodes.PAYLOAD_TOO_LARGE, "Das Bild ist größer als 1 MB.")
    }
    return bytes
}
