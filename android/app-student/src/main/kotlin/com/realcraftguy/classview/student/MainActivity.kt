package com.realcraftguy.classview.student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
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
import com.realcraftguy.classview.ui.MockupGallery

/** Phase 2: Galerie aller Mockup-Bildschirme. TODO: in Phase 4 durch die echte Navigation ersetzen. */
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
                        MockupGallery("ClassroomView – Mockups", screens.map { it.first }) { current = it }
                    } else {
                        screen.second()
                    }
                }
            }
        }
    }
}

private val screens: List<Pair<String, @Composable () -> Unit>> = listOf(
    "Start" to { StartScreen() },
    "Mein Profil" to { ProfileScreen() },
    "Beitreten" to { JoinScreen() },
    "Beitreten – lädt" to { JoinScreen(JoinState.Loading("K7P4XM")) },
    "Beitreten – falscher Code" to { JoinScreen(JoinState.Error("K7P4XN", "Diesen Code gibt es nicht. Prüfe ihn noch einmal.")) },
    "Beitreten – abgelaufen" to { JoinScreen(JoinState.Error("K7P4XM", "Dieser Code ist abgelaufen. Frag nach einem neuen.")) },
    "Beitreten – keine Verbindung" to { JoinScreen(JoinState.Error("K7P4XM", "Keine Verbindung zum Server. Bist du im WLAN?")) },
    "Beigetreten" to { JoinedScreen() },
    "Beigetreten – Verbindung weg" to { JoinedScreen(connection = Connection.Reconnecting) },
    "Freigabe-Anfrage" to { JoinedScreen(overlay = { ShareRequestDialog() }) },
    "Freigabe aktiv" to { JoinedScreen(sharing = true) },
    "HyperOS-Hinweis" to { HyperOsHintScreen() },
    "Benachrichtigungen (Illustration)" to { NotificationIllustration() },
)
