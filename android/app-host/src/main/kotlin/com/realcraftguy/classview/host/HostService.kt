package com.realcraftguy.classview.host

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.realcraftguy.classview.tsbridge.Tsbridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Zustand, den die Oberfläche anzeigt. Wird vom [HostService] aktualisiert. */
data class HostStatus(
    val serviceRunning: Boolean = false,
    val startedAt: Long? = null,
    val tailscale: TailscaleState = TailscaleState(),
    /** null = noch nicht geprüft */
    val reachable: Boolean? = null,
    val classes: Int = 0,
    val devices: Int = 0,
)

@Serializable
data class TailscaleState(val state: String = "stopped", val domain: String? = null, val error: String? = null)

object HostRuntime {
    private val mutable = MutableStateFlow(HostStatus())
    val status: StateFlow<HostStatus> = mutable
    internal fun update(block: (HostStatus) -> HostStatus) = mutable.update(block)
}

/**
 * Foreground Service (Typ specialUse): hält den Ktor-Server, die Tailscale-Brücke sowie Wake- und
 * WLAN-Lock, solange der Klassenserver laufen soll.
 */
class HostService : Service() {
    private var scope: CoroutineScope? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val json = Json { ignoreUnknownKeys = true }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (scope == null) begin()
        return START_STICKY
    }

    private fun begin() {
        acquireLocks()
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        HostRuntime.update { it.copy(serviceRunning = true, startedAt = System.currentTimeMillis(), reachable = null) }
        s.launch {
            val server = HostGraph.server(this@HostService)
            val prefs = HostGraph.prefs(this@HostService)
            var p = prefs.current()
            try {
                withContext(HostGraph.control) {
                    if (p.inviteKey == null || !server.service.hasInviteKey()) {
                        prefs.setInviteKey(server.service.rotateInviteKey())
                    }
                    server.start()
                    Tsbridge.start(
                        HostGraph.tailscaleDir(this@HostService).absolutePath,
                        p.pendingAuthKey.orEmpty(),
                        HostGraph.TAILSCALE_HOSTNAME,
                        HostGraph.SERVER_PORT.toLong(),
                    )
                }
            } catch (e: Exception) {
                HostRuntime.update { it.copy(tailscale = TailscaleState("error", error = e.message ?: e.toString())) }
            }
            var lastCheck = 0L
            while (isActive) {
                val ts = runCatching { json.decodeFromString<TailscaleState>(Tsbridge.status()) }.getOrDefault(TailscaleState("error"))
                val (classes, devices) = runCatching { server.service.stats() }.getOrDefault(0 to 0)
                HostRuntime.update { it.copy(tailscale = if (it.tailscale.state == "error" && ts.state == "stopped") it.tailscale else ts, classes = classes, devices = devices) }
                if (ts.state == "running") {
                    if (!p.tailscaleConfigured) {
                        prefs.markConfigured()
                        p = prefs.current()
                    }
                    val now = System.currentTimeMillis()
                    if (ts.domain != null && now - lastCheck > 60_000) {
                        lastCheck = now
                        val ok = checkReachable(ts.domain)
                        HostRuntime.update { it.copy(reachable = ok) }
                        updateNotification(ts.domain, ok)
                    }
                }
                delay(2_000)
            }
        }
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        val server = HostGraph.server(this)
        HostGraph.controlScope.launch {
            runCatching { Tsbridge.stop() }
            runCatching { server.stop() }
        }
        wakeLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()
        HostRuntime.update { HostStatus() }
        super.onDestroy()
    }

    /** Ruft die eigene öffentliche Adresse auf – geht der Weg über Funnel, ist der Server von außen erreichbar. */
    private fun checkReachable(domain: String): Boolean = runCatching {
        val conn = URL("https://$domain/api/info").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try { conn.responseCode == 200 } finally { conn.disconnect() }
    }.getOrDefault(false)

    private fun acquireLocks() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "classview:host").apply { setReferenceCounted(false); acquire() }
        val wm = applicationContext.getSystemService(WifiManager::class.java)
        @Suppress("DEPRECATION")
        val mode = if (Build.VERSION.SDK_INT >= 34) WifiManager.WIFI_MODE_FULL_LOW_LATENCY else WifiManager.WIFI_MODE_FULL_HIGH_PERF
        wifiLock = wm.createWifiLock(mode, "classview:host").apply { setReferenceCounted(false); acquire() }
    }

    private fun notification(text: String = "Wird gestartet …"): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Klassenserver", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, HostService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_server)
            .setContentTitle("Klassenserver läuft")
            .setContentText(text)
            .setColor(0xFF2B6BE4.toInt())
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Stoppen", stop)
            .build()
    }

    private fun updateNotification(domain: String, reachable: Boolean) {
        val text = if (reachable) domain else "$domain · von außen nicht erreichbar"
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    companion object {
        private const val CHANNEL = "server"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "stop"

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, HostService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, HostService::class.java))

        fun isCharging(context: Context): Boolean {
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }
    }
}
