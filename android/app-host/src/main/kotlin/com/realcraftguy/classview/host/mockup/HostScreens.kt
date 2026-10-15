package com.realcraftguy.classview.host.mockup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.realcraftguy.classview.ui.Card
import com.realcraftguy.classview.ui.ClassviewTheme
import com.realcraftguy.classview.ui.PrimaryButton
import com.realcraftguy.classview.ui.ScreenSurface
import com.realcraftguy.classview.ui.SecondaryButton
import com.realcraftguy.classview.ui.SectionLabel
import com.realcraftguy.classview.ui.StatusDot
import kotlin.random.Random

// Mockups der Host-App (Galaxy S21 FE, ≈ 411 × 891 dp). Statisch, ohne Logik.

object HostSample {
    const val ADDRESS = "classview.tail4b2c1.ts.net"
}

@Composable
private fun HostTopBar(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CheckRow(label: String, value: String, ok: Boolean) {
    val c = ClassviewTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (ok) c.online.copy(alpha = 0.15f) else c.dangerSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ok) Icons.Filled.Check else Icons.Filled.Warning, null, tint = if (ok) c.online else c.danger, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
}

// ---------------------------------------------------------------- Einrichtung

@Composable
fun HostSetupScreen() {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Einrichtung")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("SCHRITT 1 VON 2", style = MaterialTheme.typography.labelMedium, color = c.accent)
                Spacer(Modifier.height(6.dp))
                Text("Mit Tailscale verbinden", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Damit iPad und Tablet in der Schule diesen Server erreichen, ohne dass am Router etwas " +
                        "eingestellt werden muss.",
                    style = MaterialTheme.typography.bodyLarge, color = c.textSecondary,
                )
                Spacer(Modifier.height(24.dp))
                SectionLabel("Auth-Key")
                OutlinedTextField(
                    value = "", onValueChange = {}, singleLine = true,
                    placeholder = { Text("tskey-auth-…") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.surface, focusedContainerColor = c.surface),
                )
                Spacer(Modifier.height(20.dp))
                Card(padding = PaddingValues(18.dp)) {
                    Text("So bekommst du den Key", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Im Browser login.tailscale.com öffnen und anmelden.",
                        "Unter „DNS“ MagicDNS und HTTPS-Zertifikate einschalten.",
                        "Funnel erlauben (die Seite bietet dafür einen Knopf an).",
                        "Unter „Settings → Keys“ einen Auth-Key erzeugen und hier einfügen.",
                    ).forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = c.accent, modifier = Modifier.width(22.dp))
                            Text(t, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Verbinden", Modifier.fillMaxWidth(), enabled = false)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Hauptbildschirm

@Composable
fun HostMainScreen(running: Boolean = true) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("ClassroomView Host")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Statuskarte
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(if (running) c.accent else c.surface)
                        .padding(22.dp),
                ) {
                    val fg = if (running) c.onAccent else c.text
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (running) "Server läuft" else "Server gestoppt", style = MaterialTheme.typography.headlineSmall, color = fg)
                            Text(
                                if (running) "Seit 2 Std. 14 Min." else "iPad und Tablet können sich nicht verbinden",
                                style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f),
                            )
                        }
                        Switch(
                            checked = running, onCheckedChange = {},
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = c.accent, checkedTrackColor = Color.White,
                                checkedBorderColor = Color.White,
                            ),
                        )
                    }
                    if (running) {
                        Spacer(Modifier.height(18.dp))
                        Text("ADRESSE", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
                        Text(HostSample.ADDRESS, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace), color = fg)
                    }
                }

                Card(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                    CheckRow("Von außen erreichbar", if (running) "ja" else "nein", running)
                    HorizontalDivider(color = c.outline)
                    CheckRow("Tailscale", "angemeldet", true)
                    HorizontalDivider(color = c.outline)
                    CheckRow("Ladegerät", "angeschlossen", true)
                    HorizontalDivider(color = c.outline)
                    CheckRow("Akku-Optimierung", "aus", true)
                }

                Card(Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    SectionLabel("Gerade verbunden")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (running) "3" else "0", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(if (running) "1 Lehrkraft · 2 Schüler" else "–", style = MaterialTheme.typography.bodyLarge)
                            Text("in 1 Klasse", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }

                PrimaryButton("Einladungslink für Lehrkraft", Modifier.fillMaxWidth(), enabled = running)
                TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Hinweise für Samsung-Akku") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Einladung

/** Platzhalter für den QR-Code (der echte wird in Phase 3 mit ZXing erzeugt). */
@Composable
private fun QrPlaceholder() {
    val c = ClassviewTheme.colors
    val rnd = Random(7)
    Canvas(Modifier.size(220.dp)) {
        val n = 25
        val cell = size.width / n
        fun finder(x: Int, y: Int) {
            drawRect(c.text, Offset(x * cell, y * cell), Size(7 * cell, 7 * cell))
            drawRect(Color.White, Offset((x + 1) * cell, (y + 1) * cell), Size(5 * cell, 5 * cell))
            drawRect(c.text, Offset((x + 2) * cell, (y + 2) * cell), Size(3 * cell, 3 * cell))
        }
        for (y in 0 until n) for (x in 0 until n) {
            val inFinder = (x < 8 && y < 8) || (x >= n - 8 && y < 8) || (x < 8 && y >= n - 8)
            if (!inFinder && rnd.nextFloat() < 0.45f) drawRect(c.text, Offset(x * cell, y * cell), Size(cell, cell))
        }
        finder(0, 0); finder(n - 7, 0); finder(0, n - 7)
    }
}

@Composable
fun HostInviteScreen() {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Einladung", onBack = {})
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Für deine Lehrkraft", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mit diesem Link kann sie Klassen anlegen. Sie öffnet ihn in Safari und legt ihn auf den Home-Bildschirm.",
                    style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Box(Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) { QrPlaceholder() }
                Spacer(Modifier.height(16.dp))
                Text(
                    "https://${HostSample.ADDRESS}/#/invite/…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Link teilen", Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Neuen Link erzeugen", Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ein neuer Link macht den alten ungültig. Bestehende Klassen bleiben erhalten.",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Previews

private const val PHONE = "spec:width=411dp,height=891dp,dpi=420"

@Preview(name = "Einrichtung", device = PHONE)
@Composable private fun PSetup() = ClassviewTheme(false) { HostSetupScreen() }

@Preview(name = "Server läuft", device = PHONE)
@Composable private fun PMain() = ClassviewTheme(false) { HostMainScreen() }

@Preview(name = "Server gestoppt – dunkel", device = PHONE)
@Composable private fun PMainStopped() = ClassviewTheme(true) { HostMainScreen(running = false) }

@Preview(name = "Einladung", device = PHONE)
@Composable private fun PInvite() = ClassviewTheme(false) { HostInviteScreen() }
