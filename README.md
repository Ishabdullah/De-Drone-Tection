# De-Drone-Tection

A tactical drone detection Android application that uses BLE scanning, WiFi analysis, and acoustic monitoring to detect nearby unmanned aerial vehicles (UAVs).

## Features

### Core Detection
- **BLE RemoteID Scanner** - Scans for Bluetooth Low Energy advertisements carrying ASTM F3411 RemoteID payloads
- **WiFi Scanner** - Detects WiFi NAN/Beacon frames with RemoteID signatures
- **Acoustic Detection** - Real-time FFT analysis for rotor acoustic signatures in 100-800 Hz range
- **Drone Detection List** - Real-time display with RSSI, estimated distance, GPS location, and threat level

### Visualization
- **Radar Sweep Animation** - Pulsing radar display on dashboard
- **Map View** - Google Maps integration showing drone GPS coordinates
- **Live Spectrogram** - Acoustic frequency visualization

### Backend
- **Firebase Integration** - Cloud sync for anonymized detection data
- **Network Mode Toggle** - Enable/disable cloud synchronization

### Enhanced RF
- **RTL-SDR Support** - USB device detection for SDR dongles
- **SDR Status Display** - Real-time connection status

## Screenshots

| Dashboard | Detections | Map | Settings |
|-----------|------------|-----|----------|
| ![Dashboard](screenshots/dashboard.png) | ![Detections](screenshots/detections.png) | ![Map](screenshots/map.png) | ![Settings](screenshots/settings.png) |

## Requirements

### Permissions
- `BLUETOOTH_SCAN` - BLE device scanning
- `BLUETOOTH_CONNECT` - Connect to BLE devices
- `ACCESS_FINE_LOCATION` - Location-based detection
- `RECORD_AUDIO` - Acoustic drone detection
- `INTERNET` - Firebase cloud sync
- `POST_NOTIFICATIONS` - Foreground service notifications

### Android Version
- Minimum SDK: 28 (Android 9)
- Target SDK: 34 (Android 14)

## Build Instructions

### Prerequisites
- Android Studio Arctic Fox (2020.3.1) or later
- JDK 17
- Android SDK 34

### Build Debug APK
```bash
# Clone the repository
git clone https://github.com/your-username/De-Drone-Tection.git
cd De-Drone-Tection

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease
```

## Architecture

- **MVVM Pattern** - Model-View-ViewModel architecture
- **Jetpack Compose** - Modern declarative UI
- **StateFlow** - Reactive state management
- **Repository Pattern** - Clean data layer separation

### Project Structure
```
app/src/main/java/com/dedroneTECTION/
├── model/           # Data models (DroneDetection, ThreatLevel, etc.)
├── repository/      # Data repositories (Firebase, Detection)
├── service/         # Background services (BLE, Acoustic, WiFi)
├── ui/
│   ├── components/  # Reusable UI components
│   ├── screens/     # Screen composables
│   └── theme/       # Theme definitions
├── util/            # Utility classes (FFT, RemoteID parser)
├── viewmodel/       # ViewModels
└── MainActivity.kt  # Main activity with navigation
```

## Technical Details

### BLE RemoteID Parsing
- Parses ASTM F3411-22 RemoteID messages
- Supports Basic ID and Location message types
- Extracts drone ID, type, GPS coordinates, and altitude

### Acoustic Analysis
- FFT processing with Hann windowing
- Dominant frequency detection (100-800 Hz)
- Harmonic analysis for rotor signature matching
- Real-time confidence scoring

### Threat Assessment
- Distance-based threat levels:
  - **HIGH** - Under 50 meters
  - **MEDIUM** - 50-200 meters
  - **LOW** - Over 200 meters

## Simulation Mode

When no real drones are present, enable Simulation Mode in Settings to inject fake drone detections every 10 seconds for testing and demonstration purposes.

## Firebase Setup

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package name `com.dedroneTECTION`
3. Download `google-services.json` and place it in `app/` directory
4. Enable Realtime Database in Firebase Console

## GitHub Actions

The repository includes a CI/CD pipeline that:
- Triggers on push to main/develop branches
- Sets up JDK 17
- Builds the debug APK
- Uploads APK as a build artifact

## License

MIT License - See LICENSE file for details

## Disclaimer

This application is for educational and authorized security testing purposes only. Always obtain proper authorization before detecting or monitoring drone activity in your area.
