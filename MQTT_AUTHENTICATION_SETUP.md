# MQTT Authentication Setup Guide

## 🚀 One Command Setup

```bash
# One command configures EVERYTHING
./setup_mqtt_auth.sh

# Enter your desired username/password
# Script automatically:
# ✅ Creates mosquitto password file
# ✅ Updates health data config
# ✅ Creates .env file for Docker
# ✅ Configures listener authentication
# ✅ Restarts containers with auth
```

## 🏗️ How It Works

### 1. Health Data Publisher reads from user/export2mqtt.cfg:
```ini
mqtt_username=healthuser
mqtt_password=your_secure_password
```

### 2. API Listener reads from environment variables:
```bash
MQTT_USERNAME=healthuser
MQTT_PASSWORD=your_secure_password
```

### 3. Docker Compose uses .env file:
```bash
MQTT_USERNAME=healthuser
MQTT_PASSWORD=your_secure_password
```

## 🧪 Test Authentication

### Test publisher authentication
```bash
python3 test_export_simple.py
```

### Test listener authentication  
```bash
docker logs health-data-api
# Should show: "✓ Connected to MQTT broker"
```

### Test manual connection
```bash
docker exec test-mosquitto mosquitto_pub -u 'healthuser' -P 'password' -t 'test' -m 'authenticated'
```

## 🔄 Zero-Config Default

- **Without setup**: Anonymous access (works immediately)
- **With setup**: Authenticated access (one command)
- **Production**: Add TLS for encryption

## 🎯 Security Levels

### Level 1: Anonymous (Default)
- No authentication required
- Works out of the box
- Suitable for local testing

### Level 2: Username/Password (Recommended)
- Run `./setup_mqtt_auth.sh`
- Secure MQTT communication
- Suitable for production LAN

### Level 3: TLS + Authentication (Enterprise)
- Add SSL certificates to mosquitto
- Enable port 8883 for encrypted MQTT
- Suitable for internet-facing deployments

Your authentication system is complete, automatic, and production-ready! 🎉