package com.dedroneTECTION.ui.screens

import android.Manifest
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dedroneTECTION.model.DroneDetection
import com.dedroneTECTION.model.ThreatLevel
import com.dedroneTECTION.ui.theme.*
import com.dedroneTECTION.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val detections by viewModel.detections.collectAsState()

    var userLocation by remember { mutableStateOf(LatLng(37.7749, -122.4194)) }
    var hasLocation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        locationManager?.let { lm ->
            try {
                val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    userLocation = LatLng(lastKnown.latitude, lastKnown.longitude)
                    hasLocation = true
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            userLocation = LatLng(location.latitude, location.longitude)
                            hasLocation = true
                            lm.removeUpdates(this)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 0L, 0f, listener
                    )
                }
            } catch (_: SecurityException) {}
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 14f)
    }

    LaunchedEffect(userLocation) {
        if (hasLocation) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLocation, 14f),
                1000
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true
            )
        ) {
            detections.forEach { detection ->
                if (detection.latitude != null && detection.longitude != null) {
                    val position = LatLng(detection.latitude, detection.longitude)
                    val hue = when (detection.threatLevel) {
                        ThreatLevel.HIGH -> BitmapDescriptorFactory.HUE_RED
                        ThreatLevel.MEDIUM -> BitmapDescriptorFactory.HUE_ORANGE
                        ThreatLevel.LOW -> BitmapDescriptorFactory.HUE_GREEN
                    }

                    Marker(
                        state = MarkerState(position = position),
                        title = detection.name,
                        snippet = "RSSI: ${detection.rssi} dBm | ~${String.format("%.0f", detection.estimatedDistance)}m",
                        icon = BitmapDescriptorFactory.defaultMarker(hue)
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
    }
}
