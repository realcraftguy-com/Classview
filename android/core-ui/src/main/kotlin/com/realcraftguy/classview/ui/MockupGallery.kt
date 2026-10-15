package com.realcraftguy.classview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Liste der Mockup-Bildschirme für Phase 2 (nur zum Durchklicken auf dem Gerät). */
@Composable
fun MockupGallery(title: String, names: List<String>, onOpen: (String) -> Unit) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            CenteredColumn(maxWidth = 640.dp) {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Statische Bildschirme zum Anschauen – noch ohne Funktion. Zurück-Geste führt zur Liste.",
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
                )
                Card(padding = PaddingValues(vertical = 4.dp)) {
                    names.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(color = c.outline, modifier = Modifier.padding(start = 20.dp))
                        Text(
                            n,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onOpen(n) }.padding(horizontal = 20.dp, vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
