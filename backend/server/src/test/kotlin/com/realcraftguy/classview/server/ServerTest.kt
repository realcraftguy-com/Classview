package com.realcraftguy.classview.server

import com.realcraftguy.classview.protocol.ClassSnapshot
import com.realcraftguy.classview.protocol.CodeResponse
import com.realcraftguy.classview.protocol.CodeTtl
import com.realcraftguy.classview.protocol.CreateClassRequest
import com.realcraftguy.classview.protocol.CreateClassResponse
import com.realcraftguy.classview.protocol.ErrorCodes
import com.realcraftguy.classview.protocol.ErrorResponse
import com.realcraftguy.classview.protocol.InfoResponse
import com.realcraftguy.classview.protocol.JoinRequest
import com.realcraftguy.classview.protocol.JoinResponse
import com.realcraftguy.classview.protocol.MeResponse
import com.realcraftguy.classview.protocol.ProtocolJson
import com.realcraftguy.classview.protocol.RecoverRequest
import com.realcraftguy.classview.protocol.RecoverResponse
import com.realcraftguy.classview.protocol.RecoveryTokenResponse
import com.realcraftguy.classview.protocol.TeacherDto
import com.realcraftguy.classview.protocol.UpdateClassRequest
import com.realcraftguy.classview.protocol.UpdateDeviceRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerTest {
    private var now = 1_800_000_000_000L
    private val avatars = MemoryAvatarStore()
    private val service = ClassService(MemoryDataStore(), avatars, JvmImageProcessor(), clock = { now })
    private val inviteKey = runBlocking { service.rotateInviteKey() }
    private val config = ServerConfig("test", trustLocalProxy = true, web = WebContent.Placeholder)

    private fun withServer(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) = testApplication {
        application { classviewModule(service, config) }
        val client = createClient { install(ContentNegotiation) { json(ProtocolJson) } }
        block(client)
    }

    private suspend fun HttpClient.createClass(
        name: String = "7b · Informatik",
        ttl: CodeTtl = CodeTtl.DAY,
        key: String = inviteKey,
    ): HttpResponse = post("/api/classes") {
        contentType(ContentType.Application.Json)
        header("X-Forwarded-For", "203.0.113.1")
        setBody(CreateClassRequest(name, "Frau Berger", ttl, key))
    }

    private suspend fun HttpClient.join(code: String, name: String = "Felix", ip: String = "198.51.100.7"): HttpResponse =
        post("/api/join") {
            contentType(ContentType.Application.Json)
            header("X-Forwarded-For", ip)
            setBody(JoinRequest(code, name, "welle"))
        }

    private suspend fun HttpResponse.error(): String = body<ErrorResponse>().error

    // ------------------------------------------------------------------ Grundlagen

    @Test
    fun `info liefert Protokollversion`() = withServer { c ->
        val info = c.get("/api/info").body<InfoResponse>()
        assertEquals(1, info.protocolVersion)
    }

    @Test
    fun `Codes verwenden nur unverwechselbare Zeichen`() {
        val secrets = Secrets()
        repeat(5_000) {
            val code = secrets.classCode()
            assertEquals(6, code.length)
            assertTrue(code.none { it in "01OIL" }, code)
        }
        assertEquals("K7P4XM", Secrets.normalizeCode(" k7p-4xm "))
        assertEquals("K7P4XM", Secrets.normalizeCode("K7P·4XM"))
        assertNull(Secrets.normalizeCode("K7P4X0"))
        assertNull(Secrets.normalizeCode("K7P4X"))
    }

    // ------------------------------------------------------------------ Klassen

    @Test
    fun `Klasse anlegen braucht den Einladungsschluessel`() = withServer { c ->
        val r = c.createClass(key = "falsch")
        assertEquals(HttpStatusCode.Forbidden, r.status)
        assertEquals(ErrorCodes.INVITE_REQUIRED, r.error())
    }

    @Test
    fun `Klasse anlegen liefert Code und Lehrer-Token`() = withServer { c ->
        val created = c.createClass().also { assertEquals(HttpStatusCode.Created, it.status) }.body<CreateClassResponse>()
        assertEquals(6, created.code.length)
        assertEquals(now + CodeTtl.DAY.millis!!, created.codeExpiresAt)

        val snap = c.get("/api/classes/${created.classId}") { bearerAuth(created.teacherToken) }.body<ClassSnapshot>()
        assertEquals("7b · Informatik", snap.name)
        assertEquals("Frau Berger", snap.teacherName)
        assertEquals(created.code, snap.code)
        assertTrue(snap.devices.isEmpty())
    }

    @Test
    fun `Lehrer-Token gilt nur fuer die eigene Klasse`() = withServer { c ->
        val a = c.createClass("A").body<CreateClassResponse>()
        val b = c.createClass("B").body<CreateClassResponse>()
        val r = c.get("/api/classes/${b.classId}") { bearerAuth(a.teacherToken) }
        assertEquals(HttpStatusCode.Forbidden, r.status)
        assertEquals(HttpStatusCode.Unauthorized, c.get("/api/classes/${a.classId}").status)
        assertEquals(HttpStatusCode.Unauthorized, c.get("/api/classes/${a.classId}") { bearerAuth("unsinn") }.status)
    }

    @Test
    fun `Leerer oder zu langer Klassenname wird abgelehnt`() = withServer { c ->
        assertEquals(HttpStatusCode.BadRequest, c.createClass(name = "   ").status)
        assertEquals(HttpStatusCode.BadRequest, c.createClass(name = "x".repeat(61)).status)
    }

    // ------------------------------------------------------------------ Beitritt

    @Test
    fun `Beitritt mit gueltigem Code erscheint in der Klasse`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val joined = c.join(cls.code.lowercase().chunked(3).joinToString("-")).also { assertEquals(HttpStatusCode.Created, it.status) }
            .body<JoinResponse>()
        assertEquals(cls.classId, joined.classId)
        assertEquals("Frau Berger", joined.teacherName)

        val snap = c.get("/api/classes/${cls.classId}") { bearerAuth(cls.teacherToken) }.body<ClassSnapshot>()
        assertEquals(listOf("Felix"), snap.devices.map { it.displayName })
        assertEquals(joined.deviceId, snap.devices.single().id)

        val me = c.get("/api/devices/me") { bearerAuth(joined.deviceToken) }.body<MeResponse>()
        assertEquals("welle", me.avatarPreset)
    }

    @Test
    fun `Falscher, abgelaufener und pausierter Code`() = withServer { c ->
        val cls = c.createClass(ttl = CodeTtl.HOUR).body<CreateClassResponse>()
        val wrong = if (cls.code != "ZZZZZZ") "ZZZZZZ" else "YYYYYY"
        c.join(wrong).let { assertEquals(HttpStatusCode.NotFound, it.status); assertEquals(ErrorCodes.CODE_INVALID, it.error()) }

        c.patch("/api/classes/${cls.classId}") {
            bearerAuth(cls.teacherToken); contentType(ContentType.Application.Json); setBody(UpdateClassRequest(joinPaused = true))
        }
        assertEquals(ErrorCodes.JOIN_PAUSED, c.join(cls.code).error())

        c.patch("/api/classes/${cls.classId}") {
            bearerAuth(cls.teacherToken); contentType(ContentType.Application.Json); setBody(UpdateClassRequest(joinPaused = false))
        }
        now += CodeTtl.HOUR.millis!! + 1
        c.join(cls.code).let { assertEquals(HttpStatusCode.Gone, it.status); assertEquals(ErrorCodes.CODE_EXPIRED, it.error()) }
    }

    @Test
    fun `Code erneuern sperrt den alten Code, beigetretene Geraete bleiben`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val joined = c.join(cls.code).body<JoinResponse>()

        val newCode = c.post("/api/classes/${cls.classId}/code") { bearerAuth(cls.teacherToken) }.body<CodeResponse>()
        assertNotEquals(cls.code, newCode.code)
        assertEquals(ErrorCodes.CODE_INVALID, c.join(cls.code, "Mia").error())
        assertEquals(HttpStatusCode.Created, c.join(newCode.code, "Mia").status)

        assertEquals(HttpStatusCode.OK, c.get("/api/devices/me") { bearerAuth(joined.deviceToken) }.status)
    }

    @Test
    fun `Entfernte und ausgetretene Geraete verlieren den Zugang`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val felix = c.join(cls.code).body<JoinResponse>()
        val mia = c.join(cls.code, "Mia").body<JoinResponse>()

        assertEquals(HttpStatusCode.NoContent, c.delete("/api/classes/${cls.classId}/devices/${felix.deviceId}") { bearerAuth(cls.teacherToken) }.status)
        c.get("/api/devices/me") { bearerAuth(felix.deviceToken) }.let {
            assertEquals(HttpStatusCode.Unauthorized, it.status)
            assertEquals(ErrorCodes.DEVICE_REMOVED, it.error())
        }

        assertEquals(HttpStatusCode.NoContent, c.delete("/api/devices/me") { bearerAuth(mia.deviceToken) }.status)
        assertEquals(ErrorCodes.DEVICE_REMOVED, c.get("/api/devices/me") { bearerAuth(mia.deviceToken) }.error())

        val snap = c.get("/api/classes/${cls.classId}") { bearerAuth(cls.teacherToken) }.body<ClassSnapshot>()
        assertTrue(snap.devices.isEmpty())
    }

    @Test
    fun `Volle Klasse nimmt niemanden mehr auf`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        repeat(40) { i -> assertEquals(HttpStatusCode.Created, c.join(cls.code, "S$i").status) }
        assertEquals(ErrorCodes.CLASS_FULL, c.join(cls.code, "S40").error())
    }

    @Test
    fun `Profil aendern`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val felix = c.join(cls.code).body<JoinResponse>()
        val me = c.patch("/api/devices/me") {
            bearerAuth(felix.deviceToken); contentType(ContentType.Application.Json)
            setBody(UpdateDeviceRequest(displayName = "  Felix   M.  ", avatarPreset = "mond"))
        }.body<MeResponse>()
        assertEquals("Felix M.", me.displayName)
        assertEquals("mond", me.avatarPreset)
        val bad = c.patch("/api/devices/me") {
            bearerAuth(felix.deviceToken); contentType(ContentType.Application.Json); setBody(UpdateDeviceRequest(avatarPreset = "fremd"))
        }
        assertEquals(HttpStatusCode.BadRequest, bad.status)
    }

    // ------------------------------------------------------------------ Rate-Limits

    @Test
    fun `Durchprobieren von Codes wird gebremst`() = withServer { c ->
        val real = c.createClass().body<CreateClassResponse>().code
        val codes = Secrets.CODE_ALPHABET.map { "AAAAA$it" }.filter { it != real }.take(Limits.JOIN_FAILURES_PER_IP)
        codes.forEach { assertEquals(ErrorCodes.CODE_INVALID, c.join(it, ip = "192.0.2.66").error()) }
        val blocked = c.join(real, ip = "192.0.2.66")
        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertTrue(blocked.headers["Retry-After"]!!.toInt() > 0)

        // Andere IP ist nicht betroffen; nach 10 Minuten geht es wieder.
        assertEquals(ErrorCodes.CODE_INVALID, c.join("MMMMMM", ip = "192.0.2.77").error())
        now += Limits.TEN_MINUTES + 1
        assertEquals(ErrorCodes.CODE_INVALID, c.join("MMMMMM", ip = "192.0.2.66").error())
    }

    @Test
    fun `Eine ganze Klasse hinter einer Schul-IP kann beitreten`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        // Zwei Tippfehler zwischendurch, danach 30 erfolgreiche Beitritte von derselben IP
        c.join("AAAAAA", ip = "192.0.2.10"); c.join("BBBBBB", ip = "192.0.2.10")
        repeat(30) { i -> assertEquals(HttpStatusCode.Created, c.join(cls.code, "S$i", ip = "192.0.2.10").status, "Schüler $i") }
    }

    // ------------------------------------------------------------------ Wiederherstellung

    @Test
    fun `Wiederherstellungslink erzeugt ein weiteres Lehrer-Geraet`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val rec = c.post("/api/classes/recover") {
            contentType(ContentType.Application.Json); setBody(RecoverRequest(cls.recoveryToken, "Mac · Safari"))
        }.body<RecoverResponse>()
        assertEquals(cls.classId, rec.classId)
        assertEquals(HttpStatusCode.OK, c.get("/api/classes/${cls.classId}") { bearerAuth(rec.teacherToken) }.status)

        val teachers = c.get("/api/classes/${cls.classId}/teachers") { bearerAuth(cls.teacherToken) }.body<List<TeacherDto>>()
        assertEquals(2, teachers.size)
        val other = teachers.single { !it.current }
        assertEquals("Mac · Safari", other.label)

        // Anderes Gerät abmelden → dessen Token gilt nicht mehr
        c.delete("/api/classes/${cls.classId}/teachers/${other.id}") { bearerAuth(cls.teacherToken) }
        assertEquals(HttpStatusCode.Unauthorized, c.get("/api/classes/${cls.classId}") { bearerAuth(rec.teacherToken) }.status)

        // Link erneuern → alter Link ungültig
        val fresh = c.post("/api/classes/${cls.classId}/recovery") { bearerAuth(cls.teacherToken) }.body<RecoveryTokenResponse>()
        val old = c.post("/api/classes/recover") { contentType(ContentType.Application.Json); setBody(RecoverRequest(cls.recoveryToken)) }
        assertEquals(HttpStatusCode.NotFound, old.status)
        val ok = c.post("/api/classes/recover") { contentType(ContentType.Application.Json); setBody(RecoverRequest(fresh.recoveryToken)) }
        assertEquals(HttpStatusCode.OK, ok.status)
    }

    // ------------------------------------------------------------------ Profilbilder

    private fun pngBytes(w: Int, h: Int): ByteArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        return ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
    }

    @Test
    fun `Profilbild wird verkleinert und ist nur fuer die Lehrkraft abrufbar`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val other = c.createClass("Andere").body<CreateClassResponse>()
        val felix = c.join(cls.code).body<JoinResponse>()

        val up = c.put("/api/devices/me/avatar") { bearerAuth(felix.deviceToken); setBody(pngBytes(1200, 800)) }
        assertEquals(HttpStatusCode.OK, up.status)
        assertTrue(up.body<MeResponse>().hasImage)

        val res = c.get("/api/classes/${cls.classId}/devices/${felix.deviceId}/avatar") { bearerAuth(cls.teacherToken) }
        assertEquals(HttpStatusCode.OK, res.status)
        val img = ImageIO.read(res.readRawBytes().inputStream())
        assertEquals(512, img.width)
        assertEquals(341, img.height)

        assertEquals(HttpStatusCode.Unauthorized, c.get("/api/classes/${cls.classId}/devices/${felix.deviceId}/avatar").status)
        assertEquals(HttpStatusCode.Forbidden,
            c.get("/api/classes/${cls.classId}/devices/${felix.deviceId}/avatar") { bearerAuth(other.teacherToken) }.status)
    }

    @Test
    fun `Kein Bild oder zu grosses Bild wird abgelehnt`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val felix = c.join(cls.code).body<JoinResponse>()
        val text = c.put("/api/devices/me/avatar") { bearerAuth(felix.deviceToken); setBody("<svg></svg>".toByteArray()) }
        assertEquals(HttpStatusCode.UnsupportedMediaType, text.status)
        val big = c.put("/api/devices/me/avatar") { bearerAuth(felix.deviceToken); setBody(ByteArray(1_000_001)) }
        assertEquals(HttpStatusCode.PayloadTooLarge, big.status)
        val huge = c.put("/api/devices/me/avatar") { bearerAuth(felix.deviceToken); setBody(pngBytes(5000, 10)) }
        assertEquals(HttpStatusCode.UnsupportedMediaType, huge.status)
    }

    @Test
    fun `Klasse loeschen entfernt Geraete und Bilder`() = withServer { c ->
        val cls = c.createClass().body<CreateClassResponse>()
        val felix = c.join(cls.code).body<JoinResponse>()
        c.put("/api/devices/me/avatar") { bearerAuth(felix.deviceToken); setBody(pngBytes(64, 64)) }
        assertEquals(1, avatars.size)
        assertEquals(HttpStatusCode.NoContent, c.delete("/api/classes/${cls.classId}") { bearerAuth(cls.teacherToken) }.status)
        assertEquals(0, avatars.size)
        assertEquals(HttpStatusCode.Unauthorized, c.get("/api/devices/me") { bearerAuth(felix.deviceToken) }.status)
    }

    // ------------------------------------------------------------------ Speicherung

    @Test
    fun `Datei enthaelt keine Klartext-Tokens und uebersteht einen Neustart`() {
        val dir = Files.createTempDirectory("classview").toFile()
        val file = File(dir, "classview.json")
        val first = ClassService(JsonFileDataStore(file), MemoryAvatarStore(), JvmImageProcessor())
        val key = runBlocking { first.rotateInviteKey() }
        val created = runBlocking { first.createClass(CreateClassRequest("7b", null, CodeTtl.NEVER, key), "1.2.3.4") }
        val joined = runBlocking { first.join(JoinRequest(created.code, "Felix"), "1.2.3.4") }

        val text = file.readText()
        listOf(key, created.teacherToken, created.recoveryToken, joined.deviceToken).forEach {
            assertFalse(text.contains(it), "Klartext-Geheimnis in der Datei gefunden")
        }

        val second = ClassService(JsonFileDataStore(file), MemoryAvatarStore(), JvmImageProcessor())
        val snap = runBlocking { second.snapshot(created.classId, created.teacherToken) }
        assertEquals(listOf("Felix"), snap.devices.map { it.displayName })
        assertNull(snap.codeExpiresAt)
        dir.deleteRecursively()
    }

    @Test
    fun `Unbekannte API-Pfade liefern JSON-Fehler, sonst kommt die Web-App`() = withServer { c ->
        assertEquals(HttpStatusCode.NotFound, c.get("/api/gibtsnicht").status)
        val page = c.get("/irgendwas")
        assertEquals(HttpStatusCode.OK, page.status)
        assertEquals("nosniff", page.headers["X-Content-Type-Options"])
    }
}
