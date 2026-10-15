package com.realcraftguy.classview.host

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.realcraftguy.classview.server.ImageLimits
import com.realcraftguy.classview.server.ImageProcessor
import com.realcraftguy.classview.server.UnsupportedImageException
import com.realcraftguy.classview.server.WebAsset
import com.realcraftguy.classview.server.WebContent
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

/**
 * Bildprüfung mit Androids BitmapFactory: erst nur die Maße lesen (schützt vor riesigen Bildern), dann
 * verkleinert dekodieren und als JPEG neu kodieren. Das entfernt alle Metadaten.
 */
class AndroidImageProcessor : ImageProcessor {
    override fun process(bytes: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val mime = bounds.outMimeType ?: throw UnsupportedImageException("Kein Bild")
        if (mime !in setOf("image/jpeg", "image/png", "image/webp")) throw UnsupportedImageException("Format $mime")
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w < 1 || h < 1 || w > ImageLimits.MAX_INPUT_SIDE || h > ImageLimits.MAX_INPUT_SIDE) {
            throw UnsupportedImageException("Bild ist zu groß")
        }
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= ImageLimits.OUTPUT_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw UnsupportedImageException("Bild nicht lesbar")

        val scale = minOf(1f, ImageLimits.OUTPUT_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val outW = maxOf(1, (decoded.width * scale).toInt())
        val outH = maxOf(1, (decoded.height * scale).toInt())
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.WHITE) // transparente PNGs bekommen weißen Grund
            drawBitmap(decoded, null, Rect(0, 0, outW, outH), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        decoded.recycle()
        val buffer = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, (ImageLimits.JPEG_QUALITY * 100).toInt(), buffer)
        out.recycle()
        return buffer.toByteArray()
    }
}

/** Liefert die beim Bauen in die APK gepackte Web-App aus (assets/web). */
class AssetWebContent(private val assets: AssetManager, private val root: String) : WebContent {
    override fun get(path: String): WebAsset? {
        val safe = WebContent.safePath(path) ?: return null
        return try {
            val bytes = assets.open("$root/$safe").use { it.readBytes() }
            WebAsset(bytes, WebContent.contentTypeFor(safe))
        } catch (e: FileNotFoundException) {
            if (safe == "index.html") WebContent.Placeholder.get("") else null
        }
    }
}
