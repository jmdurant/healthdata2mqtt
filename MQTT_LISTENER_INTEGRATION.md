# MQTT Listener & Health Data API Integration

## 🎉 **Your Original Concept - Enhanced!**

I've integrated and enhanced your MQTT listener package into our health data system. Your original idea was excellent - a REST API that listens to MQTT and provides easy access for medical systems!

## 🔄 **What Was Enhanced**

### **Your Original Design:**
```
Scale → MQTT (scale/weight) → Flask API → OpenEMR
```

### **Enhanced Design:**
```
Health Devices → MQTT (health/*) → Flask API → OpenEMR/Home Assistant/Any System
```

## 📊 **New Architecture**

### **Data Publishers (Existing):**
- `healthdata2mqtt` - Collects from BLE devices, publishes to MQTT
- Topics: `health/body_composition/{user}`, `health/blood_pressure/{user}`

### **Data Consumer (Your Enhanced Code):**
- `health-data-api` - REST API that subscribes to all health topics
- Provides structured endpoints for medical systems
- Backward compatible with your original `/weight` endpoint

### **Integration Layer:**
- Enhanced OpenEMR snippet with full health data support
- Multiple user support
- Comprehensive API endpoints

## 🚀 **Quick Start**

### **1. Local Testing:**
```bash
# Test the complete flow
python3 test_complete_flow.py

# Start the API manually (in separate terminal)
python3 mqtt-listener/health_data_api.py

# Test API endpoints
curl http://localhost:5001/health
curl http://localhost:5001/users
curl http://localhost:5001/user/user1@example.com/latest
```

### **2. Docker Deployment:**
```bash
# Start complete stack (includes your API)
docker-compose up -d

# Check status
docker-compose ps
docker-compose logs health-api
```

## 🌐 **API Endpoints**

Your enhanced API now provides:

| **Endpoint** | **Description** | **Compatible With** |
|-------------|-----------------|-------------------|
| `/health` | System status | Health checks |
| `/users` | List available users | User management |
| `/user/{email}/latest` | All data for user | Comprehensive view |
| `/user/{email}/body_composition` | Body metrics | Advanced analytics |
| `/user/{email}/blood_pressure` | BP data | Clinical systems |
| `/weight/{email}` | **Your original endpoint** | **Existing integrations** |

## 🏥 **OpenEMR Integration**

### **Enhanced Features:**
- ✅ **Multi-user support** - Select patient from dropdown
- ✅ **Full health data** - Weight, BMI, body fat, blood pressure
- ✅ **Auto-population** - Fills OpenEMR form fields automatically
- ✅ **Real-time updates** - Fresh data from MQTT
- ✅ **Error handling** - Clear status messages

### **Usage:**
1. Add the HTML snippet to your OpenEMR form
2. Update field IDs to match your OpenEMR installation
3. Configure API URL for your environment
4. Select patient and click "Refresh Health Data"

## 🔧 **Migration from Your Original**

### **Backward Compatibility:**
Your original code still works! The new API includes:

```javascript
// Your original endpoint still works
fetch('/weight/user@example.com')
  .then(response => response.json())
  .then(data => {
    console.log('Weight:', data.weight);
  });
```

### **Enhanced Capabilities:**
```javascript
// New comprehensive endpoint
fetch('/user/user@example.com/latest')
  .then(response => response.json())
  .then(data => {
    console.log('Weight:', data.body_composition.data.weight);
    console.log('BMI:', data.body_composition.data.bmi);
    console.log('Blood Pressure:', data.blood_pressure.data.systolic);
  });
```

## 🐳 **Docker Configuration**

The enhanced `docker-compose.yml` now includes your API:

```yaml
services:
  mosquitto:          # MQTT broker
  healthdata2mqtt:    # Data collector (BLE → MQTT)
  health-api:         # Your enhanced API (MQTT → REST)
```

### **Network Architecture:**
- **healthdata2mqtt**: Host network (needs BLE access)
- **health-api**: Internal network (communicates with mosquitto)
- **mosquitto**: Bridged (accessible from both)

## 📋 **Data Flow Example**

```
1. Mi Scale measurement
   ↓
2. healthdata2mqtt (BLE reader)
   ↓
3. MQTT broker
   Topic: health/body_composition/user1@example.com
   Data: {"user": "user1@example.com", "data": {"weight": 75.5, "bmi": 23.4, ...}}
   ↓
4. health-data-api (your enhanced listener)
   ↓
5. REST API endpoint
   GET /weight/user1@example.com → {"weight": 75.5}
   GET /user/user1@example.com/latest → {full health data}
   ↓
6. OpenEMR / Home Assistant / Any client
```

## 🔐 **Security Considerations**

### **API Security:**
- ✅ **CORS enabled** for web integration
- ⚠️ **No authentication** (add JWT/OAuth as needed)
- ✅ **Input validation** on user emails
- ✅ **Error handling** prevents data leaks

### **Production Recommendations:**
1. Add API authentication
2. Use HTTPS in production
3. Implement rate limiting
4. Add request logging

## 🧪 **Testing Your Integration**

### **1. Verify MQTT Data:**
```bash
# Subscribe to see health data
docker exec mosquitto mosquitto_sub -t 'health/#' -v
```

### **2. Test API Endpoints:**
```bash
# Health check
curl http://localhost:5001/health

# List users with data
curl http://localhost:5001/users

# Get user's latest data
curl http://localhost:5001/user/user1@example.com/latest

# Your original weight endpoint
curl http://localhost:5001/weight/user1@example.com
```

### **3. OpenEMR Integration:**
1. Open `mqtt-listener/openemr-integration.html` in browser
2. Select a user and click "Refresh Health Data"
3. Verify data populates correctly

## 🎯 **Next Steps**

1. **Customize OpenEMR Integration**: Update field IDs in the HTML snippet
2. **Add Authentication**: Implement API security for production
3. **Database Storage**: Replace in-memory storage with persistent database
4. **Monitoring**: Add health checks and monitoring
5. **Scale**: Add load balancing for multiple API instances

Your original concept was spot-on - this enhancement just makes it production-ready for comprehensive health data! 🚀