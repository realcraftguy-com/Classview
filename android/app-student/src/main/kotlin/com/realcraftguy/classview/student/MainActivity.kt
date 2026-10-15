package com.realcraftguy.classview.student

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.realcraftguy.classview.student.data.Profile
import com.realcraftguy.classview.student.ui.HintStep
import com.realcraftguy.classview.student.ui.HintsScreen
import com.realcraftguy.classview.student.ui.JoinScreen
import com.realcraftguy.classview.student.ui.JoinedScreen
import com.realcraftguy.classview.student.ui.ProfileScreen
import com.realcraftguy.classview.student.ui.ServerScreen
import com.realcraftguy.classview.student.ui.StartScreen
import com.realcraftguy.classview.ui.ClassviewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassviewTheme {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) { StudentApp(vm) }
            }
        }
    }
}

@Composable
private fun StudentApp(vm: AppViewModel) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    if (!state.loaded) return

    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { vm.onPause() }
    }

    val photo = rememberPhoto(vm.repo.store.photoFile, state.profile)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.pickPhoto(uri)
    }
    val t = state.t
    BackHandler(enabled = t.screen != null) { vm.back() }

    when (state.screen) {
        Screen.Start -> StartScreen(
            profile = state.profile, photo = photo, notice = t.notice,
            onJoin = { vm.navigate(Screen.Join) },
            onEditProfile = { vm.navigate(Screen.Profile) },
            onDismissNotice = vm::dismissNotice,
        )
        Screen.Profile -> ProfileScreen(
            profile = state.profile, photo = photo, saving = t.profileSaving,
            onSave = vm::saveProfile,
            onPickPhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemovePhoto = vm::removePhoto,
            onBack = vm::back,
        )
        Screen.Join -> JoinScreen(
            code = t.code, hint = t.codeHint, busy = t.joinBusy, error = t.joinError,
            serverHost = state.settings?.serverUrl?.takeIf { it.isNotBlank() }?.let(::hostOf),
            onCodeChange = vm::onCodeChange, onJoin = vm::join,
            onChangeServer = { vm.navigate(Screen.Server) }, onBack = vm::back,
        )
        Screen.Server -> ServerScreen(
            current = state.settings?.serverUrl.orEmpty(), busy = t.serverBusy, error = t.serverError,
            onSave = vm::saveServer, onBack = { vm.navigate(Screen.Join) },
        )
        Screen.Joined -> {
            val m = state.membership
            if (m == null) {
                LaunchedEffect(Unit) { vm.back() }
            } else {
                JoinedScreen(
                    membership = m, profile = state.profile, photo = photo,
                    connection = t.connection, offlineMessage = t.offlineMessage,
                    onEditProfile = { vm.navigate(Screen.Profile) },
                    onLeave = vm::leave,
                    onHints = { vm.navigate(Screen.Hints) },
                )
            }
        }
        Screen.Hints -> HintsScreen(
            steps = hintSteps(context, vm, state.settings?.autostartDone == true, state.settings?.lockDone == true),
            onDone = vm::finishHints, onBack = vm::finishHints,
        )
    }
}

private fun hostOf(url: String) = Uri.parse(url).host ?: url

/** Lädt das eigene Profilfoto neu, sobald es sich ändert. */
@Composable
private fun rememberPhoto(file: File, profile: Profile): ImageBitmap? {
    val photo by produceState<ImageBitmap?>(null, profile.photoVersion, profile.hasPhoto) {
        value = if (!profile.hasPhoto) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
        }
    }
    return photo
}

/** Schritte für HyperOS. Prüfbares wird bei jeder Rückkehr in die App neu geprüft. */
@Composable
private fun hintSteps(context: Context, vm: AppViewModel, autostartDone: Boolean, lockDone: Boolean): List<HintStep> {
    val resumeTick = remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick.intValue++
        onPauseOrDispose { }
    }
    val tick = resumeTick.intValue // bei Rückkehr aus den Einstellungen neu prüfen
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick.intValue++ }
    val batteryOk = remember(tick) { context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName) }
    val notificationsOk = remember(tick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    return listOf(
        HintStep(
            "Autostart erlauben", "Damit die App nach einem Neustart wieder verbunden ist.",
            autostartDone, "Öffnen", { openAutostart(context) }, onToggleDone = { vm.setAutostartDone(it) },
        ),
        HintStep(
            "Akku: Keine Einschränkungen", "Sonst beendet HyperOS die Verbindung im Hintergrund.",
            batteryOk, "Öffnen", { requestBatteryExemption(context) },
        ),
        HintStep(
            "Benachrichtigungen erlauben", "Damit du Freigabe-Anfragen auch siehst, wenn eine andere App offen ist.",
            notificationsOk, "Erlauben",
            {
                if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            },
        ),
        HintStep(
            "App in „Zuletzt verwendet“ sperren", "Übersicht öffnen, ClassroomView gedrückt halten und auf das Schloss tippen.",
            lockDone, "Erledigt", { vm.setLockDone(true) }, onToggleDone = { vm.setLockDone(it) },
        ),
    )
}

private fun openAutostart(context: Context) {
    // Xiaomi-interne Seite ist nicht dokumentiert; wenn sie fehlt, öffnen wir die App-Einstellungen.
    val miui = Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(miui) }.onFailure { runCatching { context.startActivity(fallback) } }
}

private fun requestBatteryExemption(context: Context) {
    @Suppress("BatteryLife")
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(request) }
        .onFailure { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
}
