package com.realcraftguy.classview.host

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.realcraftguy.classview.host.ui.InviteScreen
import com.realcraftguy.classview.host.ui.MainScreen
import com.realcraftguy.classview.host.ui.SetupScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Host-Bildschirme mit Beispielzustand nach docs/mockups/android/host (S21 FE ≈ 411 × 891 dp).
 * Aufnehmen: ./gradlew :android:app-host:recordRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
class MockupScreenshots {

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        val dir = System.getProperty("mockupDir") ?: "build/mockups"
        captureRoboImage("$dir/$name.png") { ClassviewTheme(darkTheme = dark) { content() } }
    }

    private val running = HostStatus(
        serviceRunning = true,
        tailscale = TailscaleState("running", "classview.tail4b2c1.ts.net"),
        reachable = true, classes = 1, devices = 14,
    )

    @Composable
    private fun main(status: HostStatus, uptime: String? = "2 Std. 14 Min.") = MainScreen(
        status = status, charging = true, batteryUnrestricted = true, uptime = uptime,
        onToggle = {}, onInvite = {}, onBatteryHints = {}, onRequestBatteryExemption = {}, onLogs = {}, onResetTailscale = {},
    )

    @Test fun setup() = shot("01-einrichtung") { SetupScreen("", {}, {}) }
    @Test fun running() = shot("02-server-laeuft") { main(running) }
    @Test fun stoppedDark() = shot("03-server-gestoppt-dunkel", dark = true) { main(HostStatus(), uptime = null) }
    @Test fun invite() = shot("04-einladung") {
        InviteScreen("https://classview.tail4b2c1.ts.net/#/invite/beispiel-schluessel", {}, {}, {})
    }
    @Test fun error() = shot("05-fehler") {
        main(
            HostStatus(
                serviceRunning = true,
                tailscale = TailscaleState("error", error = "Funnel ist für dieses Gerät nicht erlaubt. Im Tailscale-Adminbereich Funnel aktivieren."),
            ),
        )
    }
}
