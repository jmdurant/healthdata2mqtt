# Health Data to MQTT Export

This is a modified version of the health data export system that sends data to a local MQTT broker instead of Garmin Connect.

## Overview

This system collects health data from:
- Mi Body Composition Scale 2
- Xiaomi Body Composition Scale S400
- Omron Blood Pressure Monitors

And publishes it to your local MQTT broker for integration with home automation systems.

## Prerequisites

1. **Python 3** with the following packages:
   ```bash
   pip3 install paho-mqtt
   pip3 install bluepy
   # Other existing dependencies from original project
   ```

2. **MQTT Broker** (e.g., Mosquitto):
   ```bash
   sudo apt-get install mosquitto mosquitto-clients
   ```

3. **Bluetooth LE adapter** configured and working

## Setup Instructions

### 1. Configure MQTT Settings

Copy the template configuration file:
```bash
cp user/export2mqtt.cfg.template user/export2mqtt.cfg
```

Edit `user/export2mqtt.cfg` and update:
- MQTT broker settings:
  ```ini
  mqtt_host=localhost
  mqtt_port=1883
  mqtt_username=your_username
  mqtt_password=your_password
  ```
- Device MAC addresses
- User profiles with weight ranges

### 2. Test MQTT Connection

Verify your MQTT broker is accessible:
```bash
mosquitto_sub -h localhost -p 1883 -t 'health/#' -v
```

### 3. Run the Data Collection

For one-time collection:
```bash
sudo ./import_data_mqtt.sh
```

For continuous monitoring (loop mode):
```bash
sudo ./import_data_mqtt.sh -l
```

## MQTT Topics and Data Format

### Body Composition Data
- **Topic**: `health/body_composition/{user_email}`
- **Example**: `health/body_composition/john_at_example_com`
- **Payload**: JSON with weight, BMI, body fat, muscle mass, etc.

### Blood Pressure Data
- **Topic**: `health/blood_pressure/{user_email}`
- **Example**: `health/blood_pressure/john_at_example_com`
- **Payload**: JSON with systolic, diastolic, pulse, category

See [MQTT_DOCUMENTATION.md](MQTT_DOCUMENTATION.md) for detailed format specifications.

## Home Assistant Integration

Add MQTT sensors to your `configuration.yaml`:

```yaml
mqtt:
  sensor:
    - name: "Weight"
      state_topic: "health/body_composition/user1_at_example_com"
      value_template: "{{ value_json.data.weight }}"
      unit_of_measurement: "kg"
      device_class: weight
      
    - name: "BMI"
      state_topic: "health/body_composition/user1_at_example_com"
      value_template: "{{ value_json.data.bmi }}"
      
    - name: "Blood Pressure"
      state_topic: "health/blood_pressure/user1_at_example_com"
      value_template: "{{ value_json.data.systolic }}/{{ value_json.data.diastolic }}"
```

## Troubleshooting

1. **No data published**: Check MQTT broker logs and credentials
2. **BLE connection issues**: Ensure adapter is up with `hciconfig`
3. **Missing data**: Verify device MAC addresses are correct
4. **Python errors**: Install missing dependencies with pip3

## Changes from Original

- Removed Garmin Connect authentication
- Replaced Garmin API calls with MQTT publishing
- Added `mqtt_publisher.py` module
- Created `miscale_export_mqtt.py` and `omron_export_mqtt.py`
- Modified `import_data_mqtt.sh` to use new export scripts
- Added comprehensive MQTT documentation

## Data Privacy

All data stays local on your MQTT broker. No cloud services are used.