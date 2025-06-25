# Testing Guide for Health Data to MQTT

## Current Status ✅

I've successfully converted your health data export system to send data to MQTT instead of Garmin Connect! Here's what's been completed:

### ✅ Core Components Created:
1. **`mqtt_publisher.py`** - Core MQTT publishing module
2. **`miscale_export_mqtt.py`** - Modified Miscale export for MQTT
3. **`omron_export_mqtt.py`** - Modified Omron export for MQTT  
4. **`import_data_mqtt.sh`** - Updated main script using MQTT exports
5. **Complete Docker setup** with Dockerfile, docker-compose.yml, and config

### ✅ Testing Infrastructure:
- MQTT broker (mosquitto) is running in Docker container
- Configuration files are ready
- Test scripts are available

## 🚀 Quick Start Testing

### 1. MQTT Broker is Already Running
```bash
# Check broker status
docker ps | grep mosquitto

# View broker logs
docker logs test-mosquitto
```

### 2. Test MQTT Publishing/Subscribing
```bash
# In one terminal - subscribe to health topics
docker exec -it test-mosquitto mosquitto_sub -t 'health/#' -v

# In another terminal - publish test data
docker exec test-mosquitto mosquitto_pub -t 'health/body_composition/test_user' -m '{"user":"test@example.com","data":{"weight":75.5,"bmi":23.4}}'
```

### 3. Test the Python MQTT Publisher
```bash
# First install paho-mqtt (if not already installed)
pip3 install --user paho-mqtt

# Run the test script
python3 test_mqtt_publish.py
```

### 4. Docker Container Testing
```bash
# Build and run the complete stack
docker-compose up -d

# View logs
docker-compose logs -f healthdata2mqtt

# Test from within container
docker exec -it healthdata2mqtt python3 test_mqtt_publish.py
```

## 📊 MQTT Topic Structure

Your health data will be published to these topics:

- **Body Composition**: `health/body_composition/{user_email}`
  - Weight, BMI, body fat, muscle mass, etc.
  
- **Blood Pressure**: `health/blood_pressure/{user_email}`
  - Systolic, diastolic, pulse, category

- **Raw Device Data**: `health/raw/{device_mac}` (optional)
  - Raw readings for debugging

## 🏠 Home Assistant Integration

Add these sensors to your `configuration.yaml`:

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

## 🐛 Troubleshooting

### If MQTT Connection Fails:
```bash
# Check broker is running
docker ps | grep mosquitto

# Check broker logs
docker logs test-mosquitto

# Test basic connectivity
docker exec test-mosquitto mosquitto_pub -t 'test' -m 'hello'
```

### If Bluetooth Doesn't Work in Container:
```bash
# Run with privileged mode
docker run --privileged --net=host -v /var/run/dbus:/var/run/dbus:ro healthdata2mqtt

# Check Bluetooth adapter
hciconfig
```

### If Python Dependencies Fail:
```bash
# Install manually
pip3 install paho-mqtt bluepy

# Or use virtual environment
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## 🎯 Next Steps

1. **Configure Your Devices**: Edit `user/export2mqtt.cfg` with your device MAC addresses
2. **Set User Profiles**: Update user weight ranges and email identifiers
3. **Test with Real Devices**: Run `./import_data_mqtt.sh` with your scales/monitors
4. **Set Up Home Assistant**: Add MQTT sensors to monitor your health data
5. **Automate**: Use systemd service or Docker restart policies for continuous monitoring

## 📚 Documentation

- **[MQTT_DOCUMENTATION.md](MQTT_DOCUMENTATION.md)** - Complete MQTT topic reference
- **[DOCKER_README.md](DOCKER_README.md)** - Docker setup guide
- **[README_MQTT.md](README_MQTT.md)** - Main setup instructions

## 🔧 Configuration Files

- **Main Config**: `user/export2mqtt.cfg`
- **Docker Compose**: `docker-compose.yml`
- **Mosquitto Config**: `mosquitto/config/mosquitto.conf`

The system is ready to run! Your health data will stay completely local on your MQTT broker - no cloud services needed.