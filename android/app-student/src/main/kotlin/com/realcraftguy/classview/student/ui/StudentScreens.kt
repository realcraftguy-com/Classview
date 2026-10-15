package com.realcraftguy.classview.student.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.realcraftguy.classview.student.ConnectionState
import com.realcraftguy.classview.student.data.Membership
import com.realcraftguy.classview.student.data.Profile
import com.realcraftguy.classview.ui.Avatar
import com.realcraftguy.classview.ui.AvatarPreset
import com.realcraftguy.classview.ui.Card
import com.realcraftguy.classview.ui.CenteredColumn
import com.realcraftguy.classview.ui.ClassviewTheme
import com.realcraftguy.classview.ui.CodeTextStyle
import com.realcraftguy.classview.ui.PrimaryButton
import com.realcraftguy.classview.ui.ScreenGlyph
import com.realcraftguy.classview.ui.ScreenSurface
import com.realcraftguy.classview.ui.SectionLabel
import com.realcraftguy.classview.ui.StatusPill

// ---------------------------------------------------------------- Gerüst

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun Notice(text: String, onDismiss: () -> Unit) {
    val c = ClassviewTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.accentSoft).padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Info, null, tint = c.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("OK") }
    }
}

private fun Profile.presetEnum() = AvatarPreset.fromKey(preset)

// ---------------------------------------------------------------- Start (nicht beigetreten)

@Composable
fun StartScreen(
    profile: Profile,
    photo: ImageBitmap?,
    notice: String?,
    onJoin: () -> Unit,
    onEditProfile: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    val c = ClassviewTheme.colors
    val hasName = profile.name.isNotBlank()
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("ClassroomView")
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 420.dp)) {
                    if (notice != null) {
                        Notice(notice, onDismissNotice)
                        Spacer(Modifier.height(28.dp))
                    }
                    Avatar(profile.presetEnum(), profile.name.ifBlank { "?" }, 128.dp, image = photo)
                    Spacer(Modifier.height(16.dp))
                    Text(if (hasName) profile.name else "Willkommen!", style = MaterialTheme.typography.headlineMedium)
                    if (hasName) TextButton(onClick = onEditProfile) { Text("Mein Profil bearbeiten") }
                    Spacer(Modifier.height(36.dp))
                    if (hasName) {
                        PrimaryButton("Klasse beitreten", Modifier.fillMaxWidth(), onClick = onJoin)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Du brauchst den 6-stelligen Code deiner Lehrkraft.",
                            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center,
                        )
                    } else {
                        PrimaryButton("Profil anlegen", Modifier.fillMaxWidth(), onClick = onEditProfile)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Wähle einen Namen und ein Bild. So sieht dich deine Lehrkraft.",
                            style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Profil

@Composable
fun ProfileScreen(
    profile: Profile,
    photo: ImageBitmap?,
    saving: Boolean,
    onSave: (name: String, preset: String?) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onBack: () -> Unit,
) {
    val c = ClassviewTheme.colors
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var preset by rememberSaveable { mutableStateOf(profile.preset) }
    val valid = name.isNotBlank() && name.trim().length <= 40
    ScreenSurface {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Mein Profil", onBack = onBack) {
                TextButton(onClick = { onSave(name, preset) }, enabled = valid && !saving) { Text("Fertig") }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                CenteredColumn {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(8.dp))
                        Avatar(AvatarPreset.fromKey(preset), name.ifBlank { "?" }, 112.dp, image = photo)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = onPickPhoto) { Text(if (photo == null) "Foto aus der Galerie wählen" else "Anderes Foto") }
                            if (photo != null) TextButton(onClick = onRemovePhoto) { Text("Foto entfernen", color = c.danger) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("Anzeigename")
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                        placeholder = { Text("Vorname oder Spitzname") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.surface, focusedContainerColor = c.surface),
                    )
                    Text(
                        "So sieht dich deine Lehrkraft. Tipp: Vorname oder Spitzname reicht.",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    SectionLabel(if (photo == null) "Oder ein Avatar" else "Avatar (wird angezeigt, wenn du das Foto entfernst)")
                    Card(padding = PaddingValues(16.dp)) {
                        AvatarPreset.entries.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                row.forEach { p ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { preset = p.key }.padding(6.dp),
                                    ) {
                                        Avatar(p, name, 64.dp, ring = if (p.key == preset) c.accent else null)
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
                            "Fotos werden auf dem Tablet auf 512 Pixel verkleinert und nur deiner Lehrkraft angezeigt.",
                            style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Speichern", Modifier.fillMaxWidth(), enabled = valid && !saving) { onSave(name, preset) }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Beitreten

@Composable
private fun CodeBoxes(code: String, error: Boolean, focused: Boolean) {
    val c = ClassviewTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 6) {
            if (i == 3) Box(Modifier.size(8.dp).clip(CircleShape).background(c.outline))
            val ch = code.getOrNull(i)?.toString() ?: ""
            val active = focused && i == code.length.coerceAtMost(5) && !error
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
fun JoinScreen(
    code: String,
    hint: String?,
    busy: Boolean,
    error: String?,
    serverHost: String?,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
    onChangeServer: () -> Unit,
    onBack: () -> Unit,
    autoFocus: Boolean = true,
) {
    val c = ClassviewTheme.colors
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(autoFocus, serverHost) { if (autoFocus && serverHost != null) runCatching { focus.requestFocus() } }
    ScreenSurface {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Klasse beitreten", onBack = onBack)
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 480.dp)) {
                    Text("Code eingeben", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Deine Lehrkraft zeigt den Code auf ihrem iPad.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textSecondary, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(32.dp))
                    // Ein unsichtbares Textfeld nimmt die Eingabe; die sechs Kästchen zeigen sie an.
                    BasicTextField(
                        value = TextFieldValue(code, selection = TextRange(code.length)),
                        onValueChange = { onCodeChange(it.text) },
                        enabled = !busy && serverHost != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = { onJoin() }),
                        modifier = Modifier.focusRequester(focus).onFocusChanged { focused = it.isFocused },
                        decorationBox = { CodeBoxes(code, error != null, focused) },
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.height(56.dp), contentAlignment = Alignment.Center) {
                        val msg = error ?: hint
                        if (msg != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (error != null) Icons.Filled.Warning else Icons.Filled.Info, null,
                                    tint = if (error != null) c.danger else c.textSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = if (error != null) c.danger else c.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (busy) {
                        Row(
                            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Verbinde mit der Klasse …", color = c.accent, style = MaterialTheme.typography.labelLarge)
                        }
                    } else if (serverHost == null) {
                        PrimaryButton("Server-Adresse eintragen", Modifier.fillMaxWidth(), onClick = onChangeServer)
                    } else {
                        PrimaryButton("Beitreten", Modifier.fillMaxWidth(), enabled = code.length == 6, onClick = onJoin)
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (serverHost != null) "Server: $serverHost" else "Noch kein Server eingetragen",
                            style = MaterialTheme.typography.bodySmall, color = c.textSecondary,
                        )
                        TextButton(onClick = onChangeServer) { Text("Ändern") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Server-Adresse

@Composable
fun ServerScreen(current: String, busy: Boolean, error: String?, onSave: (String) -> Unit, onBack: () -> Unit) {
    val c = ClassviewTheme.colors
    var input by rememberSaveable { mutableStateOf(current.removePrefix("https://")) }
    ScreenSurface {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Server-Adresse", onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                CenteredColumn {
                    Text(
                        "Die Adresse steht auf dem iPad deiner Lehrkraft unter dem Code, z. B. classview.tail1234.ts.net.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = input, onValueChange = { input = it.trim() }, singleLine = true,
                        placeholder = { Text("classview.tail1234.ts.net") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSave(input) }),
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = c.surface, focusedContainerColor = c.surface),
                    )
                    if (error != null) {
                        Text(error, color = c.danger, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    if (busy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Prüfe Server …", color = c.textSecondary)
                        }
                    } else {
                        PrimaryButton("Prüfen und speichern", Modifier.fillMaxWidth(), enabled = input.isNotBlank()) { onSave(input) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Beigetreten

@Composable
private fun ClassCard(membership: Membership, profile: Profile, photo: ImageBitmap?, onEditProfile: () -> Unit, modifier: Modifier = Modifier) {
    val c = ClassviewTheme.colors
    Card(modifier) {
        SectionLabel("Deine Klasse")
        Text(membership.className, style = MaterialTheme.typography.headlineSmall)
        if (membership.teacherName != null) {
            Spacer(Modifier.height(4.dp))
            Text(membership.teacherName, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = c.outline)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(AvatarPreset.fromKey(profile.preset), profile.name, 52.dp, image = photo)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                Text("So sieht dich deine Lehrkraft", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            TextButton(onClick = onEditProfile) { Text("Ändern") }
        }
    }
}

@Composable
private fun ShareCard(modifier: Modifier = Modifier) {
    val c = ClassviewTheme.colors
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
}

@Composable
fun JoinedScreen(
    membership: Membership,
    profile: Profile,
    photo: ImageBitmap?,
    connection: ConnectionState,
    offlineMessage: String?,
    onEditProfile: () -> Unit,
    onLeave: () -> Unit,
    onHints: () -> Unit,
) {
    val c = ClassviewTheme.colors
    var confirmLeave by remember { mutableStateOf(false) }
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("ClassroomView") {
                when (connection) {
                    ConnectionState.Connected -> StatusPill("Verbunden", c.online, emphasized = true)
                    ConnectionState.Offline -> StatusPill("Keine Verbindung", c.offline)
                    ConnectionState.Unknown -> StatusPill("Verbinde …", c.offline)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
                val wide = maxWidth > 900.dp
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (connection == ConnectionState.Offline && offlineMessage != null) {
                        Row(
                            Modifier.widthIn(max = if (wide) 1100.dp else 600.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceMuted).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("$offlineMessage Es wird automatisch erneut versucht.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    if (wide) {
                        Row(Modifier.widthIn(max = 1100.dp).fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            ClassCard(membership, profile, photo, onEditProfile, Modifier.weight(1f))
                            ShareCard(Modifier.weight(1f))
                        }
                    } else {
                        Column(Modifier.widthIn(max = 600.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ClassCard(membership, profile, photo, onEditProfile, Modifier.fillMaxWidth())
                            ShareCard(Modifier.fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    TextButton(onClick = onHints) { Text("Einstellungen für HyperOS") }
                    TextButton(onClick = { confirmLeave = true }, modifier = Modifier.padding(bottom = 16.dp)) {
                        Text("Klasse verlassen", color = c.danger)
                    }
                }
            }
        }
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Klasse verlassen?") },
            text = { Text("Du verschwindest aus der Liste deiner Lehrkraft. Zum Zurückkommen brauchst du wieder den Code.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; onLeave() }) { Text("Verlassen", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Abbrechen") } },
        )
    }
}

// ---------------------------------------------------------------- HyperOS-Hinweise

data class HintStep(
    val title: String,
    val text: String,
    val done: Boolean,
    val action: String,
    val onAction: () -> Unit,
    /** Nicht automatisch prüfbar: Nutzer bestätigt selbst. */
    val onToggleDone: ((Boolean) -> Unit)? = null,
)

@Composable
fun HintsScreen(steps: List<HintStep>, onDone: () -> Unit, onBack: () -> Unit) {
    val c = ClassviewTheme.colors
    ScreenSurface {
        Column(Modifier.fillMaxSize()) {
            TopBar("Einrichtung", onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                CenteredColumn(maxWidth = 640.dp) {
                    Text("Damit Anfragen ankommen", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "HyperOS beendet Apps im Hintergrund sehr schnell. Stell diese Dinge einmal ein – " +
                            "die Knöpfe öffnen die passende Einstellung. Die Menünamen können je nach HyperOS-Version leicht abweichen.",
                        style = MaterialTheme.typography.bodyLarge, color = c.textSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Card(padding = PaddingValues(vertical = 6.dp)) {
                        steps.forEachIndexed { i, s ->
                            if (i > 0) HorizontalDivider(color = c.outline, modifier = Modifier.padding(start = 72.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(if (s.done) c.online else c.surfaceMuted)
                                        .then(if (s.onToggleDone != null) Modifier.clickable { s.onToggleDone.invoke(!s.done) } else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (s.done) Icon(Icons.Filled.Check, "Erledigt", tint = Color.White, modifier = Modifier.size(20.dp))
                                    else Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                                }
                                Spacer(Modifier.width(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                                    Text(s.text, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                    if (s.onToggleDone != null && !s.done) {
                                        Text("Tippe auf den Kreis, wenn erledigt.", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                if (s.done) Text("Erledigt", color = c.online, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                else TextButton(onClick = s.onAction) { Text(s.action) }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Fertig", Modifier.fillMaxWidth(), onClick = onDone)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
