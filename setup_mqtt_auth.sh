#!/bin/bash

echo "🔐 Setting up MQTT Authentication"
echo "================================="

# Check if mosquitto container is running
if ! docker ps | grep -q test-mosquitto; then
    echo "❌ Mosquitto container not running. Start with: docker run -d --name test-mosquitto -p 1883:1883 -v $(pwd)/mosquitto/config:/mosquitto/config eclipse-mosquitto:2"
    exit 1
fi

# Get username and password
read -p "Enter MQTT username: " mqtt_user
read -s -p "Enter MQTT password: " mqtt_pass
echo

# Create mosquitto config directory if it doesn't exist
mkdir -p mosquitto/config

# Create password file
echo "Creating password file..."
docker exec test-mosquitto mosquitto_passwd -c -b /mosquitto/config/passwd "$mqtt_user" "$mqtt_pass"

# Update mosquitto.conf to require authentication
echo "Updating mosquitto configuration..."
cat > mosquitto/config/mosquitto.conf << EOF
persistence true
persistence_location /mosquitto/data/

# Logging
log_dest file /mosquitto/log/mosquitto.log
log_dest stdout
log_type all

# Security - Authentication Required
allow_anonymous false
password_file /mosquitto/config/passwd

# Network
listener 1883
protocol mqtt

# WebSocket support (optional)
listener 9001
protocol websockets
EOF

# Update export2mqtt.cfg with credentials
echo "Updating health data configuration..."
if [ -f user/export2mqtt.cfg ]; then
    # Update existing config
    sed -i "s/mqtt_username=.*/mqtt_username=$mqtt_user/" user/export2mqtt.cfg
    sed -i "s/mqtt_password=.*/mqtt_password=$mqtt_pass/" user/export2mqtt.cfg
    echo "✅ Updated user/export2mqtt.cfg with new credentials"
else
    echo "⚠️  Configuration file user/export2mqtt.cfg not found"
    echo "   Please manually update with:"
    echo "   mqtt_username=$mqtt_user"
    echo "   mqtt_password=$mqtt_pass"
fi

# Create environment file for Docker
echo "Creating .env file for Docker..."
cat > .env << EOF
MQTT_USERNAME=$mqtt_user
MQTT_PASSWORD=$mqtt_pass
EOF
echo "✅ Created .env file for Docker Compose"

# Restart mosquitto to apply changes
echo "Restarting mosquitto container..."
docker restart test-mosquitto

echo ""
echo "🎉 MQTT Authentication Setup Complete!"
echo ""
echo "📋 Summary:"
echo "   • Username: $mqtt_user"
echo "   • Password: [hidden]"
echo "   • Anonymous access: DISABLED"
echo "   • Config updated: user/export2mqtt.cfg"
echo "   • Environment file: .env (for Docker)"
echo ""
echo "🧪 Test connection:"
echo "   docker exec test-mosquitto mosquitto_pub -u '$mqtt_user' -P '$mqtt_pass' -t 'test' -m 'authenticated'"
echo ""
echo "🐳 Docker deployment with authentication:"
echo "   docker-compose down"
echo "   docker-compose up -d"
echo "   # The .env file will automatically provide credentials to all services"
echo ""
echo "🔄 To remove authentication:"
echo "   • Set 'allow_anonymous true' in mosquitto/config/mosquitto.conf"
echo "   • Remove password_file line"
echo "   • Delete .env file"
echo "   • Restart containers"