package com.realcraftguy.classview.host.ui

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.realcraftguy.classview.host.HostStatus
import com.realcraftguy.classview.ui.Card
import com.realcraftguy.classview.ui.ClassviewTheme
import com.realcraftguy.classview.ui.PrimaryButton
import com.realcraftguy.classview.ui.ScreenSurface
import com.realcraftguy.classview.ui.SecondaryButton
import com.realcraftguy.classview.ui.SectionLabel

@Composable
private fun HostTopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun CheckRow(label: String, value: String, ok: Boolean?, onClick: (() -> Unit)? = null) {
    val c = ClassviewTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val bg = when (ok) { true -> c.online.copy(alpha = 0.15f); false -> c.dangerSoft; null -> c.surfaceMuted }
        Box(Modifier.size(28.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            when (ok) {
                true -> Icon(Icons.Filled.Check, null, tint = c.online, modifier = Modifier.size(16.dp))
                false -> Icon(Icons.Filled.Warning, null, tint = c.danger, modifier = Modifier.size(16.dp))
                null -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = c.textSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (onClick != null && ok == false) {
            TextButton(onClick = onClick) { Text("Beheben") }
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}

// ---------------------------------------------------------------- Einrichtung

@Composable
fun SetupScreen(
    authKey: String,
    onAuthKeyChange: (String) -> Unit,
    onConnect: () -> Unit,
    error: String? = null,
) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Einrichtung")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
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
                    value = authKey, onValueChange = onAuthKeyChange, singleLine = true,
                    placeholder = { Text("tskey-auth-…") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.surface, focusedContainerColor = c.surface),
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = c.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                Card(padding = PaddingValues(18.dp)) {
                    Text("So bekommst du den Key", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Im Browser login.tailscale.com öffnen und anmelden.",
                        "Unter „DNS“ MagicDNS und „HTTPS Certificates“ einschalten.",
                        "Unter „Access controls“ Funnel erlauben (siehe Anleitung in docs/setup.md).",
                        "Unter „Settings → Keys“ auf „Generate auth key“ tippen und den Key hier einfügen.",
                    ).forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = c.accent, modifier = Modifier.width(22.dp))
                            Text(t, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Verbinden", Modifier.fillMaxWidth(), enabled = authKey.trim().startsWith("tskey-"), onClick = onConnect)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Hauptbildschirm

@Composable
fun MainScreen(
    status: HostStatus,
    charging: Boolean,
    batteryUnrestricted: Boolean,
    onToggle: (Boolean) -> Unit,
    onInvite: () -> Unit,
    onBatteryHints: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onLogs: () -> Unit,
    onResetTailscale: () -> Unit,
    uptime: String? = null,
) {
    val c = ClassviewTheme.colors
    val ts = status.tailscale
    val running = status.serviceRunning && ts.state == "running"
    val (title, subtitle) = when {
        !status.serviceRunning -> "Server gestoppt" to "iPad und Tablet können sich nicht verbinden"
        ts.state == "error" -> "Problem" to (ts.error ?: "Unbekannter Fehler")
        ts.state != "running" -> "Wird gestartet …" to "Verbindung mit Tailscale wird aufgebaut"
        else -> "Server läuft" to (uptime?.let { "Seit $it" } ?: "")
    }
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("ClassroomView Host") { TextButton(onClick = onLogs) { Text("Protokoll") } }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val cardBg = when {
                    running -> c.accent
                    status.serviceRunning && ts.state == "error" -> c.dangerSoft
                    else -> c.surface
                }
                val fg = if (running) c.onAccent else c.text
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cardBg).padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.headlineSmall, color = fg)
                            if (subtitle.isNotEmpty()) {
                                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = status.serviceRunning, onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = c.accent, checkedTrackColor = Color.White, checkedBorderColor = Color.White),
                        )
                    }
                    if (running && ts.domain != null) {
                        Spacer(Modifier.height(18.dp))
                        Text("ADRESSE", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.7f))
                        SelectionContainer {
                            Text(ts.domain, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace), color = fg)
                        }
                    }
                }

                Card(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                    val reach = if (!running) false else status.reachable
                    CheckRow("Von außen erreichbar", when (reach) { true -> "ja"; false -> "nein"; null -> "wird geprüft" },
                        if (status.serviceRunning) reach else false)
                    HorizontalDivider(color = c.outline)
                    CheckRow("Tailscale", when (ts.state) { "running" -> "verbunden"; "starting" -> "verbindet"; "error" -> "Fehler"; else -> "aus" },
                        when (ts.state) { "running" -> true; "starting" -> null; else -> false })
                    HorizontalDivider(color = c.outline)
                    CheckRow("Ladegerät", if (charging) "angeschlossen" else "nicht angeschlossen", charging)
                    HorizontalDivider(color = c.outline)
                    CheckRow("Akku-Optimierung", if (batteryUnrestricted) "aus" else "an", batteryUnrestricted, onRequestBatteryExemption)
                }

                Card(Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    SectionLabel("Auf diesem Server")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${status.classes}", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(if (status.classes == 1) "Klasse" else "Klassen", style = MaterialTheme.typography.bodyLarge)
                            Text("${status.devices} Schülergeräte beigetreten", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }

                PrimaryButton("Einladungslink für Lehrkraft", Modifier.fillMaxWidth(), enabled = running, onClick = onInvite)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = onBatteryHints) { Text("Hinweise für Samsung-Akku") }
                    TextButton(onClick = onResetTailscale) { Text("Tailscale neu einrichten", color = c.textSecondary) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Einladung

@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0))
    }
    Canvas(modifier) {
        val cell = size.width / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix.get(x, y)) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}

@Composable
fun InviteScreen(link: String?, onShare: () -> Unit, onRotate: () -> Unit, onBack: () -> Unit) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Einladung", onBack = onBack)
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
                if (link == null) {
                    Text("Der Server läuft noch nicht.", color = c.textSecondary)
                } else {
                    Box(Modifier.clip(RoundedCornerShape(24.dp)).background(Color.White).padding(20.dp)) {
                        QrCode(link, Modifier.size(220.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    SelectionContainer {
                        Text(
                            link, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            color = c.textSecondary, textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Link teilen", Modifier.fillMaxWidth(), onClick = onShare)
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Neuen Link erzeugen", Modifier.fillMaxWidth(), onClick = onRotate)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Wer diesen Link hat, kann auf deinem Server Klassen anlegen. Ein neuer Link macht den alten " +
                                "ungültig; bestehende Klassen bleiben erhalten.",
                            style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Hinweise und Protokoll

@Composable
fun BatteryHintsScreen(onOpenAppSettings: () -> Unit, onBack: () -> Unit) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Samsung-Akku", onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text(
                    "One UI beendet Apps im Hintergrund gern. Damit der Server den ganzen Schultag läuft:",
                    style = MaterialTheme.typography.bodyLarge, color = c.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth(), padding = PaddingValues(18.dp)) {
                    listOf(
                        "App-Einstellungen → Akku → „Nicht eingeschränkt“.",
                        "Einstellungen → Akku → Hintergrundnutzungslimits → „Nie schlafende Apps“ → ClassroomView Host hinzufügen.",
                        "Dort „Nicht verwendete Apps in Standby versetzen“ ausschalten.",
                        "Handy dauerhaft ans Ladegerät. Tipp: Akku → „Akku schützen“ (bis 80 % laden) schont den Akku.",
                        "Die Menünamen können je nach One-UI-Version leicht abweichen.",
                    ).forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Text("${i + 1}.", color = c.accent, modifier = Modifier.width(24.dp))
                            Text(t, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("App-Einstellungen öffnen", Modifier.fillMaxWidth(), onClick = onOpenAppSettings)
            }
        }
    }
}

@Composable
fun LogScreen(text: String, onRefresh: () -> Unit, onBack: () -> Unit) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            HostTopBar("Protokoll", onBack = onBack) { TextButton(onClick = onRefresh) { Text("Aktualisieren") } }
            SelectionContainer {
                Text(
                    text.ifBlank { "Noch keine Einträge." },
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = c.text,
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState(Int.MAX_VALUE)).padding(16.dp),
                )
            }
        }
    }
}
