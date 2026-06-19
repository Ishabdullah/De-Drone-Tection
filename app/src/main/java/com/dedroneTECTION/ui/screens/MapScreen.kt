package com.dedroneTECTION.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.model.DroneDetection
import com.dedroneTECTION.model.ThreatLevel
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detections by viewModel.detections.collectAsState()

    var userLocation by remember { mutableStateOf(Pair(37.7749, -122.4194)) }
    var hasLocation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        locationManager?.let { lm ->
            try {
                val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    userLocation = Pair(lastKnown.latitude, lastKnown.longitude)
                    hasLocation = true
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            userLocation = Pair(location.latitude, location.longitude)
                            hasLocation = true
                            lm.removeUpdates(this)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener)
                }
            } catch (_: SecurityException) {}
        }
    }

    var zoom by remember { mutableFloatStateOf(14f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(8f, 20f)
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    }
                }
        ) {
            val centerLat = userLocation.first
            val centerLon = userLocation.second
            val metersPerPixel = 156543.03 * cos(Math.toRadians(centerLat)) / Math.pow(2.0, zoom.toDouble())

            drawGrid(centerLat, centerLon, metersPerPixel)

            val userScreenX = size.width / 2 + offset.x
            val userScreenY = size.height / 2 + offset.y
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 10.dp.toPx(),
                center = Offset(userScreenX, userScreenY)
            )
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = 0.3f),
                radius = 20.dp.toPx(),
                center = Offset(userScreenX, userScreenY)
            )

            detections.forEach { detection ->
                if (detection.latitude != null && detection.longitude != null) {
                    val dLat = detection.latitude - centerLat
                    val dLon = detection.longitude - centerLon
                    val screenX = (dLon * 111320 * cos(Math.toRadians(centerLat)) / metersPerPixel + size.width / 2 + offset.x).toFloat()
                    val screenY = (-dLat * 110540 / metersPerPixel + size.height / 2 + offset.y).toFloat()

                    if (screenX in -50f..size.width + 50f && screenY in -50f..size.height + 50f) {
                        val color = when (detection.threatLevel) {
                            ThreatLevel.HIGH -> Alert
                            ThreatLevel.MEDIUM -> Primary
                            ThreatLevel.LOW -> Success
                        }
                        drawCircle(
                            color = color.copy(alpha = 0.3f),
                            radius = 16.dp.toPx(),
                            center = Offset(screenX, screenY)
                        )
                        drawCircle(
                            color = color,
                            radius = 8.dp.toPx(),
                            center = Offset(screenX, screenY)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = "MAP VIEW",
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(Surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "${detections.size} drone${if (detections.size != 1) "s" else ""} on map",
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Surface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MapLegendItem(color = Alert, label = "HIGH threat")
            MapLegendItem(color = Primary, label = "MEDIUM threat")
            MapLegendItem(color = Success, label = "LOW threat")
            MapLegendItem(color = Color(0xFF2196F3), label = "Your location")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { zoom = (zoom + 1f).coerceAtMost(20f) },
                containerColor = Surface,
                contentColor = Primary,
                modifier = Modifier.size(40.dp)
            ) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            FloatingActionButton(
                onClick = { zoom = (zoom - 1f).coerceAtLeast(8f) },
                containerColor = Surface,
                contentColor = Primary,
                modifier = Modifier.size(40.dp)
            ) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MapLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun DrawScope.drawGrid(centerLat: Double, centerLon: Double, metersPerPixel: Double) {
    val gridColor = Color(0xFF2C2C2E)
    val gridSpacing = 50.dp.toPx()

    for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
        drawLine(
            color = gridColor,
            start = Offset(x.toFloat(), 0f),
            end = Offset(x.toFloat(), size.height),
            strokeWidth = 0.5.dp.toPx()
        )
    }
    for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y.toFloat()),
            end = Offset(size.width, y.toFloat()),
            strokeWidth = 0.5.dp.toPx()
        )
    }
}
