package com.realcraftguy.classview.host

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.realcraftguy.classview.host.mockup.HostInviteScreen
import com.realcraftguy.classview.host.mockup.HostMainScreen
import com.realcraftguy.classview.host.mockup.HostSetupScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import com.realcraftguy.classview.ui.MockupGallery

/** Phase 2: Galerie aller Mockup-Bildschirme. TODO: in Phase 3 durch die echte Host-App ersetzen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassviewTheme {
                var current by rememberSaveable { mutableStateOf<String?>(null) }
                BackHandler(enabled = current != null) { current = null }
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    val screen = screens.firstOrNull { it.first == current }
                    if (screen == null) {
                        MockupGallery("ClassroomView Host – Mockups", screens.map { it.first }) { current = it }
                    } else {
                        screen.second()
                    }
                }
            }
        }
    }
}

private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
    "Einrichtung" to { HostSetupScreen() },
    "Server läuft" to { HostMainScreen() },
    "Server gestoppt" to { HostMainScreen(running = false) },
    "Einladungslink" to { HostInviteScreen() },
)
