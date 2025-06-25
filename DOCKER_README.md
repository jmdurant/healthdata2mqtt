# Docker Setup for Health Data to MQTT

This directory contains everything needed to run the health data export system in Docker containers.

## Quick Start

1. **Build and start the containers:**
   ```bash
   docker-compose up -d
   ```

2. **View logs:**
   ```bash
   docker-compose logs -f healthdata2mqtt
   ```

3. **Test MQTT publishing:**
   ```bash
   docker exec -it healthdata2mqtt python3 test_mqtt_publish.py
   ```

4. **Subscribe to health data topics:**
   ```bash
   docker exec -it mosquitto mosquitto_sub -t 'health/#' -v
   ```

## Configuration

### Environment Variables

You can configure the system using environment variables:

```yaml
# In docker-compose.yml
environment:
  - MQTT_HOST=localhost
  - MQTT_PORT=1883
  - MQTT_USERNAME=your_user
  - MQTT_PASSWORD=your_password
```

### Persistent Configuration

The `user/` directory is mounted as a volume for persistent configuration and data:

- `user/export2mqtt.cfg` - Main configuration file
- `user/miscale_backup.csv` - Body composition data backup
- `user/omron_backup.csv` - Blood pressure data backup

### Device Configuration

Edit `user/export2mqtt.cfg` to configure your devices:

```ini
# Enable your scale
switch_miscale=on

# Set device MAC address
ble_miscale_mac=12:34:56:78:90:AB

# Configure users
miscale_export_user1=("male", 180, "01-01-1990", "user1@example.com", 85, 70)
```

## Architecture

The setup includes two containers:

1. **mosquitto** - MQTT broker
   - Ports: 1883 (MQTT), 9001 (WebSocket)
   - Data persisted in `mosquitto/data/`

2. **healthdata2mqtt** - Health data collector
   - Runs in privileged mode for Bluetooth access
   - Connects to devices via BLE
   - Publishes data to MQTT broker

## Bluetooth Requirements

The container needs access to the host's Bluetooth adapter:

- Runs with `privileged: true`
- Mounts `/var/run/dbus` for D-Bus communication
- Maps USB devices for BLE adapters

## MQTT Topics

Health data is published to structured topics:

- `health/body_composition/{user_email}` - Weight, BMI, body fat, etc.
- `health/blood_pressure/{user_email}` - Blood pressure readings
- `health/raw/{device_mac}` - Raw device data (optional)

## Troubleshooting

### No Bluetooth Adapter
```bash
# Check if adapter is visible in container
docker exec -it healthdata2mqtt hciconfig
```

### MQTT Connection Issues
```bash
# Test MQTT broker
docker exec -it mosquitto mosquitto_sub -t 'test'
```

### View Application Logs
```bash
# Real-time logs
docker-compose logs -f healthdata2mqtt

# Check temp.log for detailed errors
docker exec -it healthdata2mqtt cat /dev/shm/temp.log
```

## Development

### Rebuild After Changes
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Run Interactive Shell
```bash
docker exec -it healthdata2mqtt /bin/bash
```

### Test Without Loop Mode
```bash
docker exec -it healthdata2mqtt ./import_data_mqtt.sh
```

## Home Assistant Integration

Add to your Home Assistant `configuration.yaml`:

```yaml
mqtt:
  broker: YOUR_DOCKER_HOST_IP
  port: 1883
  
  sensor:
    - name: "Weight"
      state_topic: "health/body_composition/user1_at_example_com"
      value_template: "{{ value_json.data.weight }}"
      unit_of_measurement: "kg"
      device_class: weight
```

## Security

For production:
1. Set `allow_anonymous false` in `mosquitto/config/mosquitto.conf`
2. Create password file: `mosquitto_passwd -c mosquitto/config/passwd username`
3. Update MQTT credentials in environment variables