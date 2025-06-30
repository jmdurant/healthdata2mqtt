# Algorithm Sync Guide: Python ↔ Android Kotlin

This guide documents how to keep the Android Kotlin implementations in sync with upstream Python algorithm changes from the [RobertWojtowicz/export2garmin](https://github.com/RobertWojtowicz/export2garmin) project.

## 🔄 **Sync Process Overview**

1. **Monitor Upstream**: Use `./track_upstream_changes.sh` to detect changes
2. **Update Python**: Apply algorithm changes to Python files
3. **Port to Kotlin**: Update corresponding Android Kotlin files
4. **Test All Platforms**: Verify standalone, Docker, and Android

## 📁 **File Mapping: Python → Android Kotlin**

### **Body Composition Algorithms**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| `miscale/Xiaomi_Scale_Body_Metrics.py` | `android/app/src/main/java/com/healthdata/mqtt/data/BodyMetrics.kt` | BMI, body fat, muscle mass, BMR calculations |
| `miscale/body_scales.py` | `android/app/src/main/java/com/healthdata/mqtt/data/BodyScales.kt` | Reference tables for health metrics |

### **Blood Pressure Algorithms**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| `omron/sharedDriver.py` | `android/app/src/main/java/com/healthdata/mqtt/data/BloodPressureMetrics.kt` | BP categorization, EU/US standards |

### **Pulse Oximetry Algorithms**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| External: [tonyfu97/Pulse-Ox-BLE](https://github.com/tonyfu97/Pulse-Ox-BLE) | `android/app/src/main/java/com/healthdata/mqtt/data/PulseOximetryReading.kt` | SpO2, heart rate, signal quality assessment |

### **Temperature Monitoring**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| N/A (Standard BLE protocols) | `android/app/src/main/java/com/healthdata/mqtt/data/TemperatureReading.kt` | IEEE-11073 temperature data parsing |

### **BLE Communication (Android-Specific)**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| `miscale/miscale_ble.py` | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | MISCALE BLE device scanning |
| `omron/omblepy.py` | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | Omron BP device communication |
| External: [tonyfu97/Pulse-Ox-BLE](https://github.com/tonyfu97/Pulse-Ox-BLE) | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | OxySmart pulse oximeter (Nordic UART) |
| Standard BLE protocols | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | FT95 thermometer (Health Thermometer Service) |

## 🔬 **Key Algorithm Functions to Sync**

### **1. Body Composition Calculations**

#### **Python (`Xiaomi_Scale_Body_Metrics.py`)**
```python
def body_fat_calculation(impedance, height, weight, age, sex):
    # BMR calculation using Mifflin-St Jeor equation
    bmr = bmr_calculation(height, weight, age, sex)
    
    # Body fat percentage using impedance
    if sex == "male":
        body_fat = (1.2 * bmi) + (0.23 * age) - (10.8 * 1) - 5.4
    else:
        body_fat = (1.2 * bmi) + (0.23 * age) - (10.8 * 0) - 5.4
    
    return body_fat, bmr
```

#### **Kotlin (`BodyMetrics.kt`)**
```kotlin
fun calculateBodyFat(impedance: Int, height: Int, weight: Double, age: Int, isMale: Boolean): BodyComposition {
    val bmr = calculateBMR(height, weight, age, isMale)
    val bmi = weight / ((height / 100.0) * (height / 100.0))
    
    val bodyFat = (1.2 * bmi) + (0.23 * age) - (10.8 * if (isMale) 1 else 0) - 5.4
    
    return BodyComposition(bodyFat = bodyFat, bmr = bmr.toInt())
}
```

### **2. Blood Pressure Categorization**

#### **Python (`sharedDriver.py`)**
```python
def categorize_blood_pressure(systolic, diastolic, region="EU"):
    if region == "EU":
        if systolic < 120 and diastolic < 80:
            return "Normal"
        elif systolic < 130 and diastolic < 85:
            return "High-Normal"
        # ... more categories
```

#### **Kotlin (`BloodPressureMetrics.kt`)**
```kotlin
fun categorizeBP(systolic: Int, diastolic: Int, region: String = "EU"): BPCategory {
    return when (region) {
        "EU" -> when {
            systolic < 120 && diastolic < 80 -> BPCategory.NORMAL
            systolic < 130 && diastolic < 85 -> BPCategory.HIGH_NORMAL
            // ... more categories
        }
        else -> BPCategory.UNKNOWN
    }
}
```

### **3. Pulse Oximetry Data Processing**

#### **Python (External Reference: tonyfu97/Pulse-Ox-BLE)**
```python
def _handle_data(self, handle, data):
    if data.startswith(b'\xaaU\x0f\x07\x02') and len(data) == 11:
        pleth_data = data[5:-1]  # Skip header and last byte
        pleth_data_as_int = [int(b) for b in pleth_data]
        
        # Adjust for systolic peak encoding
        for i, b in enumerate(pleth_data_as_int):
            if b > 127:
                b = b - 128  # Remove systolic peak marker
```

#### **Kotlin (`PulseOximetryReading.kt`)**
```kotlin
fun createFromBleData(rawData: ByteArray, deviceAddress: String): PulseOximetryReading? {
    // Validate packet structure (11 bytes with header)
    val expectedHeader = byteArrayOf(0xAA.toByte(), 0x55, 0x0F, 0x07, 0x02)
    if (!rawData.sliceArray(0..4).contentEquals(expectedHeader)) return null
    
    // Process plethysmogram data for systolic peaks
    val processedPlethData = plethData.map { byte ->
        val intValue = byte.toInt() and 0xFF
        if (intValue > 127) (intValue - 128).toByte() else intValue.toByte()
    }.toByteArray()
}
```

### **4. Temperature Data Processing**

#### **Python (Standard IEEE-11073 Reference)**
```python
# Standard IEEE-11073 FLOAT parsing (no specific upstream source)
def parse_ieee11073_float(data, offset):
    mantissa = data[offset] | (data[offset + 1] << 8) | (data[offset + 2] << 16)
    exponent = data[offset + 3]
    return mantissa * (10 ** exponent)
```

#### **Kotlin (`TemperatureReading.kt`)**
```kotlin
private fun parseIEEE11073Float(data: ByteArray, offset: Int): Double {
    val mantissa = (data[offset].toInt() and 0xFF) or 
                  ((data[offset + 1].toInt() and 0xFF) shl 8) or
                  ((data[offset + 2].toInt() and 0xFF) shl 16)
    val exponent = data[offset + 3].toInt()
    return mantissa * 10.0.pow(exponent.toDouble())
}
```

## 🛠️ **Sync Workflow**

### **Step 1: Detect Changes**
```bash
# Run the tracking script
./track_upstream_changes.sh

# If algorithm files changed, review the specific differences
git diff HEAD upstream/master -- miscale/Xiaomi_Scale_Body_Metrics.py
git diff HEAD upstream/master -- miscale/body_scales.py
```

### **Step 2: Update Python Files**
```bash
# Apply algorithm updates (preserve MQTT export functionality)
git checkout upstream/master -- miscale/Xiaomi_Scale_Body_Metrics.py
git checkout upstream/master -- miscale/body_scales.py
git checkout upstream/master -- omron/sharedDriver.py

# Note: Pulse oximetry and temperature algorithms are external/standard protocols
# Monitor external repositories:
# - https://github.com/tonyfu97/Pulse-Ox-BLE for OxySmart updates
# - IEEE-11073 standards for temperature protocol changes

# Test Python functionality
python3 test_export_simple.py
```

### **Step 3: Port to Android Kotlin**

#### **For Body Composition Changes:**
1. Review changes in `miscale/Xiaomi_Scale_Body_Metrics.py`
2. Update corresponding functions in `android/app/src/main/java/com/healthdata/mqtt/data/BodyMetrics.kt`
3. Update reference tables in `android/app/src/main/java/com/healthdata/mqtt/data/BodyScales.kt`

#### **For Blood Pressure Changes:**
1. Review changes in `omron/sharedDriver.py`
2. Update corresponding functions in `android/app/src/main/java/com/healthdata/mqtt/data/BloodPressureMetrics.kt`

#### **For Pulse Oximetry Changes:**
1. Monitor [tonyfu97/Pulse-Ox-BLE](https://github.com/tonyfu97/Pulse-Ox-BLE) for protocol updates
2. Update parsing logic in `android/app/src/main/java/com/healthdata/mqtt/data/PulseOximetryReading.kt`
3. Update BLE communication in `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt`

#### **For Temperature Changes:**
1. Monitor IEEE-11073 standard updates for health thermometer protocols
2. Update parsing logic in `android/app/src/main/java/com/healthdata/mqtt/data/TemperatureReading.kt`
3. Update BLE service handling in `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt`

### **Step 4: Test All Platforms**
```bash
# Test Python standalone
python3 test_export_simple.py

# Test Docker
docker-compose up --build

# Test Android
cd android && ./gradlew assembleDebug
```

## 📊 **Specific Algorithm Areas to Monitor**

### **High Impact - Always Sync**
- ✅ **BMR Calculations**: Core metabolic rate formulas
- ✅ **Body Fat Algorithms**: Impedance-based calculations
- ✅ **BP Categorization**: Medical standard classifications (EU/US)
- ✅ **Reference Tables**: Age/gender-specific ranges
- ✅ **SpO2 Health Categories**: Oxygen saturation thresholds
- ✅ **Heart Rate Categories**: Bradycardia/tachycardia classifications

### **Medium Impact - Review & Sync**
- ⚠️ **Device Detection**: BLE scanning improvements
- ⚠️ **Data Parsing**: Raw sensor data interpretation (PPG, IEEE-11073)
- ⚠️ **Unit Conversions**: lbs/kg, metric/imperial, °C/°F
- ⚠️ **Signal Quality**: PPG data validation and quality assessment
- ⚠️ **Temperature Calibration**: Device-specific adjustment factors

### **Low Impact - Optional Sync**
- 🔍 **Error Handling**: Non-algorithm improvements
- 🔍 **Logging**: Debug/status messages (OXYSMART *, THERMOMETER *)
- 🔍 **Configuration**: Export-specific settings
- 🔍 **BLE Connection**: Timeout and retry logic

## 🔧 **Common Porting Patterns**

### **Python → Kotlin Type Conversions**
| Python | Kotlin | Notes |
|--------|--------|-------|
| `float` | `Double` | Decimal precision |
| `int` | `Int` | Integer values |
| `str` | `String` | Text data |
| `bool` | `Boolean` | True/false values |
| `dict` | `Map<String, Any>` | Key-value pairs |
| `list` | `List<T>` | Arrays/collections |

### **Mathematical Function Conversions**
```python
# Python
import math
result = math.pow(x, 2)
rounded = round(value, 1)
```

```kotlin
// Kotlin
import kotlin.math.*
val result = x.pow(2.0)
val rounded = (value * 10).roundToInt() / 10.0
```

## 📋 **Testing Checklist**

### **After Each Sync:**
- [ ] Python algorithms produce correct results
- [ ] Android Kotlin algorithms produce identical results
- [ ] All unit tests pass
- [ ] MQTT data format remains consistent across all devices
- [ ] Docker build completes successfully
- [ ] Android app compiles without errors
- [ ] All 4 device types supported: MISCALE, Omron, FT95, OxySmart
- [ ] MQTT topics follow consistent pattern: `healthdata/{user}/{device_type}`
- [ ] REST API endpoints work for all data types

### **Verification Commands:**
```bash
# Python syntax check
python3 -m py_compile miscale/Xiaomi_Scale_Body_Metrics.py

# Android build check
cd android && ./gradlew assembleDebug

# Algorithm consistency test (create this test)
python3 test_algorithm_consistency.py
```

## 🚨 **Important Notes**

1. **Preserve MQTT Functionality**: Never sync files that remove MQTT export logic
2. **Test Thoroughly**: Algorithm changes affect health data accuracy
3. **Version Tracking**: Update version comments in both Python and Kotlin
4. **Documentation**: Update this guide when new algorithms are added
5. **Backup**: Always test in a branch before merging to main

## 📞 **When to Sync**

- ✅ **Immediately**: Health calculation algorithm improvements
- ✅ **Regularly**: Reference table updates (BMI scales, BP categories, SpO2 thresholds)
- ✅ **As Needed**: Device compatibility improvements, new BLE protocol support
- ✅ **Monitor External**: tonyfu97/Pulse-Ox-BLE repository for OxySmart updates
- ✅ **Standards Updates**: IEEE-11073 health device protocol changes
- ❌ **Never**: Export destination changes (Garmin → MQTT is our fork)

## 🏥 **Supported Health Devices**

| Device | Purpose | Data Output | MQTT Topic |
|--------|---------|-------------|------------|
| **🌡️ FT95 Thermometer** | Body temperature | Temperature (°C/°F), location | `healthdata/{user}/temperature` |
| **⚖️ MISCALE Body Scale** | Body composition | Weight, BMI, body fat, muscle mass | `healthdata/{user}/body_composition` |
| **🩺 Omron Blood Pressure** | Cardiovascular health | Systolic/diastolic BP, pulse, category | `healthdata/{user}/blood_pressure` |
| **🫁 OxySmart Pulse Oximeter** | Respiratory health | SpO2 %, heart rate, signal quality | `healthdata/{user}/pulse_oximetry` |

---

**Remember**: The goal is to maintain identical health data processing across all platforms while preserving the MQTT-based export functionality that makes this fork unique. We now support **4 different health device types** with complete BLE integration and standardized MQTT output.