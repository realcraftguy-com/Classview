package com.realcraftguy.classview.host

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.realcraftguy.classview.server.ClassviewServer
import com.realcraftguy.classview.server.ServerConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.concurrent.Executors

private val Context.hostPrefs by preferencesDataStore(name = "host")

/** Einstellungen der Host-App. Liegen im App-privaten Speicher; Backups sind abgeschaltet. */
class HostPrefs(private val context: Context) {
    private val authKey = stringPreferencesKey("tailscale_auth_key")
    private val configured = booleanPreferencesKey("tailscale_configured")
    private val inviteKey = stringPreferencesKey("invite_key")

    val state = context.hostPrefs.data.map { p ->
        Prefs(
            pendingAuthKey = p[authKey],
            tailscaleConfigured = p[configured] ?: false,
            inviteKey = p[inviteKey],
        )
    }

    suspend fun current(): Prefs = state.first()

    suspend fun setPendingAuthKey(key: String?) = context.hostPrefs.edit {
        if (key == null) it.remove(authKey) else it[authKey] = key
    }

    /** Nach erfolgreicher Anmeldung: Auth-Key wird nicht mehr gebraucht und gelöscht. */
    suspend fun markConfigured() = context.hostPrefs.edit {
        it[configured] = true
        it.remove(authKey)
    }

    suspend fun resetTailscale() = context.hostPrefs.edit {
        it[configured] = false
        it.remove(authKey)
    }

    suspend fun setInviteKey(key: String) = context.hostPrefs.edit { it[inviteKey] = key }

    data class Prefs(val pendingAuthKey: String?, val tailscaleConfigured: Boolean, val inviteKey: String?)
}

/** Einfache Abhängigkeitsverwaltung: eine Server-Instanz pro Prozess. */
object HostGraph {
    /**
     * Start und Stopp von Server und Tailscale laufen nacheinander auf einem eigenen Thread, damit ein
     * schnelles Aus- und Wiedereinschalten sich nicht überholt.
     */
    val control = Executors.newSingleThreadExecutor { Thread(it, "classview-control") }.asCoroutineDispatcher()
    val controlScope = CoroutineScope(SupervisorJob() + control)

    const val SERVER_PORT = 8080
    const val TAILSCALE_HOSTNAME = "classview"

    @Volatile private var serverInstance: ClassviewServer? = null
    @Volatile private var prefsInstance: HostPrefs? = null

    fun server(context: Context): ClassviewServer = serverInstance ?: synchronized(this) {
        serverInstance ?: ClassviewServer.create(
            dataDir = File(context.filesDir, "server"),
            images = AndroidImageProcessor(),
            config = ServerConfig(
                serverVersion = BuildConfig.VERSION_NAME,
                trustLocalProxy = true,
                web = AssetWebContent(context.applicationContext.assets, "web"),
            ),
            port = SERVER_PORT,
        ).also { serverInstance = it }
    }

    fun prefs(context: Context): HostPrefs = prefsInstance ?: synchronized(this) {
        prefsInstance ?: HostPrefs(context.applicationContext).also { prefsInstance = it }
    }

    fun tailscaleDir(context: Context) = File(context.filesDir, "tailscale")
}
