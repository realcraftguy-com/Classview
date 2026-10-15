package com.realcraftguy.classview.server

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Prüft ein hochgeladenes Profilbild und kodiert es neu. Das Neukodieren entfernt Metadaten (EXIF/GPS)
 * und macht aus jeder Eingabe ein sauberes JPEG. Wirft [UnsupportedImageException], wenn die Daten kein
 * unterstütztes Bild sind.
 */
interface ImageProcessor {
    fun process(bytes: ByteArray): ByteArray
}

class UnsupportedImageException(message: String) : Exception(message)

object ImageLimits {
    const val MAX_UPLOAD_BYTES = 1_000_000
    const val MAX_INPUT_SIDE = 4096
    const val OUTPUT_SIDE = 512
    const val JPEG_QUALITY = 0.85f
}

/** Variante für den PC (javax.imageio: JPEG und PNG). Die Host-App nutzt Androids BitmapFactory. */
class JvmImageProcessor : ImageProcessor {
    override fun process(bytes: ByteArray): ByteArray {
        val (format, width, height) = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull()
                ?: throw UnsupportedImageException("Kein unterstütztes Bildformat")
            try {
                reader.input = input
                Triple(reader.formatName.lowercase(), reader.getWidth(0), reader.getHeight(0))
            } finally {
                reader.dispose()
            }
        }
        if (format !in setOf("jpeg", "jpg", "png")) throw UnsupportedImageException("Format $format wird nicht unterstützt")
        if (width > ImageLimits.MAX_INPUT_SIDE || height > ImageLimits.MAX_INPUT_SIDE || width < 1 || height < 1) {
            throw UnsupportedImageException("Bild ist zu groß")
        }
        val source = ImageIO.read(ByteArrayInputStream(bytes)) ?: throw UnsupportedImageException("Bild nicht lesbar")

        val scale = minOf(1.0, ImageLimits.OUTPUT_SIDE.toDouble() / maxOf(width, height))
        val w = maxOf(1, (width * scale).toInt())
        val h = maxOf(1, (height * scale).toInt())
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        out.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            color = java.awt.Color.WHITE
            fillRect(0, 0, w, h)
            drawImage(source, 0, 0, w, h, null)
            dispose()
        }

        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val buffer = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(buffer).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = ImageLimits.JPEG_QUALITY
            }
            writer.write(null, IIOImage(out, null, null), params)
            writer.dispose()
        }
        return buffer.toByteArray()
    }
}
