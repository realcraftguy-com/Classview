package com.realcraftguy.classview.student.mockup

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.realcraftguy.classview.ui.Avatar
import com.realcraftguy.classview.ui.AvatarPreset
import com.realcraftguy.classview.ui.Card
import com.realcraftguy.classview.ui.CenteredColumn
import com.realcraftguy.classview.ui.ClassGlyph
import com.realcraftguy.classview.ui.ClassviewTheme
import com.realcraftguy.classview.ui.CodeTextStyle
import com.realcraftguy.classview.ui.PrimaryButton
import com.realcraftguy.classview.ui.ScreenGlyph
import com.realcraftguy.classview.ui.ScreenSurface
import com.realcraftguy.classview.ui.SecondaryButton
import com.realcraftguy.classview.ui.SectionLabel
import com.realcraftguy.classview.ui.StatusDot
import com.realcraftguy.classview.ui.StatusPill

// Mockups für Phase 2: statische Bildschirme ohne Logik. Beispieldaten stehen hier zentral,
// damit Previews, Screenshot-Tests und die Mockup-Galerie dieselben Inhalte zeigen.

object Sample {
    const val NAME = "Felix"
    val PRESET = AvatarPreset.Welle
    const val CLASS_NAME = "7b · Informatik"
    const val TEACHER = "Frau Berger"
}

// ---------------------------------------------------------------- Gerüst

@Composable
private fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
        Spacer(Modifier.width(8.dp))
    }
}

// ---------------------------------------------------------------- Start (nicht beigetreten)

@Composable
fun StartScreen(name: String = Sample.NAME, preset: AvatarPreset? = Sample.PRESET) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("ClassroomView")
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(max = 420.dp),
                ) {
                    Avatar(preset, name, 128.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(name, style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = {}) { Text("Mein Profil bearbeiten") }
                    Spacer(Modifier.height(40.dp))
                    PrimaryButton("Klasse beitreten", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Du brauchst den 6-stelligen Code deiner Lehrkraft.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Profil

@Composable
fun ProfileScreen(name: String = Sample.NAME, selected: AvatarPreset? = Sample.PRESET) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("Mein Profil", onBack = {}) { TextButton(onClick = {}) { Text("Fertig") } }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                CenteredColumn {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(8.dp))
                        Avatar(selected, name, 112.dp)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {}) { Text("Foto aus der Galerie wählen") }
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Anzeigename")
                    OutlinedTextField(
                        value = name, onValueChange = {}, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.surface, focusedContainerColor = c.surface),
                    )
                    Text(
                        "So sieht dich deine Lehrkraft. Tipp: Vorname oder Spitzname reicht.",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    SectionLabel("Oder ein Avatar")
                    Card(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                        AvatarPreset.entries.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { p ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Avatar(p, name, 64.dp, ring = if (p == selected) c.accent else null)
                                        Spacer(Modifier.height(6.dp))
                                        Text(p.label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Filled.Info, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Fotos werden auf dem Tablet verkleinert und nur deiner Lehrkraft angezeigt.",
                            style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Beitreten

sealed interface JoinState {
    data class Typing(val code: String) : JoinState
    data class Loading(val code: String) : JoinState
    data class Error(val code: String, val message: String) : JoinState
}

@Composable
private fun CodeBoxes(code: String, error: Boolean) {
    val c = ClassviewTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 6) {
            if (i == 3) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.outline))
            }
            val ch = code.getOrNull(i)?.toString() ?: ""
            val active = i == code.length && !error
            Box(
                Modifier
                    .size(width = 56.dp, height = 68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(
                        width = if (active || error) 2.dp else 1.dp,
                        color = when { error -> c.danger; active -> c.accent; else -> c.outline },
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(ch, style = CodeTextStyle.copy(fontSize = MaterialTheme.typography.headlineMedium.fontSize, letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing))
            }
        }
    }
}

@Composable
fun JoinScreen(state: JoinState = JoinState.Typing("K7P4")) {
    val c = ClassviewTheme.colors
    val code = when (state) {
        is JoinState.Typing -> state.code
        is JoinState.Loading -> state.code
        is JoinState.Error -> state.code
    }
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("Klasse beitreten", onBack = {})
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 480.dp)) {
                    Text("Code eingeben", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Deine Lehrkraft zeigt den Code auf ihrem iPad.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    CodeBoxes(code, error = state is JoinState.Error)
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                        if (state is JoinState.Error) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = c.danger, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(state.message, color = c.danger, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (state) {
                        is JoinState.Loading -> {
                            Row(
                                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Verbinde mit der Klasse …", color = c.accent, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        else -> PrimaryButton("Beitreten", Modifier.fillMaxWidth(), enabled = code.length == 6 && state !is JoinState.Error)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Beigetreten

enum class Connection { Connected, Reconnecting }

@Composable
private fun ClassCard(modifier: Modifier = Modifier) {
    val c = ClassviewTheme.colors
    Card(modifier) {
        SectionLabel("Deine Klasse")
        Text(Sample.CLASS_NAME, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(Sample.TEACHER, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = c.outline)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(Sample.PRESET, Sample.NAME, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(Sample.NAME, style = MaterialTheme.typography.titleMedium)
                Text("So sieht dich deine Lehrkraft", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            TextButton(onClick = {}) { Text("Ändern") }
        }
    }
}

@Composable
private fun ShareCard(sharing: Boolean, modifier: Modifier = Modifier) {
    val c = ClassviewTheme.colors
    if (!sharing) {
        Card(modifier) {
            SectionLabel("Bildschirmfreigabe")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
                    ScreenGlyph(c.textSecondary, 28.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Keine Freigabe aktiv", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Möchte deine Lehrkraft deinen Bildschirm sehen, wirst du vorher gefragt.",
                        style = MaterialTheme.typography.bodyMedium, color = c.textSecondary,
                    )
                }
            }
        }
    } else {
        Column(
            modifier
                .clip(RoundedCornerShape(20.dp))
                .background(c.accent)
                .padding(20.dp),
        ) {
            Text("BILDSCHIRMFREIGABE", style = MaterialTheme.typography.labelMedium, color = c.onAccent.copy(alpha = 0.8f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    ScreenGlyph(c.onAccent, 28.dp, sharing = true)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Bildschirm wird geteilt", style = MaterialTheme.typography.titleMedium, color = c.onAccent)
                    Text("${Sample.TEACHER} sieht deinen Bildschirm · seit 2:14", style = MaterialTheme.typography.bodyMedium, color = c.onAccent.copy(alpha = 0.85f))
                }
            }
            Spacer(Modifier.height(18.dp))
            androidx.compose.material3.Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = c.danger),
            ) { Text("Freigabe beenden", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
fun JoinedScreen(sharing: Boolean = false, connection: Connection = Connection.Connected, overlay: @Composable () -> Unit = {}) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TopBar("ClassroomView") {
                    when (connection) {
                        Connection.Connected -> StatusPill("Verbunden", c.online, emphasized = true)
                        Connection.Reconnecting -> StatusPill("Verbindung wird wiederhergestellt …", c.offline)
                    }
                }
                BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
                    val wide = maxWidth > 900.dp
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.weight(0.6f))
                        if (wide) {
                            Row(Modifier.widthIn(max = 1100.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                ClassCard(Modifier.weight(1f))
                                ShareCard(sharing, Modifier.weight(1f))
                            }
                        } else {
                            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ClassCard(Modifier.fillMaxWidth())
                                ShareCard(sharing, Modifier.fillMaxWidth())
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {}, modifier = Modifier.padding(bottom = 16.dp)) {
                            Text("Klasse verlassen", color = c.danger)
                        }
                    }
                }
            }
            overlay()
        }
    }
}

// ---------------------------------------------------------------- Freigabe-Anfrage

@Composable
fun ShareRequestDialog(secondsLeft: Int = 52) {
    val c = ClassviewTheme.colors
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                ScreenGlyph(c.accent, 38.dp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Bildschirm freigeben?", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Text(
                "${Sample.TEACHER} möchte deinen Bildschirm sehen.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            Text(Sample.CLASS_NAME, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceMuted).padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Info, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Nach „Erlauben“ fragt Android noch einmal. Dort kannst du auch nur eine einzelne App teilen.",
                    style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Verweigern", Modifier.weight(1f))
                PrimaryButton("Erlauben", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Anfrage läuft in 0:%02d ab".format(secondsLeft),
                style = MaterialTheme.typography.labelMedium, color = c.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------- HyperOS-Hinweis

private data class Step(val title: String, val text: String, val done: Boolean)

@Composable
fun HyperOsHintScreen() {
    val c = ClassviewTheme.colors
    val steps = listOf(
        Step("Autostart erlauben", "Damit die App nach einem Neustart wieder verbunden ist.", true),
        Step("Akku: Keine Einschränkungen", "Sonst beendet HyperOS die Verbindung im Hintergrund.", true),
        Step("Schwebende Benachrichtigungen", "Damit du Anfragen auch siehst, wenn eine andere App offen ist.", false),
        Step("App in „Zuletzt verwendet“ sperren", "Karte gedrückt halten und auf das Schloss tippen.", false),
        Step("Benachrichtigungssymbole anzeigen", "Damit das Klassen-Symbol oben in der Statusleiste erscheint.", false),
    )
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("Einrichtung", onBack = {})
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                CenteredColumn(maxWidth = 640.dp) {
                    Text("Damit Anfragen ankommen", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "HyperOS beendet Apps im Hintergrund sehr schnell. Stell diese fünf Dinge einmal ein – " +
                            "die Knöpfe öffnen die passende Einstellung.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Card(padding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
                        steps.forEachIndexed { i, s ->
                            if (i > 0) HorizontalDivider(color = c.outline, modifier = Modifier.padding(start = 72.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(if (s.done) c.online else c.surfaceMuted),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (s.done) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    else Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                                }
                                Spacer(Modifier.width(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                                    Text(s.text, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                }
                                Spacer(Modifier.width(12.dp))
                                if (s.done) Text("Erledigt", color = c.online, style = MaterialTheme.typography.labelLarge)
                                else TextButton(onClick = {}) { Text("Öffnen") }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Fertig", Modifier.fillMaxWidth())
                    TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Später erinnern") }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Statusleiste & Benachrichtigungen (Illustration)

@Composable
private fun FakeStatusBar() {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF15161A)).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("9:41", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(12.dp))
        ClassGlyph(Color.White, 18.dp)
        Spacer(Modifier.weight(1f))
        Text("Bluetooth · WLAN · 82 %", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FakeNotification(title: String, text: String, colorized: Boolean, actions: List<String>, sharing: Boolean = false) {
    val c = ClassviewTheme.colors
    val bg = if (colorized) c.accent else c.surface
    val fg = if (colorized) c.onAccent else c.text
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sharing) ScreenGlyph(fg, 18.dp, sharing = true) else ClassGlyph(fg, 18.dp)
            Spacer(Modifier.width(8.dp))
            Text("ClassroomView", style = MaterialTheme.typography.labelMedium, color = fg.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.85f))
        if (actions.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                actions.forEach { Text(it, style = MaterialTheme.typography.labelLarge, color = if (colorized) fg else c.accent) }
            }
        }
    }
}

@Composable
fun NotificationIllustration() {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            FakeStatusBar()
            Column(
                Modifier.fillMaxSize().background(Color(0xFF2A2C31)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Illustration – das genaue Aussehen bestimmt HyperOS", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    FakeNotification("Mit Klasse 7b · Informatik verbunden", "Tippen, um ClassroomView zu öffnen", colorized = true, actions = emptyList())
                    FakeNotification(
                        "${Sample.TEACHER} möchte deinen Bildschirm sehen", "Anfrage läuft in 0:52 ab",
                        colorized = false, actions = listOf("Verweigern", "Öffnen"),
                    )
                    FakeNotification(
                        "Bildschirm wird geteilt", "${Sample.TEACHER} sieht deinen Bildschirm",
                        colorized = true, actions = listOf("Freigabe beenden"), sharing = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(c.accent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Oben links in der Statusleiste erscheint das Klassen-Symbol (weiß, Farbe bestimmt das System).",
                            color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Previews

private const val TABLET_LAND = "spec:width=1333dp,height=890dp,dpi=360"
private const val TABLET_PORT = "spec:width=890dp,height=1333dp,dpi=360"

@Preview(name = "Start", device = TABLET_LAND)
@Composable private fun PStart() = ClassviewTheme(false) { StartScreen() }

@Preview(name = "Start ohne Profil", device = TABLET_PORT)
@Composable private fun PStartEmpty() = ClassviewTheme(false) { StartScreen(name = "Neu", preset = null) }

@Preview(name = "Profil", device = TABLET_PORT)
@Composable private fun PProfile() = ClassviewTheme(false) { ProfileScreen() }

@Preview(name = "Beitreten", device = TABLET_LAND)
@Composable private fun PJoin() = ClassviewTheme(false) { JoinScreen() }

@Preview(name = "Beitreten – Fehler", device = TABLET_LAND)
@Composable private fun PJoinError() = ClassviewTheme(false) { JoinScreen(JoinState.Error("K7P4XN", "Diesen Code gibt es nicht. Prüfe ihn noch einmal.")) }

@Preview(name = "Beigetreten", device = TABLET_LAND)
@Composable private fun PJoined() = ClassviewTheme(false) { JoinedScreen() }

@Preview(name = "Beigetreten – dunkel", device = TABLET_LAND, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun PJoinedDark() = ClassviewTheme(true) { JoinedScreen() }

@Preview(name = "Anfrage", device = TABLET_LAND)
@Composable private fun PRequest() = ClassviewTheme(false) { JoinedScreen(overlay = { ShareRequestDialog() }) }

@Preview(name = "Freigabe aktiv", device = TABLET_PORT)
@Composable private fun PSharing() = ClassviewTheme(false) { JoinedScreen(sharing = true) }

@Preview(name = "HyperOS-Hinweis", device = TABLET_LAND)
@Composable private fun PHint() = ClassviewTheme(false) { HyperOsHintScreen() }

@Preview(name = "Benachrichtigungen", device = TABLET_PORT)
@Composable private fun PNotif() = ClassviewTheme(false) { NotificationIllustration() }
