package com.realcraftguy.classview.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Eigenes Bildschirm-Symbol (Monitor mit Fuß) auf einem 24×24-Raster. Material-Icons-Core enthält
 * kein passendes Symbol, und das erweiterte Paket wäre für ein Symbol unnötig groß.
 */
@Composable
fun ScreenGlyph(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier, sharing: Boolean = false) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        drawRoundRect(
            color, topLeft = Offset(3f * u, 4f * u), size = Size(18f * u, 12f * u),
            cornerRadius = CornerRadius(2.2f * u), style = Stroke(1.9f * u),
        )
        drawLine(color, Offset(9f * u, 20f * u), Offset(15f * u, 20f * u), strokeWidth = 1.9f * u, cap = StrokeCap.Round)
        drawLine(color, Offset(12f * u, 16f * u), Offset(12f * u, 20f * u), strokeWidth = 1.9f * u)
        if (sharing) {
            // Pfeil nach oben rechts = „wird geteilt“
            drawLine(color, Offset(9.5f * u, 12.5f * u), Offset(14.5f * u, 7.5f * u), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawLine(color, Offset(11f * u, 7.5f * u), Offset(14.5f * u, 7.5f * u), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
            drawLine(color, Offset(14.5f * u, 7.5f * u), Offset(14.5f * u, 11f * u), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        }
    }
}

/** Klassen-Symbol für die Statusleiste: drei Köpfe, der mittlere vorne. */
@Composable
fun ClassGlyph(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        drawCircle(color.copy(alpha = 0.7f), 2.6f * u, Offset(6f * u, 9f * u))
        drawCircle(color.copy(alpha = 0.7f), 2.6f * u, Offset(18f * u, 9f * u))
        drawCircle(color, 3.4f * u, Offset(12f * u, 8f * u))
        drawArc(color.copy(alpha = 0.7f), 180f, 180f, true, Offset(1.5f * u, 13f * u), Size(9f * u, 8f * u))
        drawArc(color.copy(alpha = 0.7f), 180f, 180f, true, Offset(13.5f * u, 13f * u), Size(9f * u, 8f * u))
        drawArc(color, 180f, 180f, true, Offset(6f * u, 13f * u), Size(12f * u, 10f * u))
    }
}
