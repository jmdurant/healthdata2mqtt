# 🪟 Windows Bluetooth & Docker Guide

## ❌ **The Challenge: Docker + Windows Bluetooth**

**Unfortunately, Docker containers on Windows CANNOT directly access Windows Bluetooth hardware.** This is a fundamental limitation of how Docker works on Windows.

### **Why This Happens:**
- 🐧 **Linux containers** - Our health data collector runs in a Linux container
- 🪟 **Windows host** - Your Bluetooth hardware is managed by Windows
- 🚫 **No bridge** - Docker Desktop doesn't provide Bluetooth passthrough
- 🔌 **USB/Hardware isolation** - Containers can't access host Bluetooth stack

## 🔧 **Solutions for Windows Users**

### **🏆 Option 1: Hybrid Deployment (Recommended)**

Run the data collector **locally on Windows** and the API **in Docker**:

```
Windows Host:
├── 📡 MQTT Broker (Docker)
├── 🌐 Health Data API (Docker)  
└── 📱 Health Data Collector (Native Python)
```

**Setup:**
```powershell
# 1. Install Python dependencies on Windows
pip install paho-mqtt bluepy-windows

# 2. Run MQTT & API in Docker
docker-compose up mosquitto health-api

# 3. Run data collector natively
python import_data_mqtt.py
```

### **🖥️ Option 2: WSL2 with USB Passthrough**

Use WSL2 (Windows Subsystem for Linux) with USB device passthrough:

**Requirements:**
- Windows 11 or Windows 10 with recent updates
- WSL2 enabled
- USB passthrough support

**Setup:**
```powershell
# Install WSL2 with Ubuntu
wsl --install Ubuntu

# In WSL2, clone the project
git clone <your-repo>
cd healthdata2mqtt

# Run normally in WSL2
./import_data_mqtt.sh
```

**USB Bluetooth Adapter Passthrough:**
```powershell
# List USB devices
usbipd wsl list

# Attach Bluetooth adapter to WSL2
usbipd wsl attach --distribution Ubuntu --busid <bus-id>
```

### **🔌 Option 3: External Bluetooth Bridge**

Use a dedicated device (Raspberry Pi, ESP32) as a Bluetooth bridge:

```
Bluetooth Devices → RPi/ESP32 → MQTT → Docker Containers
```

**ESP32 Bridge Example:**
- ESP32 scans for BLE devices
- Publishes raw data to MQTT
- Docker containers consume MQTT data

### **🌐 Option 4: Windows Native + MQTT**

Run everything natively on Windows:

**Install Requirements:**
```powershell
# Install Python
winget install Python.Python.3

# Install dependencies
pip install paho-mqtt flask flask-cors

# Install Windows Bluetooth libraries
pip install bleak  # Windows BLE library
```

**Run Services:**
```powershell
# Terminal 1: MQTT Broker
docker run -p 1883:1883 eclipse-mosquitto

# Terminal 2: Health Data API
python mqtt-listener/health_data_api.py

# Terminal 3: Data Collector (modified for Windows)
python windows_health_collector.py
```

## 📊 **Solution Comparison**

| **Solution** | **Complexity** | **Bluetooth Access** | **Docker Benefits** | **Best For** |
|-------------|---------------|---------------------|-------------------|--------------|
| **Hybrid** | 🟡 Medium | ✅ Full | 🟡 Partial | **Most users** |
| **WSL2** | 🔴 High | ✅ Full | ✅ Full | Power users |
| **Bridge Device** | 🔴 High | ✅ Full | ✅ Full | IoT enthusiasts |
| **Native Windows** | 🟢 Low | ✅ Full | ❌ None | Simple setups |

## 🚀 **Recommended Setup for Windows**

### **Step 1: Hybrid Architecture**

```yaml
# docker-compose-windows.yml
version: '3.8'
services:
  mosquitto:
    image: eclipse-mosquitto:2
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto/config:/mosquitto/config

  health-api:
    build: ./mqtt-listener
    ports:
      - "5001:5001"
    environment:
      - MQTT_HOST=host.docker.internal  # Points to Windows host
    depends_on:
      - mosquitto
```

### **Step 2: Windows Data Collector**

Create a Windows-compatible version:

```python
# windows_health_collector.py
import asyncio
from bleak import BleakScanner, BleakClient
from mqtt_publisher import MQTTHealthDataPublisher

async def scan_for_devices():
    """Scan for health devices using Windows BLE"""
    devices = await BleakScanner.discover()
    for device in devices:
        if device.name and ("mi" in device.name.lower() or "omron" in device.name.lower()):
            print(f"Found device: {device.name} - {device.address}")
            # Process device data
```

### **Step 3: Deployment**

```powershell
# Start Docker services
docker-compose -f docker-compose-windows.yml up -d

# Install Windows Python dependencies
pip install bleak paho-mqtt

# Run Windows data collector
python windows_health_collector.py
```

## 📱 **Alternative: ESP32 Bluetooth Bridge**

For a completely Docker-based solution, use an ESP32 as a Bluetooth bridge:

### **ESP32 Code (Arduino):**
```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <BLEDevice.h>

// Scan for health devices
// Publish data to MQTT
// Bridge between BLE and WiFi
```

### **Benefits:**
- ✅ **Dedicated hardware** for Bluetooth scanning
- ✅ **24/7 operation** independent of PC
- ✅ **Low power** consumption
- ✅ **Full Docker** deployment on Windows

## 🧪 **Testing Your Setup**

### **Test MQTT Communication:**
```powershell
# Test from Windows to Docker
docker exec <mosquitto-container> mosquitto_sub -t "health/#"

# Test publishing from Windows
python -c "
from mqtt_publisher import MQTTHealthDataPublisher
pub = MQTTHealthDataPublisher('localhost', 1883)
pub.connect()
pub.publish_body_composition('test@example.com', 1234567890, {'weight': 75.0})
"
```

### **Verify API Access:**
```powershell
# Test API endpoint
curl http://localhost:5001/health
curl http://localhost:5001/users
```

## 🎯 **Production Recommendations**

### **For Home Users:**
1. **Use Hybrid approach** (Docker API + Windows collector)
2. **Run on dedicated PC** or always-on device
3. **Consider ESP32 bridge** for reliability

### **For Developers:**
1. **Use WSL2** for full Linux compatibility
2. **USB passthrough** for real hardware testing
3. **Docker development** environment

### **For IoT Enthusiasts:**
1. **ESP32 Bluetooth bridge** for scalability
2. **Multiple device support** 
3. **Mesh networking** capabilities

## 🔧 **Quick Start Command**

```powershell
# Clone and setup for Windows hybrid deployment
git clone <repo>
cd healthdata2mqtt

# Start Docker services (API + MQTT)
docker-compose -f docker-compose-windows.yml up -d

# Install Windows dependencies
pip install -r requirements-windows.txt

# Configure and run
copy user\export2mqtt.cfg.template user\export2mqtt.cfg
# Edit user\export2mqtt.cfg with your device MAC addresses
python windows_health_collector.py
```

The **hybrid approach** is your best bet for Windows - Docker handles the API/storage while Windows handles Bluetooth! 🚀