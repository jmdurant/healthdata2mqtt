#!/usr/bin/python3

from flask import Flask, jsonify, request
from flask_cors import CORS
import threading
import json
import time
import os
from datetime import datetime, timedelta
import paho.mqtt.client as mqtt
from typing import Dict, Optional, Any

app = Flask(__name__)
CORS(app)  # Enable CORS for web integrations

# Storage for health data (in production, use a database)
health_data_store = {
    'body_composition': {},  # {user_email: latest_data}
    'blood_pressure': {},    # {user_email: latest_data}
    'raw': {}               # {device_mac: latest_data}
}

# Configuration - Read from environment variables
MQTT_HOST = os.getenv('MQTT_HOST', 'mosquitto')
MQTT_PORT = int(os.getenv('MQTT_PORT', '1883'))
MQTT_USERNAME = os.getenv('MQTT_USERNAME')
MQTT_PASSWORD = os.getenv('MQTT_PASSWORD')

# Log configuration on startup
print(f"🔧 MQTT Configuration:")
print(f"   Host: {MQTT_HOST}")
print(f"   Port: {MQTT_PORT}")
print(f"   Username: {MQTT_USERNAME if MQTT_USERNAME else 'Anonymous'}")
print(f"   Password: {'***' if MQTT_PASSWORD else 'None'}")

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✓ Connected to MQTT broker")
        # Subscribe to all health topics
        client.subscribe("health/+/+")
        client.subscribe("health/raw/+")
        print("✓ Subscribed to health topics")
    else:
        print(f"✗ Failed to connect to MQTT broker, code: {rc}")

def on_message(client, userdata, msg):
    try:
        topic_parts = msg.topic.split('/')
        
        if len(topic_parts) >= 3 and topic_parts[0] == 'health':
            data = json.loads(msg.payload.decode())
            
            if topic_parts[1] == 'body_composition':
                user_email = data.get('user')
                health_data_store['body_composition'][user_email] = {
                    'timestamp': data.get('timestamp'),
                    'received_at': datetime.now().isoformat(),
                    'data': data.get('data', {})
                }
                print(f"📊 Body composition data for {user_email}")
                
            elif topic_parts[1] == 'blood_pressure':
                user_email = data.get('user')
                health_data_store['blood_pressure'][user_email] = {
                    'timestamp': data.get('timestamp'),
                    'received_at': datetime.now().isoformat(),
                    'data': data.get('data', {})
                }
                print(f"🩺 Blood pressure data for {user_email}")
                
            elif topic_parts[1] == 'raw':
                device_mac = data.get('device', topic_parts[2])
                health_data_store['raw'][device_mac] = {
                    'timestamp': data.get('timestamp'),
                    'received_at': datetime.now().isoformat(),
                    'data': data.get('data', {})
                }
                print(f"📱 Raw device data from {device_mac}")
                
    except Exception as e:
        print(f"Error processing message from {msg.topic}: {e}")

def mqtt_worker():
    """Background thread to handle MQTT connection"""
    global MQTT_HOST, MQTT_PORT, MQTT_USERNAME, MQTT_PASSWORD
    
    try:
        # Use callback API version for newer paho-mqtt
        try:
            client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION1)
        except AttributeError:
            client = mqtt.Client()
            
        if MQTT_USERNAME and MQTT_PASSWORD:
            client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
            
        client.on_connect = on_connect
        client.on_message = on_message
        
        client.connect(MQTT_HOST, MQTT_PORT, 60)
        client.loop_forever()
        
    except Exception as e:
        print(f"MQTT worker error: {e}")

# Start MQTT client in background
threading.Thread(target=mqtt_worker, daemon=True).start()

# API Routes

@app.route("/")
def index():
    """API documentation"""
    return jsonify({
        "name": "Health Data API",
        "version": "1.0",
        "description": "REST API for accessing health data from MQTT",
        "endpoints": {
            "/health": "Get health status",
            "/users": "List all users with data",
            "/user/<email>/latest": "Get latest data for user",
            "/user/<email>/body_composition": "Get body composition data",
            "/user/<email>/blood_pressure": "Get blood pressure data",
            "/weight/<email>": "Get just weight (OpenEMR compatible)",
            "/devices": "List raw device data"
        }
    })

@app.route("/health")
def health_check():
    """Health check endpoint"""
    data_count = (
        len(health_data_store['body_composition']) +
        len(health_data_store['blood_pressure']) +
        len(health_data_store['raw'])
    )
    
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "data_points": data_count,
        "users": list(set(
            list(health_data_store['body_composition'].keys()) +
            list(health_data_store['blood_pressure'].keys())
        ))
    })

@app.route("/users")
def list_users():
    """List all users with available data"""
    users = {}
    
    for email in health_data_store['body_composition']:
        users[email] = users.get(email, {})
        users[email]['body_composition'] = health_data_store['body_composition'][email]['received_at']
    
    for email in health_data_store['blood_pressure']:
        users[email] = users.get(email, {})
        users[email]['blood_pressure'] = health_data_store['blood_pressure'][email]['received_at']
    
    return jsonify(users)

@app.route("/user/<email>/latest")
def get_user_latest(email):
    """Get all latest data for a user"""
    result = {"user": email}
    
    if email in health_data_store['body_composition']:
        result['body_composition'] = health_data_store['body_composition'][email]
    
    if email in health_data_store['blood_pressure']:
        result['blood_pressure'] = health_data_store['blood_pressure'][email]
    
    if not result.get('body_composition') and not result.get('blood_pressure'):
        return jsonify({"error": "No data found for user"}), 404
    
    return jsonify(result)

@app.route("/user/<email>/body_composition")
def get_body_composition(email):
    """Get body composition data for user"""
    if email not in health_data_store['body_composition']:
        return jsonify({"error": "No body composition data found"}), 404
    
    return jsonify(health_data_store['body_composition'][email])

@app.route("/user/<email>/blood_pressure")
def get_blood_pressure(email):
    """Get blood pressure data for user"""
    if email not in health_data_store['blood_pressure']:
        return jsonify({"error": "No blood pressure data found"}), 404
    
    return jsonify(health_data_store['blood_pressure'][email])

@app.route("/weight/<email>")
def get_weight_only(email):
    """Get just weight data (compatible with your OpenEMR snippet)"""
    if email in health_data_store['body_composition']:
        weight = health_data_store['body_composition'][email]['data'].get('weight')
        if weight:
            return jsonify({
                "weight": weight,
                "timestamp": health_data_store['body_composition'][email]['timestamp'],
                "user": email
            })
    
    return jsonify({"error": "No weight data found"}), 404

@app.route("/devices")
def list_devices():
    """List raw device data"""
    return jsonify(health_data_store['raw'])

@app.route("/device/<device_mac>")
def get_device_data(device_mac):
    """Get data for specific device"""
    if device_mac not in health_data_store['raw']:
        return jsonify({"error": "No data found for device"}), 404
    
    return jsonify(health_data_store['raw'][device_mac])

if __name__ == "__main__":
    print("🏥 Health Data API Starting...")
    print("📡 Connecting to MQTT broker...")
    app.run(host="0.0.0.0", port=5001, debug=False)