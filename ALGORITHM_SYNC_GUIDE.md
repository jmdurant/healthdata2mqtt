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

### **BLE Communication (Android-Specific)**
| Python File | Android Kotlin File | Purpose |
|-------------|---------------------|---------|
| `miscale/miscale_ble.py` | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | BLE device scanning logic |
| `omron/omblepy.py` | `android/app/src/main/java/com/healthdata/mqtt/service/BleHealthScannerService.kt` | Omron device communication |

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
- ✅ **BP Categorization**: Medical standard classifications
- ✅ **Reference Tables**: Age/gender-specific ranges

### **Medium Impact - Review & Sync**
- ⚠️ **Device Detection**: BLE scanning improvements
- ⚠️ **Data Parsing**: Raw sensor data interpretation
- ⚠️ **Unit Conversions**: lbs/kg, metric/imperial

### **Low Impact - Optional Sync**
- 🔍 **Error Handling**: Non-algorithm improvements
- 🔍 **Logging**: Debug/status messages
- 🔍 **Configuration**: Export-specific settings

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
- [ ] MQTT data format remains consistent
- [ ] Docker build completes successfully
- [ ] Android app compiles without errors

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
- ✅ **Regularly**: Reference table updates (BMI scales, BP categories)
- ✅ **As Needed**: Device compatibility improvements
- ❌ **Never**: Export destination changes (Garmin → MQTT is our fork)

---

**Remember**: The goal is to maintain identical health data processing across all platforms while preserving the MQTT-based export functionality that makes this fork unique.