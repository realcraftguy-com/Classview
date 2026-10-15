package com.realcraftguy.classview.server

import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Startpunkt zum Entwickeln auf dem PC: ./gradlew :backend:server:run
 *
 * Umgebungsvariablen:
 *  - CLASSVIEW_DATA  Datenordner (Standard: .dev-data)
 *  - CLASSVIEW_HOST  Adresse (Standard: 127.0.0.1; 0.0.0.0 für Tests im WLAN)
 *  - CLASSVIEW_PORT  Port (Standard: 8080)
 *  - CLASSVIEW_WEB   Ordner der gebauten Web-App (Standard: web/dist)
 */
fun main() {
    val dataDir = File(System.getenv("CLASSVIEW_DATA") ?: ".dev-data")
    val webDir = File(System.getenv("CLASSVIEW_WEB") ?: "web/dist")
    val host = System.getenv("CLASSVIEW_HOST") ?: "127.0.0.1"
    val port = System.getenv("CLASSVIEW_PORT")?.toIntOrNull() ?: 8080

    val config = ServerConfig(
        serverVersion = "dev",
        trustLocalProxy = false,
        web = if (File(webDir, "index.html").isFile) DirectoryWebContent(webDir) else WebContent.Placeholder,
    )
    val server = ClassviewServer.create(dataDir, JvmImageProcessor(), config, host, port)

    // Einladungsschlüssel für die Lehrer-Web-App: nur beim ersten Start erzeugen und merken.
    val keyFile = File(dataDir, "invite-key.txt")
    val inviteKey = if (keyFile.isFile) keyFile.readText().trim() else runBlocking { server.service.rotateInviteKey() }.also {
        dataDir.mkdirs()
        keyFile.writeText(it)
    }

    server.start()
    println(
        """
        |ClassroomView-Server läuft auf http://$host:$port
        |Einladungslink:  http://$host:$port/#/invite/$inviteKey
        |Daten:           ${dataDir.absolutePath}
        |Beenden mit Strg+C.
        """.trimMargin(),
    )
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    Thread.currentThread().join()
}
