# MQTT Health Data Export Documentation

This document describes the MQTT topic structure and data formats used by the healthdata2mqtt system.

## Overview

The system publishes health data from various devices (Mi Body Composition Scale, Omron Blood Pressure monitors) to an MQTT broker instead of uploading to Garmin Connect. This allows for local data storage and integration with home automation systems.

## MQTT Configuration

Configure your MQTT broker settings in `user/export2mqtt.cfg`:

```ini
mqtt_host=localhost
mqtt_port=1883
mqtt_username=your_username
mqtt_password=your_password
```

## Topic Structure

### Body Composition Data

**Topic Pattern:** `health/body_composition/{user_email}`

- `{user_email}` is sanitized by replacing `@` with `_at_` and `.` with `_`
- Example: `health/body_composition/john_at_example_com`

**Payload Format (JSON):**
```json
{
  "user": "john@example.com",
  "timestamp": "2024-01-15T10:30:00",
  "type": "body_composition",
  "data": {
    "weight": 75.5,
    "bmi": 23.4,
    "body_fat_percent": 18.5,
    "muscle_mass": 58.2,
    "bone_mass": 3.1,
    "water_percent": 55.3,
    "physique_rating": 5,
    "visceral_fat": 7,
    "metabolic_age": 28,
    "bmr": 1650,
    "lbm": 61.5,
    "ideal_weight": 72.5,
    "fat_mass_to_ideal": "Normal:3.0",
    "protein_percent": 17.8,
    "impedance": 500
  }
}
```

### Blood Pressure Data

**Topic Pattern:** `health/blood_pressure/{user_email}`

- `{user_email}` is sanitized by replacing `@` with `_at_` and `.` with `_`
- Example: `health/blood_pressure/john_at_example_com`

**Payload Format (JSON):**
```json
{
  "user": "john@example.com",
  "timestamp": "2024-01-15T10:35:00",
  "type": "blood_pressure",
  "data": {
    "systolic": 120,
    "diastolic": 80,
    "pulse": 65,
    "category": "Normal",
    "movement_detected": false,
    "irregular_heartbeat": false
  }
}
```

### Raw Scale Data (Optional)

**Topic Pattern:** `health/raw/{device_mac}`

- `{device_mac}` is the device MAC address with colons removed
- Example: `health/raw/1234567890AB`

**Payload Format (JSON):**
```json
{
  "device": "12:34:56:78:90:AB",
  "timestamp": "2024-01-15T10:30:00",
  "type": "raw_scale_data",
  "data": {
    "weight": 75.5,
    "impedance": 500,
    "battery_voltage": 3.1,
    "battery_percent": 85
  }
}
```

## Data Retention

All messages are published with:
- QoS 1 (at least once delivery) for body composition and blood pressure data
- Retain flag set to `true` to preserve the last reading
- QoS 0 (fire and forget) for raw scale data

## Integration Examples

### Home Assistant

Add the following MQTT sensors to your Home Assistant configuration:

```yaml
mqtt:
  sensor:
    - name: "John Weight"
      state_topic: "health/body_composition/john_at_example_com"
      value_template: "{{ value_json.data.weight }}"
      unit_of_measurement: "kg"
      device_class: weight
      
    - name: "John BMI"
      state_topic: "health/body_composition/john_at_example_com"
      value_template: "{{ value_json.data.bmi }}"
      
    - name: "John Blood Pressure"
      state_topic: "health/blood_pressure/john_at_example_com"
      value_template: "{{ value_json.data.systolic }}/{{ value_json.data.diastolic }}"
```

### Node-RED

Subscribe to topics using the MQTT input node:
- Topic: `health/+/+` (to receive all health data)
- Parse JSON payload in a function node

### Python Example

```python
import paho.mqtt.client as mqtt
import json

def on_message(client, userdata, message):
    payload = json.loads(message.payload.decode())
    print(f"Received {payload['type']} data for {payload['user']}")
    print(f"Data: {payload['data']}")

client = mqtt.Client()
client.on_message = on_message
client.connect("localhost", 1883)
client.subscribe("health/+/+")
client.loop_forever()
```

## Data Fields Description

### Body Composition Fields

- `weight`: Body weight in kilograms
- `bmi`: Body Mass Index
- `body_fat_percent`: Body fat percentage
- `muscle_mass`: Skeletal muscle mass in kilograms
- `bone_mass`: Bone mass in kilograms
- `water_percent`: Body water percentage
- `physique_rating`: Body type rating (1-9)
- `visceral_fat`: Visceral fat rating
- `metabolic_age`: Metabolic age in years
- `bmr`: Basal Metabolic Rate in kcal
- `lbm`: Lean Body Mass coefficient
- `ideal_weight`: Ideal weight in kilograms
- `fat_mass_to_ideal`: Fat mass compared to ideal
- `protein_percent`: Protein percentage
- `impedance`: Bioelectrical impedance value

### Blood Pressure Fields

- `systolic`: Systolic blood pressure in mmHg
- `diastolic`: Diastolic blood pressure in mmHg
- `pulse`: Heart rate in bpm
- `category`: Blood pressure category (Normal, High-Normal, Grade_1, Grade_2)
- `movement_detected`: Boolean indicating if movement was detected during measurement
- `irregular_heartbeat`: Boolean indicating if irregular heartbeat was detected

## Troubleshooting

1. **Connection Issues**: Check MQTT broker logs and ensure credentials are correct
2. **Missing Data**: Verify topics are correct and retained messages are enabled
3. **Permission Errors**: Ensure MQTT user has publish permissions for health/* topics