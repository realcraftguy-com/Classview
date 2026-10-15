package com.realcraftguy.classview.student

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.realcraftguy.classview.student.mockup.Connection
import com.realcraftguy.classview.student.mockup.HyperOsHintScreen
import com.realcraftguy.classview.student.mockup.JoinScreen
import com.realcraftguy.classview.student.mockup.JoinState
import com.realcraftguy.classview.student.mockup.JoinedScreen
import com.realcraftguy.classview.student.mockup.NotificationIllustration
import com.realcraftguy.classview.student.mockup.ProfileScreen
import com.realcraftguy.classview.student.mockup.ShareRequestDialog
import com.realcraftguy.classview.student.mockup.StartScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Mockups als PNG nach docs/mockups/android/student (Pad 7 ≈ 1333 × 890 dp).
 * Aufnehmen: ./gradlew :android:app-student:recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MockupScreenshots {

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        val dir = System.getProperty("mockupDir") ?: "build/mockups"
        captureRoboImage("$dir/$name.png") { ClassviewTheme(darkTheme = dark) { content() } }
    }

    @Test @Config(qualifiers = LAND)
    fun start() = shot("01-start") { StartScreen() }

    @Test @Config(qualifiers = PORT)
    fun profile() = shot("02-profil") { ProfileScreen() }

    @Test @Config(qualifiers = LAND)
    fun join() = shot("03-beitreten") { JoinScreen() }

    @Test @Config(qualifiers = LAND)
    fun joinLoading() = shot("04-beitreten-laedt") { JoinScreen(JoinState.Loading("K7P4XM")) }

    @Test @Config(qualifiers = LAND)
    fun joinError() = shot("05-beitreten-fehler") {
        JoinScreen(JoinState.Error("K7P4XN", "Diesen Code gibt es nicht. Prüfe ihn noch einmal."))
    }

    @Test @Config(qualifiers = LAND)
    fun joined() = shot("06-beigetreten") { JoinedScreen() }

    @Test @Config(qualifiers = LAND)
    fun joinedDark() = shot("07-beigetreten-dunkel", dark = true) { JoinedScreen() }

    @Test @Config(qualifiers = PORT)
    fun joinedReconnecting() = shot("08-beigetreten-hochformat-verbindung-weg") { JoinedScreen(connection = Connection.Reconnecting) }

    @Test @Config(qualifiers = LAND)
    fun request() = shot("09-freigabe-anfrage") { JoinedScreen(overlay = { ShareRequestDialog() }) }

    @Test @Config(qualifiers = LAND)
    fun requestDark() = shot("10-freigabe-anfrage-dunkel", dark = true) { JoinedScreen(overlay = { ShareRequestDialog() }) }

    @Test @Config(qualifiers = LAND)
    fun sharing() = shot("11-freigabe-aktiv") { JoinedScreen(sharing = true) }

    @Test @Config(qualifiers = PORT)
    fun sharingPortrait() = shot("12-freigabe-aktiv-hochformat") { JoinedScreen(sharing = true) }

    @Test @Config(qualifiers = LAND)
    fun hyperOs() = shot("13-hyperos-hinweis") { HyperOsHintScreen() }

    @Test @Config(qualifiers = PORT)
    fun notifications() = shot("14-benachrichtigungen") { NotificationIllustration() }

    companion object {
        const val LAND = "w1333dp-h890dp-land-xhdpi"
        const val PORT = "w890dp-h1333dp-port-xhdpi"
    }
}
