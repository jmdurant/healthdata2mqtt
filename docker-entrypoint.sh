#!/bin/bash

# Update MQTT config from environment variables if provided
if [ ! -f /app/user/export2mqtt.cfg ]; then
    echo "No config file found, copying template..."
    cp /app/user/export2mqtt.cfg.template /app/user/export2mqtt.cfg
fi

# Update MQTT host if provided via environment
if [ -n "$MQTT_HOST" ]; then
    sed -i "s/mqtt_host=.*/mqtt_host=$MQTT_HOST/" /app/user/export2mqtt.cfg
fi

if [ -n "$MQTT_PORT" ]; then
    sed -i "s/mqtt_port=.*/mqtt_port=$MQTT_PORT/" /app/user/export2mqtt.cfg
fi

if [ -n "$MQTT_USERNAME" ]; then
    sed -i "s/mqtt_username=.*/mqtt_username=$MQTT_USERNAME/" /app/user/export2mqtt.cfg
fi

if [ -n "$MQTT_PASSWORD" ]; then
    sed -i "s/mqtt_password=.*/mqtt_password=$MQTT_PASSWORD/" /app/user/export2mqtt.cfg
fi

# Start bluetooth service (if running privileged)
if [ -f /etc/init.d/bluetooth ]; then
    sudo /etc/init.d/bluetooth start 2>/dev/null || true
fi

# Wait for bluetooth to be ready
sleep 5

# Check if BLE adapter is available
hciconfig 2>/dev/null || {
    echo "Warning: No Bluetooth adapter found. BLE functionality will not work."
    echo "Make sure the container is running with --privileged flag and Bluetooth is available on host."
}

# Execute the main script
exec "$@"