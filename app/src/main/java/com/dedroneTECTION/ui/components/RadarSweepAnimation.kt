package com.dedroneTECTION.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.dedroneTECTION.ui.theme.Alert
import com.dedroneTECTION.ui.theme.Primary
import com.dedroneTECTION.ui.theme.RadarSweep

@Composable
fun RadarSweepAnimation(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    detectionCount: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val detectionPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "detection"
    )

    Canvas(modifier = modifier.size(200.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.width / 2 - 4.dp.toPx()

        drawCircle(
            color = Primary.copy(alpha = 0.1f),
            radius = radius,
            center = Offset(centerX, centerY)
        )

        for (i in 1..3) {
            drawCircle(
                color = Primary.copy(alpha = 0.15f),
                radius = radius * i / 4,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawLine(
            color = Primary.copy(alpha = 0.2f),
            start = Offset(centerX - radius, centerY),
            end = Offset(centerX + radius, centerY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Primary.copy(alpha = 0.2f),
            start = Offset(centerX, centerY - radius),
            end = Offset(centerX, centerY + radius),
            strokeWidth = 1.dp.toPx()
        )

        if (isActive) {
            rotate(sweepAngle, Offset(centerX, centerY)) {
                drawLine(
                    color = Primary.copy(alpha = pulseAlpha),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, centerY - radius),
                    strokeWidth = 2.dp.toPx()
                )

                drawArc(
                    color = RadarSweep.copy(alpha = pulseAlpha * 0.3f),
                    startAngle = -30f,
                    sweepAngle = 30f,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
        }

        if (detectionCount > 0) {
            repeat(detectionCount.coerceAtMost(5)) { i ->
                val angle = (i * 72f + detectionPulse) * (Math.PI / 180f)
                val dist = radius * (0.4f + (i * 0.12f))
                val dx = centerX + dist * kotlin.math.cos(angle).toFloat()
                val dy = centerY + dist * kotlin.math.sin(angle).toFloat()

                drawCircle(
                    color = if (i == 0) Alert else Primary,
                    radius = 4.dp.toPx(),
                    center = Offset(dx, dy)
                )
                drawCircle(
                    color = (if (i == 0) Alert else Primary).copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = Offset(dx, dy)
                )
            }
        }
    }
}
