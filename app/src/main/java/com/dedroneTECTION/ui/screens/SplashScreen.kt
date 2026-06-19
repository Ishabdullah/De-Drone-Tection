package com.dedroneTECTION.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.ui.components.RadarSweepAnimation
import com.dedroneTECTION.ui.theme.*

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val dotCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )
    val dots = ".".repeat(dotCount.toInt())

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            RadarSweepAnimation(
                modifier = Modifier.size(160.dp),
                isActive = true,
                detectionCount = 3
            )

            Text(
                text = "DE-DRONE-TECTION",
                color = Primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )

            Text(
                text = "TACTICAL DRONE DETECTION SYSTEM",
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "INITIALIZING SENSORS$dots",
                color = ScanningPulse,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
