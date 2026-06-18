package com.dedroneTECTION.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.model.ThreatLevel

@Composable
fun ThreatBadge(level: ThreatLevel, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "threat_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (color, text) = when (level) {
        ThreatLevel.HIGH -> Alert to "HIGH"
        ThreatLevel.MEDIUM -> Primary to "MEDIUM"
        ThreatLevel.LOW -> Success to "LOW"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = if (level == ThreatLevel.HIGH) pulseAlpha else 0.8f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (level == ThreatLevel.HIGH) OnBackground else OnPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ScanningIndicator(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isActive) ScanningPulse.copy(alpha = dotAlpha) else OnSurfaceVariant.copy(alpha = 0.3f))
        )
        Text(
            text = label,
            color = if (isActive) OnSurface else OnSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ThreatSummaryBar(
    high: Int,
    medium: Int,
    low: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ThreatCount(count = high, label = "HIGH", color = Alert)
        ThreatCount(count = medium, label = "MED", color = Primary)
        ThreatCount(count = low, label = "LOW", color = Success)
    }
}

@Composable
private fun ThreatCount(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            color = color,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PulseDot(
    isActive: Boolean,
    color: androidx.compose.ui.graphics.Color = ScanningPulse,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size((8.dp * scale).coerceIn(6.dp, 12.dp))
            .clip(RoundedCornerShape(50))
            .background(if (isActive) color else OnSurfaceVariant.copy(alpha = 0.3f))
    )
}
