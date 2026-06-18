package com.dedroneTECTION.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.ui.components.*
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.model.DroneDetection
import com.dedroneTECTION.viewmodel.MainViewModel

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val detections by viewModel.detections.collectAsState()
    val lastDetection = detections.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "DASHBOARD",
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarSweepAnimation(
                    isActive = uiState.isBLEEnabled || uiState.isWiFiEnabled || uiState.isSimulationEnabled,
                    detectionCount = detections.size.coerceAtMost(5)
                )
            }
        }

        item {
            ThreatSummaryBar(
                high = uiState.highThreatCount,
                medium = uiState.mediumThreatCount,
                low = uiState.lowThreatCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ACTIVE SCANS",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScanToggle(
                        label = "BLE",
                        isActive = uiState.isBLEEnabled,
                        onToggle = { viewModel.toggleBLEScan() }
                    )
                    ScanToggle(
                        label = "WiFi",
                        isActive = uiState.isWiFiEnabled,
                        onToggle = { viewModel.toggleWiFiScan() }
                    )
                    ScanToggle(
                        label = "Audio",
                        isActive = uiState.isAcousticEnabled,
                        onToggle = { viewModel.toggleAcousticDetection() }
                    )
                    ScanToggle(
                        label = "SIM",
                        isActive = uiState.isSimulationEnabled,
                        onToggle = { viewModel.toggleSimulationMode() }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NETWORK MODE",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = uiState.isNetworkEnabled,
                    onCheckedChange = { viewModel.toggleNetworkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = OnSurfaceVariant,
                        uncheckedTrackColor = SurfaceVariant
                    )
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "LAST DETECTION",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (lastDetection != null) {
                    DroneDetectionCard(detection = lastDetection)
                } else {
                    Text(
                        text = "No drones detected",
                        color = OnSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Drones Detected",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${detections.size} active",
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                ThreatBadge(
                    level = when {
                        uiState.highThreatCount > 0 -> com.dedroneTECTION.model.ThreatLevel.HIGH
                        uiState.mediumThreatCount > 0 -> com.dedroneTECTION.model.ThreatLevel.MEDIUM
                        else -> com.dedroneTECTION.model.ThreatLevel.LOW
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanToggle(
    label: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isActive) Primary.copy(alpha = 0.2f) else SurfaceVariant,
                    RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            ScanningIndicator(
                label = "",
                isActive = isActive
            )
        }
        TextButton(onClick = onToggle) {
            Text(
                text = label,
                color = if (isActive) Primary else OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DroneDetectionCard(detection: DroneDetection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detection.name,
                color = OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "RSSI: ${detection.rssi} dBm | ~${String.format("%.0fm", detection.estimatedDistance)}",
                color = OnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        ThreatBadge(level = detection.threatLevel)
    }
}
