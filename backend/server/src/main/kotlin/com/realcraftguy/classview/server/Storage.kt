package com.realcraftguy.classview.server

import com.realcraftguy.classview.protocol.ProtocolJson
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Lädt und speichert den gesamten [ServerData]-Zustand. */
interface DataStore {
    fun load(): ServerData
    fun save(data: ServerData)
}

/** Speichert Profilbilder (bereits geprüft und neu kodiert) unter zufälligen Namen. */
interface AvatarStore {
    fun put(name: String, bytes: ByteArray)
    fun get(name: String): ByteArray?
    fun delete(name: String)
}

class MemoryDataStore(private var data: ServerData = ServerData()) : DataStore {
    var saves = 0
        private set

    override fun load(): ServerData = ProtocolJson.decodeFromString(ProtocolJson.encodeToString(data))
    override fun save(data: ServerData) {
        this.data = ProtocolJson.decodeFromString(ProtocolJson.encodeToString(data))
        saves++
    }
}

class MemoryAvatarStore : AvatarStore {
    private val files = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    override fun put(name: String, bytes: ByteArray) { files[name] = bytes }
    override fun get(name: String): ByteArray? = files[name]
    override fun delete(name: String) { files.remove(name) }
    val size get() = files.size
}

/**
 * JSON-Datei, atomar geschrieben: erst in eine temporäre Datei, dann per Umbenennen ersetzt. So bleibt
 * bei einem Absturz mitten im Schreiben immer eine vollständige Datei übrig.
 */
class JsonFileDataStore(private val file: File) : DataStore {
    override fun load(): ServerData {
        if (!file.exists()) return ServerData()
        return ProtocolJson.decodeFromString(ServerData.serializer(), file.readText())
    }

    override fun save(data: ServerData) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(ProtocolJson.encodeToString(ServerData.serializer(), data))
        try {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

class FileAvatarStore(private val dir: File) : AvatarStore {
    private val safeName = Regex("^[A-Za-z0-9_-]{8,64}\\.jpg$")

    private fun fileFor(name: String): File {
        if (!safeName.matches(name)) throw IOException("ungültiger Dateiname")
        return File(dir, name)
    }

    override fun put(name: String, bytes: ByteArray) {
        dir.mkdirs()
        fileFor(name).writeBytes(bytes)
    }

    override fun get(name: String): ByteArray? = fileFor(name).takeIf { it.exists() }?.readBytes()

    override fun delete(name: String) {
        fileFor(name).delete()
    }
}
