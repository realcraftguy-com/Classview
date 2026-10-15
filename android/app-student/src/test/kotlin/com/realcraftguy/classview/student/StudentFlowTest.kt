package com.realcraftguy.classview.student

import com.realcraftguy.classview.protocol.CodeTtl
import com.realcraftguy.classview.protocol.CreateClassRequest
import com.realcraftguy.classview.protocol.ErrorCodes
import com.realcraftguy.classview.server.ClassService
import com.realcraftguy.classview.server.ClassviewServer
import com.realcraftguy.classview.server.JvmImageProcessor
import com.realcraftguy.classview.server.MemoryAvatarStore
import com.realcraftguy.classview.server.MemoryDataStore
import com.realcraftguy.classview.server.ServerConfig
import com.realcraftguy.classview.server.WebContent
import com.realcraftguy.classview.student.data.ServerError
import com.realcraftguy.classview.student.data.Status
import com.realcraftguy.classview.student.data.StudentRepository
import com.realcraftguy.classview.student.data.StudentStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.net.ServerSocket

/**
 * Spielt den Ablauf der Schüler-App gegen den echten Server durch (läuft im Testprozess auf 127.0.0.1).
 * Ein Test für den ganzen Ablauf, weil DataStore pro Prozess nur eine Instanz zulässt.
 */
@RunWith(RobolectricTestRunner::class)
class StudentFlowTest {
    private val port = ServerSocket(0).use { it.localPort }
    private val service = ClassService(MemoryDataStore(), MemoryAvatarStore(), JvmImageProcessor())
    private val server = ClassviewServer(service, ServerConfig("test", false, WebContent.Placeholder), port = port)

    @After fun stop() = server.stop()

    @Test
    fun beitretenProfilOfflineEntfernen() = runBlocking {
        server.start()
        val repo = StudentRepository(StudentStore(RuntimeEnvironment.getApplication()))
        repo.store.clearMembership()

        val key = service.rotateInviteKey()
        val cls = service.createClass(CreateClassRequest("7b · Informatik", "Frau Berger", CodeTtl.DAY, key), "127.0.0.1")

        // Ohne Namen kein Beitritt
        repo.store.saveProfile("", null)
        expectError("no_name") { repo.join(cls.code) }

        // Server-Adresse prüfen und speichern (http nur im Test/Debug)
        val url = repo.setServer("http://127.0.0.1:$port/")
        assertEquals("http://127.0.0.1:$port", url)
        expectError(ServerError.NETWORK) { repo.setServer("http://127.0.0.1:1") }
        assertEquals(url, repo.store.currentSettings().serverUrl) // falsche Adresse überschreibt nichts

        // Profil anlegen, falscher Code, dann richtiger Code in Kleinschrift mit Trennzeichen
        repo.saveProfile("  Felix  ", "welle")
        val wrong = if (cls.code == "ZZZZZZ") "YYYYYY" else "ZZZZZZ"
        expectError(ErrorCodes.CODE_INVALID) { repo.join(wrong) }
        val m = repo.join(cls.code.lowercase().let { it.take(3) + "-" + it.drop(3) })
        assertEquals("7b · Informatik", m.className)
        assertEquals("Frau Berger", m.teacherName)

        var snap = service.snapshot(cls.classId, cls.teacherToken)
        assertEquals("Felix", snap.devices.single().displayName)
        assertEquals("welle", snap.devices.single().avatarPreset)

        // Gespeicherte Klasse: neuer Repository-Start verbindet sich ohne Code wieder
        val again = StudentRepository(StudentStore(RuntimeEnvironment.getApplication()))
        assertEquals(Status.Connected, again.refresh())

        // Profiländerung wird sofort übertragen
        repo.saveProfile("Felix M.", "mond")
        snap = service.snapshot(cls.classId, cls.teacherToken)
        assertEquals("Felix M.", snap.devices.single().displayName)
        assertEquals("mond", snap.devices.single().avatarPreset)

        // Server weg: Mitgliedschaft bleibt, Änderung wird vorgemerkt …
        server.stop()
        assertTrue(repo.refresh() is Status.Offline)
        repo.saveProfile("Felix Offline", "mond")
        assertTrue(repo.store.currentSettings().profileDirty)
        assertEquals(m.deviceId, repo.store.currentMembership()?.deviceId)

        // … und beim nächsten Kontakt nachgeholt
        server.start()
        assertEquals(Status.Connected, repo.refresh())
        assertEquals("Felix Offline", service.snapshot(cls.classId, cls.teacherToken).devices.single().displayName)
        assertTrue(!repo.store.currentSettings().profileDirty)

        // Code erneuern ändert nichts für beigetretene Geräte
        service.rotateCode(cls.classId, cls.teacherToken)
        assertEquals(Status.Connected, repo.refresh())

        // Lehrkraft entfernt das Gerät → App erkennt es und löscht die Mitgliedschaft
        service.removeDevice(cls.classId, cls.teacherToken, m.deviceId)
        val removed = repo.refresh()
        assertTrue(removed is Status.Removed)
        assertEquals("Du wurdest aus der Klasse entfernt.", (removed as Status.Removed).message)
        assertNull(repo.store.currentMembership())

        // Erneut beitreten und selbst verlassen
        val code = service.snapshot(cls.classId, cls.teacherToken).code!!
        repo.join(code)
        assertEquals(1, service.snapshot(cls.classId, cls.teacherToken).devices.size)
        assertTrue(repo.leave())
        assertEquals(0, service.snapshot(cls.classId, cls.teacherToken).devices.size)
        assertNull(repo.store.currentMembership())
    }

    @Test
    fun codeEingabeWirdBereinigt() {
        assertEquals("K7P4XM" to null, StudentRepository.sanitizeCode("k7p-4xm"))
        assertEquals("K7P4XM", StudentRepository.sanitizeCode("K7P4XM99").first)
        val (code, hint) = StudentRepository.sanitizeCode("K0P")
        assertEquals("KP", code)
        assertEquals("Codes enthalten kein 0, O, 1, I oder L.", hint)
    }

    private suspend fun expectError(code: String, block: suspend () -> Unit) {
        try {
            block()
            fail("Fehler $code erwartet")
        } catch (e: ServerError) {
            assertEquals(code, e.code)
        }
    }
}
