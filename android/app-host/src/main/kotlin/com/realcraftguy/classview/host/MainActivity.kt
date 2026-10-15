package com.realcraftguy.classview.host

import android.Manifest
import android.content.Intent
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.realcraftguy.classview.host.ui.BatteryHintsScreen
import com.realcraftguy.classview.host.ui.InviteScreen
import com.realcraftguy.classview.host.ui.LogScreen
import com.realcraftguy.classview.host.ui.MainScreen
import com.realcraftguy.classview.host.ui.SetupScreen
import com.realcraftguy.classview.tsbridge.Tsbridge
import com.realcraftguy.classview.ui.ClassviewTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ClassviewTheme { Box(Modifier.fillMaxSize().safeDrawingPadding()) { HostApp() } } }
    }
}

private enum class Screen { Main, Invite, Battery, Logs }

@Composable
private fun HostApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { HostGraph.prefs(context) }
    val prefState by prefs.state.collectAsState(initial = null)
    val status by HostRuntime.status.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.Main) }
    var authKey by rememberSaveable { mutableStateOf("") }
    var charging by remember { mutableStateOf(false) }
    var batteryOk by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    // Ladezustand, Akku-Ausnahme und Laufzeit regelmäßig auffrischen, solange die App sichtbar ist.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                charging = HostService.isCharging(context)
                batteryOk = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
                now = System.currentTimeMillis()
                delay(3_000)
            }
        }
    }

    BackHandler(enabled = screen != Screen.Main) { screen = Screen.Main }
    val p = prefState ?: return

    val needsSetup = !p.tailscaleConfigured && p.pendingAuthKey == null && !status.serviceRunning
    if (needsSetup) {
        SetupScreen(
            authKey = authKey,
            onAuthKeyChange = { authKey = it },
            onConnect = {
                scope.launch {
                    prefs.setPendingAuthKey(authKey.trim())
                    authKey = ""
                    HostService.start(context)
                }
            },
        )
        return
    }

    val domain = status.tailscale.domain
    val inviteLink = if (domain != null && p.inviteKey != null && status.tailscale.state == "running") {
        "https://$domain/#/invite/${p.inviteKey}"
    } else null

    when (screen) {
        Screen.Main -> MainScreen(
            status = status,
            charging = charging,
            batteryUnrestricted = batteryOk,
            uptime = status.startedAt?.let { formatDuration(now - it) },
            onToggle = { on -> if (on) HostService.start(context) else HostService.stop(context) },
            onInvite = { screen = Screen.Invite },
            onBatteryHints = { screen = Screen.Battery },
            onRequestBatteryExemption = {
                @Suppress("BatteryLife")
                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            },
            onLogs = { logs = Tsbridge.logs(); screen = Screen.Logs },
            onResetTailscale = {
                scope.launch {
                    HostService.stop(context)
                    HostGraph.controlScope.launch { HostGraph.tailscaleDir(context).deleteRecursively() }
                    prefs.resetTailscale()
                }
            },
        )
        Screen.Invite -> InviteScreen(
            link = inviteLink,
            onShare = {
                if (inviteLink != null) {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "ClassroomView")
                        .putExtra(Intent.EXTRA_TEXT, "Einladung zu ClassroomView (in Safari öffnen):\n$inviteLink")
                    context.startActivity(Intent.createChooser(send, "Einladungslink teilen"))
                }
            },
            onRotate = {
                scope.launch {
                    val key = HostGraph.server(context).service.rotateInviteKey()
                    prefs.setInviteKey(key)
                }
            },
            onBack = { screen = Screen.Main },
        )
        Screen.Battery -> BatteryHintsScreen(
            onOpenAppSettings = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            },
            onBack = { screen = Screen.Main },
        )
        Screen.Logs -> LogScreen(logs, onRefresh = { logs = Tsbridge.logs() }, onBack = { screen = Screen.Main })
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes < 60) "$minutes Min." else "${minutes / 60} Std. ${minutes % 60} Min."
}
