package com.dedroneTECTION.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.model.DetectionMethod
import com.dedroneTECTION.model.DroneDetection
import com.dedroneTECTION.model.ThreatLevel
import com.dedroneTECTION.ui.components.ThreatBadge
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetectionsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val detections by viewModel.detections.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredDetections = if (searchQuery.isEmpty()) {
        detections
    } else {
        detections.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.droneId.contains(searchQuery, ignoreCase = true) ||
                    it.detectionMethod.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Text(
            text = "DETECTIONS",
            color = Primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Search drones…",
                    color = OnSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = OnSurfaceVariant)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = SurfaceVariant,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${filteredDetections.size} DETECTIONS",
            color = OnSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDetections, key = { it.id }) { detection ->
                DetectionListItem(detection = detection)
            }
        }
    }
}

@Composable
private fun DetectionListItem(detection: DroneDetection) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detection.name,
                color = OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            ThreatBadge(level = detection.threatLevel)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoChip(label = "RSSI", value = "${detection.rssi} dBm")
            InfoChip(label = "DIST", value = "~${String.format("%.0f", detection.estimatedDistance)}m")
        }

        if (detection.latitude != null && detection.longitude != null) {
            Text(
                text = "Lat: ${String.format("%.6f", detection.latitude)}, Lon: ${String.format("%.6f", detection.longitude)}",
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Via: ${detection.detectionMethod.name}",
                color = Primary.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = dateFormat.format(Date(detection.timestamp)),
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = OnSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
