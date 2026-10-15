package com.realcraftguy.classview.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Eingebaute Avatare. Eigene, einfache Grafiken auf einem 100×100-Raster; die Web-App zeichnet
 * dieselben Formen als SVG (web/mockups/index.html, `AVATARS`). Der [key] wird an den Server übertragen.
 */
enum class AvatarPreset(val key: String, val label: String, val background: Color, val foreground: Color) {
    Welle("welle", "Welle", Color(0xFFDCE8FD), Color(0xFF2B6BE4)),
    Berg("berg", "Berg", Color(0xFFE1F0E6), Color(0xFF2F7A4D)),
    Sonne("sonne", "Sonne", Color(0xFFFFF0D4), Color(0xFFDB8F12)),
    Mond("mond", "Mond", Color(0xFFE8E5F9), Color(0xFF5B4FC4)),
    Stern("stern", "Stern", Color(0xFFFCE6EB), Color(0xFFCB4468)),
    Blatt("blatt", "Blatt", Color(0xFFE2F3EE), Color(0xFF248A6E)),
    Wolke("wolke", "Wolke", Color(0xFFE3F0F7), Color(0xFF3580A3)),
    Planet("planet", "Planet", Color(0xFFF4E7DA), Color(0xFFAE6430)),
    ;

    companion object {
        fun fromKey(key: String?): AvatarPreset? = entries.firstOrNull { it.key == key }
    }
}

@Composable
fun Avatar(
    preset: AvatarPreset?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Color? = null,
    /** Eigenes Foto; hat Vorrang vor [preset]. */
    image: ImageBitmap? = null,
) {
    val ringModifier = if (ring != null) Modifier.border(3.dp, ring, CircleShape) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .clip(CircleShape)
            .background(preset?.background ?: ClassviewTheme.colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else if (preset != null) {
            Canvas(Modifier.size(size)) {
                scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) {
                    drawPreset(preset)
                }
            }
        } else {
            Text(
                text = name.trim().take(1).uppercase(),
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
                color = ClassviewTheme.colors.textSecondary,
            )
        }
    }
}

private fun DrawScope.drawPreset(p: AvatarPreset) {
    val fg = p.foreground
    when (p) {
        AvatarPreset.Welle -> {
            for ((i, y) in listOf(42f, 60f).withIndex()) {
                val path = Path().apply {
                    moveTo(18f, y)
                    cubicTo(28f, y - 10f, 38f, y - 10f, 50f, y)
                    cubicTo(62f, y + 10f, 72f, y + 10f, 82f, y)
                }
                drawPath(path, fg.copy(alpha = if (i == 0) 1f else 0.55f), style = Stroke(7f, cap = StrokeCap.Round))
            }
        }
        AvatarPreset.Berg -> {
            drawPath(Path().apply { moveTo(14f, 72f); lineTo(40f, 34f); lineTo(66f, 72f); close() }, fg)
            drawPath(Path().apply { moveTo(46f, 72f); lineTo(64f, 46f); lineTo(86f, 72f); close() }, fg.copy(alpha = 0.6f))
            drawPath(Path().apply { moveTo(33f, 44f); lineTo(40f, 34f); lineTo(47f, 44f); close() }, Color.White.copy(alpha = 0.85f))
        }
        AvatarPreset.Sonne -> {
            drawCircle(fg, radius = 15f, center = Offset(50f, 50f))
            for (i in 0 until 8) {
                val a = i * PI / 4
                drawLine(
                    fg,
                    Offset(50f + 23f * cos(a).toFloat(), 50f + 23f * sin(a).toFloat()),
                    Offset(50f + 31f * cos(a).toFloat(), 50f + 31f * sin(a).toFloat()),
                    strokeWidth = 6f, cap = StrokeCap.Round,
                )
            }
        }
        AvatarPreset.Mond -> {
            val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(50f, 50f), 24f)) }
            val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(62f, 42f), 21f)) }
            drawPath(Path.combine(PathOperation.Difference, outer, cut), fg)
        }
        AvatarPreset.Stern -> {
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) 29f else 12f
                val a = -PI / 2 + i * PI / 5
                val x = 50f + r * cos(a).toFloat()
                val y = 52f + r * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, fg)
        }
        AvatarPreset.Blatt -> {
            val leaf = Path().apply {
                moveTo(26f, 74f)
                cubicTo(24f, 40f, 46f, 22f, 78f, 24f)
                cubicTo(80f, 56f, 60f, 76f, 26f, 74f)
                close()
            }
            drawPath(leaf, fg)
            drawLine(Color.White.copy(alpha = 0.75f), Offset(30f, 70f), Offset(66f, 36f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
        AvatarPreset.Wolke -> {
            drawCircle(fg, 13f, Offset(37f, 55f))
            drawCircle(fg, 17f, Offset(53f, 47f))
            drawCircle(fg, 12f, Offset(67f, 56f))
            drawRect(fg, topLeft = Offset(37f, 55f), size = Size(30f, 13f))
        }
        AvatarPreset.Planet -> {
            drawCircle(fg, 17f, Offset(50f, 50f))
            drawOval(
                fg.copy(alpha = 0.7f),
                topLeft = Offset(20f, 42f), size = Size(60f, 16f),
                style = Stroke(4.5f),
            )
            drawArc(
                fg, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(33f, 33f), size = Size(34f, 34f),
            )
        }
    }
}
