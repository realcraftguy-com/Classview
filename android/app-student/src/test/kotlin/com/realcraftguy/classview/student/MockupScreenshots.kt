package com.realcraftguy.classview.student

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.realcraftguy.classview.student.data.Membership
import com.realcraftguy.classview.student.data.Profile
import com.realcraftguy.classview.student.ui.HintStep
import com.realcraftguy.classview.student.ui.HintsScreen
import com.realcraftguy.classview.student.ui.JoinScreen
import com.realcraftguy.classview.student.ui.JoinedScreen
import com.realcraftguy.classview.student.ui.ProfileScreen
import com.realcraftguy.classview.student.ui.ServerScreen
import com.realcraftguy.classview.student.ui.StartScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Bildschirme der Schüler-App mit Beispielzustand nach docs/mockups/android/student
 * (Pad 7 ≈ 1333 × 890 dp). Aufnehmen: ./gradlew :android:app-student:recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MockupScreenshots {

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        val dir = System.getProperty("mockupDir") ?: "build/mockups"
        captureRoboImage("$dir/$name.png") { ClassviewTheme(darkTheme = dark) { content() } }
    }

    private val felix = Profile("Felix", "welle", 0, false)
    private val membership = Membership("https://classview.tail4b2c1.ts.net", "c", "d", "t", "7b · Informatik", "Frau Berger")
    private val host = "classview.tail4b2c1.ts.net"

    @Test @Config(qualifiers = LAND)
    fun start() = shot("01-start") { StartScreen(felix, null, null, {}, {}, {}) }

    @Test @Config(qualifiers = LAND)
    fun startNew() = shot("01b-start-neu") { StartScreen(Profile(), null, "Du wurdest aus der Klasse entfernt.", {}, {}, {}) }

    @Test @Config(qualifiers = PORT)
    fun profile() = shot("02-profil") { ProfileScreen(felix, null, false, { _, _ -> }, {}, {}, {}) }

    @Test @Config(qualifiers = LAND)
    fun join() = shot("03-beitreten") { JoinScreen("K7P4", null, false, null, host, {}, {}, {}, {}, autoFocus = false) }

    @Test @Config(qualifiers = LAND)
    fun joinLoading() = shot("04-beitreten-laedt") { JoinScreen("K7P4XM", null, true, null, host, {}, {}, {}, {}, autoFocus = false) }

    @Test @Config(qualifiers = LAND)
    fun joinError() = shot("05-beitreten-fehler") {
        JoinScreen("K7P4XN", null, false, "Diesen Code gibt es nicht. Prüfe ihn noch einmal.", host, {}, {}, {}, {}, autoFocus = false)
    }

    @Test @Config(qualifiers = LAND)
    fun joinNoServer() = shot("05b-beitreten-ohne-server") { JoinScreen("", null, false, null, null, {}, {}, {}, {}, autoFocus = false) }

    @Test @Config(qualifiers = PORT)
    fun server() = shot("05c-server-adresse") { ServerScreen("", false, null, {}, {}) }

    @Test @Config(qualifiers = LAND)
    fun joined() = shot("06-beigetreten") { JoinedScreen(membership, felix, null, ConnectionState.Connected, null, {}, {}, {}) }

    @Test @Config(qualifiers = LAND)
    fun joinedDark() = shot("07-beigetreten-dunkel", dark = true) { JoinedScreen(membership, felix, null, ConnectionState.Connected, null, {}, {}, {}) }

    @Test @Config(qualifiers = PORT)
    fun joinedOffline() = shot("08-beigetreten-hochformat-offline") {
        JoinedScreen(membership, felix, null, ConnectionState.Offline, "Keine Verbindung zum Server. Bist du im Internet?", {}, {}, {})
    }

    @Test @Config(qualifiers = LAND)
    fun hints() = shot("13-hyperos-hinweis") {
        HintsScreen(
            listOf(
                HintStep("Autostart erlauben", "Damit die App nach einem Neustart wieder verbunden ist.", true, "Öffnen", {}, {}),
                HintStep("Akku: Keine Einschränkungen", "Sonst beendet HyperOS die Verbindung im Hintergrund.", true, "Öffnen", {}),
                HintStep("Benachrichtigungen erlauben", "Damit du Freigabe-Anfragen auch siehst, wenn eine andere App offen ist.", false, "Erlauben", {}),
                HintStep("App in „Zuletzt verwendet“ sperren", "Übersicht öffnen, ClassroomView gedrückt halten und auf das Schloss tippen.", false, "Erledigt", {}, {}),
            ),
            {}, {},
        )
    }

    companion object {
        const val LAND = "w1333dp-h890dp-land-xhdpi"
        const val PORT = "w890dp-h1333dp-port-xhdpi"
    }
}
