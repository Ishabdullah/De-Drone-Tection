package com.dedroneTECTION.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.ui.components.ScanningIndicator
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val sdrStatus by viewModel.sdrStatus.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = Primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        SettingsSection(title = "SCAN CONFIGURATION") {
            SettingsRow(
                title = "Scan Interval",
                subtitle = "${uiState.scanInterval / 1000}s"
            ) {
                Slider(
                    value = uiState.scanInterval.toFloat(),
                    onValueChange = { viewModel.setScanInterval(it.toLong()) },
                    valueRange = 1000f..30000f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceVariant
                    ),
                    modifier = Modifier.width(160.dp)
                )
            }

            SettingsRow(
                title = "Acoustic Sensitivity",
                subtitle = "${(uiState.acousticSensitivity * 100).toInt()}%"
            ) {
                Slider(
                    value = uiState.acousticSensitivity,
                    onValueChange = { viewModel.setAcousticSensitivity(it) },
                    valueRange = 0.1f..1.0f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = SurfaceVariant
                    ),
                    modifier = Modifier.width(160.dp)
                )
            }
        }

        SettingsSection(title = "NETWORK") {
            SettingsSwitchRow(
                title = "Firebase Sync",
                subtitle = "Upload detections to cloud",
                checked = uiState.isNetworkEnabled,
                onCheckedChange = { viewModel.toggleNetworkMode() }
            )
        }

        SettingsSection(title = "SIMULATION") {
            SettingsSwitchRow(
                title = "Simulation Mode",
                subtitle = "Inject fake detections every 10s",
                checked = uiState.isSimulationEnabled,
                onCheckedChange = { viewModel.toggleSimulationMode() }
            )
        }

        SettingsSection(title = "ENHANCED RF MODE (RTL-SDR)") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (sdrStatus.connected) Success else Alert,
                            RoundedCornerShape(50)
                        )
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sdrStatus.connected) "SDR Connected" else "No SDR Detected",
                        color = OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (sdrStatus.connected) {
                        Text(
                            text = sdrStatus.deviceName,
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Text(
                text = "Connect an RTL-SDR dongle via USB-C to enable enhanced RF spectrum analysis for drone signal detection.",
                color = OnSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        SettingsSection(title = "ACTIVE SERVICES") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanningIndicator(label = "BLE Scanner", isActive = uiState.isBLEEnabled)
                ScanningIndicator(label = "WiFi Scanner", isActive = uiState.isWiFiEnabled)
                ScanningIndicator(label = "Acoustic Monitor", isActive = uiState.isAcousticEnabled)
                ScanningIndicator(label = "Simulation Engine", isActive = uiState.isSimulationEnabled)
            }
        }

        SettingsSection(title = "PERMISSIONS REQUIRED") {
            val permissions = listOf(
                "BLUETOOTH_SCAN" to "BLE device scanning",
                "BLUETOOTH_CONNECT" to "Connect to BLE devices",
                "ACCESS_FINE_LOCATION" to "Location-based detection",
                "RECORD_AUDIO" to "Acoustic drone detection",
                "INTERNET" to "Firebase cloud sync"
            )

            permissions.forEach { (perm, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = perm,
                        color = Primary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = desc,
                        color = OnSurfaceVariant,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = OnSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                color = OnSurface,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = OnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = OnSurface,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = OnSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = Primary.copy(alpha = 0.3f),
                uncheckedThumbColor = OnSurfaceVariant,
                uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}
