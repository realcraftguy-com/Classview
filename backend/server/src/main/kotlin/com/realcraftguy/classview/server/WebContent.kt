package com.realcraftguy.classview.server

import io.ktor.http.ContentType
import io.ktor.http.defaultForFilePath
import java.io.File

class WebAsset(val bytes: ByteArray, val contentType: ContentType)

/** Liefert die gebaute Lehrer-Web-App (web/dist) aus – vom Dateisystem (PC) oder aus den App-Assets. */
fun interface WebContent {
    fun get(path: String): WebAsset?

    companion object {
        fun safePath(path: String): String? {
            val clean = path.trim('/').ifEmpty { "index.html" }
            if (clean.split('/').any { it == ".." || it.startsWith(".") }) return null
            return clean
        }

        fun contentTypeFor(path: String): ContentType = when {
            path.endsWith(".webmanifest") -> ContentType("application", "manifest+json")
            else -> ContentType.defaultForFilePath(path)
        }

        /** Platzhalterseite, solange die Web-App noch nicht gebaut ist. */
        val Placeholder = WebContent { path ->
            if (path.isEmpty() || path == "index.html") {
                WebAsset(
                    """<!doctype html><html lang="de"><meta charset="utf-8"><title>ClassroomView</title>
                    |<body style="font-family:system-ui;padding:40px">ClassroomView-Server läuft.
                    |Die Lehrer-Web-App wurde noch nicht gebaut (cd web && npm run build).</body></html>""".trimMargin()
                        .toByteArray(),
                    ContentType.Text.Html.withParameter("charset", "utf-8"),
                )
            } else null
        }
    }
}

class DirectoryWebContent(private val root: File) : WebContent {
    override fun get(path: String): WebAsset? {
        val safe = WebContent.safePath(path) ?: return null
        val file = File(root, safe)
        if (!file.isFile || !file.canonicalPath.startsWith(root.canonicalPath)) return null
        return WebAsset(file.readBytes(), WebContent.contentTypeFor(safe))
    }
}
