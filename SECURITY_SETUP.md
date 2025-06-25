# Security Configuration Guide

## 🔐 Current Security Status

**Default Setup**: Open for local testing
- ✅ **Local Network Only**: MQTT broker only listens on localhost
- ❌ **No Authentication**: Anonymous access allowed
- ❌ **No Encryption**: Plain text communication

## 🛡️ Securing Your MQTT Broker

### Option 1: Username/Password Authentication

1. **Create password file:**
```bash
# Create mosquitto password file
docker exec test-mosquitto mosquitto_passwd -c /mosquitto/config/passwd healthuser

# Or locally if you have mosquitto installed
mosquitto_passwd -c mosquitto/config/passwd healthuser
```

2. **Update mosquitto.conf:**
```conf
# Security
allow_anonymous false
password_file /mosquitto/config/passwd
```

3. **Update your health data config:**
```ini
# In user/export2mqtt.cfg
mqtt_username=healthuser
mqtt_password=your_secure_password
```

### Option 2: TLS/SSL Encryption

1. **Generate certificates:**
```bash
# Create CA and server certificates
openssl genrsa -out mosquitto/certs/ca.key 2048
openssl req -new -x509 -days 365 -key mosquitto/certs/ca.key -out mosquitto/certs/ca.crt
openssl genrsa -out mosquitto/certs/server.key 2048
openssl req -new -key mosquitto/certs/server.key -out mosquitto/certs/server.csr
openssl x509 -req -in mosquitto/certs/server.csr -CA mosquitto/certs/ca.crt -CAkey mosquitto/certs/ca.key -CAcreateserial -out mosquitto/certs/server.crt -days 365
```

2. **Update mosquitto.conf:**
```conf
# TLS Settings
listener 8883
protocol mqtt
cafile /mosquitto/certs/ca.crt
certfile /mosquitto/certs/server.crt
keyfile /mosquitto/certs/server.key
tls_version tlsv1.2
```

3. **Update application config:**
```ini
mqtt_host=localhost
mqtt_port=8883
mqtt_use_tls=true
```

### Option 3: Network Isolation

1. **Docker Network Security:**
```yaml
# In docker-compose.yml
services:
  mosquitto:
    networks:
      - health-internal
    # Remove public port mapping for internal-only access
    # ports:
    #   - "1883:1883"

networks:
  health-internal:
    driver: bridge
    internal: true  # No external access
```

2. **Firewall Rules (if running locally):**
```bash
# Block external access to MQTT port
sudo ufw deny 1883
sudo ufw allow from 192.168.1.0/24 to any port 1883  # Only local network
```

## 🏠 **Recommended Security for Home Use**

### **Level 1: Basic (Current Setup)**
- ✅ Local network only
- ✅ No internet exposure
- ❌ No authentication

**Good for**: Isolated home networks, testing

### **Level 2: Authenticated (Recommended)**
```bash
# Quick setup script
./setup_mqtt_auth.sh
```

- ✅ Username/password authentication
- ✅ Local network only
- ✅ Encrypted credentials

**Good for**: Home automation with multiple devices

### **Level 3: Encrypted (Paranoid)**
- ✅ TLS encryption
- ✅ Username/password authentication  
- ✅ Certificate validation
- ✅ Network isolation

**Good for**: Sensitive health data, shared networks

## 🔧 **Quick Security Setup Script**

I'll create a script to easily secure your installation: