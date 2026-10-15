package com.realcraftguy.classview.host

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.realcraftguy.classview.host.mockup.HostInviteScreen
import com.realcraftguy.classview.host.mockup.HostMainScreen
import com.realcraftguy.classview.host.mockup.HostSetupScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rendert die Host-Mockups als PNG nach docs/mockups/android/host (S21 FE ≈ 411 × 891 dp).
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

    @Test fun setup() = shot("01-einrichtung") { HostSetupScreen() }
    @Test fun running() = shot("02-server-laeuft") { HostMainScreen() }
    @Test fun stoppedDark() = shot("03-server-gestoppt-dunkel", dark = true) { HostMainScreen(running = false) }
    @Test fun invite() = shot("04-einladung") { HostInviteScreen() }
}
