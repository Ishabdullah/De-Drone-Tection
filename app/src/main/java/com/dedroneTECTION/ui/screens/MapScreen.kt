package com.dedroneTECTION.ui.screens

import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.model.ThreatLevel
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.viewmodel.MainViewModel
import kotlin.math.roundToInt

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
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(8f, 20f)
                    panOffset = Offset(panOffset.x + pan.x, panOffset.y + pan.y)
                }
            }
    ) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight

        val centerLat = userLocation.first
        val centerLon = userLocation.second
        val metersPerPixel = 156543.03 * Math.cos(Math.toRadians(centerLat)) / Math.pow(2.0, zoom.toDouble())
        val pxPerDegLon = 111320.0 * Math.cos(Math.toRadians(centerLat)) / metersPerPixel
        val pxPerDegLat = 110540.0 / metersPerPixel

        val halfW = boxWidth.value / 2f
        val halfH = boxHeight.value / 2f

        for (i in 0..20) {
            val xPos = ((i * 200f + panOffset.x) % (20 * 200f))
            Box(
                modifier = Modifier
                    .offset { IntOffset(xPos.roundToInt(), 0) }
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(gridLineColor)
            )
        }
        for (i in 0..25) {
            val yPos = ((i * 200f + panOffset.y) % (25 * 200f))
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, yPos.roundToInt()) }
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(gridLineColor)
            )
        }

        val userScreenX = halfW + panOffset.x
        val userScreenY = halfH + panOffset.y

        Box(
            modifier = Modifier
                .offset { IntOffset((userScreenX - 24).roundToInt(), (userScreenY - 24).roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3).copy(alpha = 0.2f))
        )
        Box(
            modifier = Modifier
                .offset { IntOffset((userScreenX - 10).roundToInt(), (userScreenY - 10).roundToInt()) }
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3))
        )

        detections.forEach { detection ->
            if (detection.latitude != null && detection.longitude != null) {
                val dLat = detection.latitude - centerLat
                val dLon = detection.longitude - centerLon
                val sx = (dLon * pxPerDegLon + halfW + panOffset.x).roundToInt()
                val sy = (-dLat * pxPerDegLat + halfH + panOffset.y).roundToInt()

                if (sx in -60..boxWidth.value.roundToInt() + 60 && sy in -60..boxHeight.value.roundToInt() + 60) {
                    val color = when (detection.threatLevel) {
                        ThreatLevel.HIGH -> Alert
                        ThreatLevel.MEDIUM -> Primary
                        ThreatLevel.LOW -> Success
                    }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(sx - 20, sy - 20) }
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(sx - 8, sy - 8) }
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
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
            LegendRow(color = Alert, label = "HIGH threat")
            LegendRow(color = Primary, label = "MEDIUM threat")
            LegendRow(color = Success, label = "LOW threat")
            LegendRow(color = Color(0xFF2196F3), label = "Your location")
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

private val gridLineColor = Color(0xFF1C1C1E)

@Composable
private fun LegendRow(color: Color, label: String) {
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
