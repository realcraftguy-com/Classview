package com.realcraftguy.classview.server

import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Startet und stoppt den eingebetteten Server. Wird von der Host-App und vom PC-Startpunkt genutzt.
 * Lauscht standardmäßig nur auf 127.0.0.1 – von außen erreichbar ist er nur über die Tailscale-Brücke.
 */
class ClassviewServer(
    val service: ClassService,
    private val config: ServerConfig,
    private val host: String = "127.0.0.1",
    val port: Int = 8080,
) {
    private var engine: EmbeddedServer<*, *>? = null
    private var scope: CoroutineScope? = null

    @Synchronized
    fun start() {
        if (engine != null) return
        engine = embeddedServer(CIO, port = port, host = host) { classviewModule(service, config) }.start(wait = false)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { s ->
            s.launch {
                while (isActive) {
                    runCatching { service.cleanup() }
                    delay(6 * 60 * 60 * 1000L)
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        scope?.cancel()
        scope = null
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 2000)
        engine = null
    }

    val isRunning: Boolean get() = engine != null

    companion object {
        /** Standardaufbau mit Dateien in [dataDir] (JSON-Datei + Ordner für Profilbilder). */
        fun create(
            dataDir: File,
            images: ImageProcessor,
            config: ServerConfig,
            host: String = "127.0.0.1",
            port: Int = 8080,
        ): ClassviewServer {
            val service = ClassService(
                store = JsonFileDataStore(File(dataDir, "classview.json")),
                avatars = FileAvatarStore(File(dataDir, "avatars")),
                images = images,
            )
            return ClassviewServer(service, config, host, port)
        }
    }
}
