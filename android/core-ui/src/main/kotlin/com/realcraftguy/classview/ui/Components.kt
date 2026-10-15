package com.realcraftguy.classview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Weiße Karte mit dezentem Schatten. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ClassviewTheme.colors
    Column(
        modifier
            .shadow(1.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun StatusDot(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Kleine Kapsel mit Statuspunkt, z. B. „Verbunden“. */
@Composable
fun StatusPill(text: String, dot: Color, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val c = ClassviewTheme.colors
    val bg = if (emphasized) c.accent else c.surfaceMuted
    val fg = if (emphasized) c.onAccent else c.text
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusDot(if (emphasized) c.onAccent else dot, 8.dp)
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit = {}) {
    val c = ClassviewTheme.colors
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (danger) c.dangerSoft else c.surfaceMuted,
            contentColor = if (danger) c.danger else c.text,
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Abschnittsüberschrift in Grau, wie in Einstellungslisten. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = ClassviewTheme.colors.textSecondary,
    )
}

/** Schmale Spalte in der Mitte breiter Bildschirme (Tablet), volle Breite auf dem Handy. */
@Composable
fun CenteredColumn(
    maxWidth: Dp = 560.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth(),
            content = content,
        )
    }
}

/** Hintergrundfläche eines ganzen Bildschirms. */
@Composable
fun ScreenSurface(content: @Composable () -> Unit) {
    Surface(color = ClassviewTheme.colors.background, contentColor = ClassviewTheme.colors.text, content = content)
}
