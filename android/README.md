# Health Data MQTT Android App

This Android application serves as a mobile companion to the healthdata2mqtt system, enabling Bluetooth Low Energy (BLE) scanning of health devices and publishing data to an MQTT broker.

## 🚀 Features

### ✅ **Completed Core Features**
- **BLE Health Device Scanner**: Automatically discovers and connects to Mi Body Scales and Omron Blood Pressure monitors
- **MQTT Publisher**: Publishes health data to MQTT broker with authentication support
- **Body Composition Analysis**: Complete port of Python body metrics calculations to Kotlin
- **Blood Pressure Analysis**: Categorizes BP readings using EU/US medical standards
- **Foreground Service**: Background scanning with proper Android lifecycle management
- **Modern UI**: Jetpack Compose-based interface with Material Design 3

### 📱 **Device Support**
- **Mi Body Composition Scale**: Weight, impedance, and full body analysis (BMI, body fat, muscle mass, etc.)
- **Omron Blood Pressure Monitors**: Systolic, diastolic, pulse, with medical categorization
- **Generic Health Devices**: Extensible framework for additional BLE health devices

### 🔐 **Authentication & Security**
- MQTT username/password authentication
- Secure BLE device pairing
- Proper Android permissions handling (Android 6+ location, Android 12+ Bluetooth)

## 🏗️ **Architecture**

### **Core Components**

1. **MainActivity.kt**
   - Main UI with device scanning controls
   - Real-time status display for MQTT and BLE
   - Permission management

2. **BleHealthScannerService.kt**
   - Foreground service for continuous BLE scanning
   - GATT connection management
   - Health device protocol handling
   - Automatic device discovery and connection

3. **MQTTHealthDataPublisher.kt**
   - MQTT client with authentication
   - Publishing health data, raw device data, and device discovery events
   - Connection management with callbacks

### **Data Models**

4. **BodyMetrics.kt & BodyScales.kt**
   - Complete port of Python body composition calculations
   - BMI, body fat %, muscle mass, bone mass, water %, visceral fat
   - BMR, protein %, metabolic age, body type analysis

5. **BloodPressureMetrics.kt**
   - BP categorization (Normal, High-Normal, Grade 1, Grade 2)
   - EU and US medical standards support
   - Health recommendations and risk assessment

## 📋 **Requirements**

### **Android Version**
- **Minimum SDK**: Android 5.0 (API 21)
- **Target SDK**: Android 14 (API 34)
- **Recommended**: Android 6.0+ for location-based BLE scanning

### **Hardware Requirements**
- **Bluetooth LE**: Required for health device communication
- **Location Services**: Required for BLE scanning on Android 6+
- **Internet**: Required for MQTT broker connectivity

### **Permissions**
```xml
<!-- Bluetooth permissions -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- Location (required for BLE scanning) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## 🔧 **Setup & Installation**

### **1. Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 34
- Kotlin 1.9+

### **2. Build & Install**
```bash
cd android/
./gradlew assembleDebug
./gradlew installDebug
```

### **3. Configuration**
The app will connect to your MQTT broker using the same configuration as the main healthdata2mqtt system:
- **Default Broker**: localhost:1883
- **Authentication**: Reads from same config as Python version
- **Topics**: Compatible with existing MQTT listener

## 📊 **MQTT Topic Structure**

### **Published Topics**
```
health/body_composition/{user_email}     # Complete body analysis
health/blood_pressure/{user_email}       # BP readings with categories
health/raw/{device_mac}                  # Raw device data
health/discovery/{device_mac}            # Device discovery events
```

### **Message Format**
```json
{
  "user": "user@example.com",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "type": "body_composition",
  "data": {
    "weight": 70.5,
    "bmi": 22.1,
    "fat_percentage": 15.2,
    "muscle_mass": 55.8,
    "bone_mass": 3.1,
    "water_percentage": 58.4,
    "visceral_fat": 8.0,
    "bmr": 1654,
    "metabolic_age": 28
  }
}
```

## 🔍 **Usage**

### **1. First Launch**
- Grant Bluetooth and Location permissions
- Enable Bluetooth if not already enabled
- App automatically connects to MQTT broker

### **2. Health Device Scanning**
- Toggle "Bluetooth Scanning" switch to start/stop
- App runs in background with persistent notification
- Devices are automatically discovered and connected

### **3. Data Publishing**
- Health data is automatically published to MQTT
- Real-time status shows connection and device count
- Raw data published for debugging/monitoring

## 🧪 **Testing**

### **BLE Device Simulation**
For testing without physical devices, the app publishes device discovery events that can be monitored via MQTT.

### **MQTT Testing**
```bash
# Subscribe to all health topics
mosquitto_sub -h localhost -t "health/#" -v

# Test connection with existing broker
mosquitto_pub -h localhost -t "test/android" -m "Hello from Android"
```

## 🔗 **Integration with Main System**

This Android app is fully compatible with the existing healthdata2mqtt infrastructure:

- **Same MQTT Topics**: Uses identical topic structure
- **Same Data Format**: JSON payload format matches Python version  
- **Same Authentication**: Uses same MQTT credentials
- **Same Health Calculations**: Body composition algorithms identical to Python

## 🚧 **Future Enhancements**

### **Pending Features** (TODO)
- Configuration UI for MQTT broker settings
- Device discovery and manual pairing interface
- Historical data storage and sync
- User profile management
- Additional health device protocols

### **Advanced Features** (Future)
- Local MQTT broker mode
- Data encryption and secure transmission
- Integration with health platforms (Google Fit, Apple Health)
- Multi-user support
- Cloud synchronization

## 🤝 **Contributing**

The Android app follows the same contribution guidelines as the main healthdata2mqtt project. Key areas for contribution:

1. **Additional Device Support**: BLE protocol implementations
2. **UI/UX Improvements**: Material Design enhancements
3. **Performance Optimization**: Battery usage and BLE efficiency
4. **Testing**: Unit tests and device compatibility testing

## 📄 **License**

Same license as the main healthdata2mqtt project.

---

**Note**: This Android app provides a complete mobile solution for health data collection, enabling the healthdata2mqtt system to work seamlessly on Android devices while maintaining full compatibility with the existing Python-based infrastructure.