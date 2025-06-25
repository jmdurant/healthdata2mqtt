#!/usr/bin/python3

import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime

class MQTTHealthDataPublisher:
    def __init__(self, broker_host='localhost', broker_port=1883, username=None, password=None):
        """Initialize MQTT publisher for health data."""
        self.broker_host = broker_host
        self.broker_port = broker_port
        self.username = username
        self.password = password
        
        # Use the new callback API version for paho-mqtt 2.x
        try:
            self.client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION1)
        except AttributeError:
            # Fallback for older versions
            self.client = mqtt.Client()
        
        if username and password:
            self.client.username_pw_set(username, password)
        
        self.client.on_connect = self._on_connect
        self.client.on_publish = self._on_publish
        
    def _on_connect(self, client, userdata, flags, rc):
        """Callback for when the client connects to the broker."""
        if rc == 0:
            print("MQTT * Connected successfully to broker")
        else:
            print(f"MQTT * Failed to connect, return code {rc}")
    
    def _on_publish(self, client, userdata, mid):
        """Callback for when a message is published."""
        print(f"MQTT * Message {mid} published successfully")
    
    def connect(self):
        """Connect to MQTT broker."""
        try:
            self.client.connect(self.broker_host, self.broker_port, 60)
            self.client.loop_start()
            time.sleep(1)  # Give it time to connect
            return True
        except Exception as e:
            print(f"MQTT * Connection error: {e}")
            return False
    
    def disconnect(self):
        """Disconnect from MQTT broker."""
        self.client.loop_stop()
        self.client.disconnect()
    
    def publish_body_composition(self, user_email, timestamp, data):
        """Publish body composition data to MQTT.
        
        Args:
            user_email: User identifier
            timestamp: Unix timestamp or datetime object
            data: Dictionary containing body composition metrics
        """
        if isinstance(timestamp, int):
            timestamp_str = datetime.fromtimestamp(timestamp).isoformat()
        else:
            timestamp_str = timestamp
        
        # Prepare MQTT payload
        payload = {
            "user": user_email,
            "timestamp": timestamp_str,
            "type": "body_composition",
            "data": data
        }
        
        # Publish to topic: health/body_composition/{user_email}
        topic = f"health/body_composition/{user_email.replace('@', '_at_').replace('.', '_')}"
        
        try:
            result = self.client.publish(topic, json.dumps(payload), qos=1, retain=True)
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"MQTT * Published body composition data to {topic}")
                return True
            else:
                print(f"MQTT * Failed to publish to {topic}, error code: {result.rc}")
                return False
        except Exception as e:
            print(f"MQTT * Publish error: {e}")
            return False
    
    def publish_blood_pressure(self, user_email, timestamp, systolic, diastolic, pulse, category=None, mov=None, ihb=None):
        """Publish blood pressure data to MQTT.
        
        Args:
            user_email: User identifier
            timestamp: Unix timestamp or datetime object
            systolic: Systolic blood pressure
            diastolic: Diastolic blood pressure
            pulse: Heart rate
            category: Blood pressure category (optional)
            mov: Movement detection (optional)
            ihb: Irregular heartbeat detection (optional)
        """
        if isinstance(timestamp, int):
            timestamp_str = datetime.fromtimestamp(timestamp).isoformat()
        else:
            timestamp_str = timestamp
        
        # Prepare MQTT payload
        payload = {
            "user": user_email,
            "timestamp": timestamp_str,
            "type": "blood_pressure",
            "data": {
                "systolic": systolic,
                "diastolic": diastolic,
                "pulse": pulse
            }
        }
        
        # Add optional fields if provided
        if category:
            payload["data"]["category"] = category
        if mov is not None:
            payload["data"]["movement_detected"] = bool(mov)
        if ihb is not None:
            payload["data"]["irregular_heartbeat"] = bool(ihb)
        
        # Publish to topic: health/blood_pressure/{user_email}
        topic = f"health/blood_pressure/{user_email.replace('@', '_at_').replace('.', '_')}"
        
        try:
            result = self.client.publish(topic, json.dumps(payload), qos=1, retain=True)
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"MQTT * Published blood pressure data to {topic}")
                return True
            else:
                print(f"MQTT * Failed to publish to {topic}, error code: {result.rc}")
                return False
        except Exception as e:
            print(f"MQTT * Publish error: {e}")
            return False
    
    def publish_raw_scale_data(self, device_mac, timestamp, weight, impedance, battery_v=None, battery_percent=None):
        """Publish raw scale data to MQTT (for debugging/monitoring).
        
        Args:
            device_mac: Device MAC address
            timestamp: Unix timestamp
            weight: Weight in kg
            impedance: Impedance value
            battery_v: Battery voltage (optional)
            battery_percent: Battery percentage (optional)
        """
        # Prepare MQTT payload
        payload = {
            "device": device_mac,
            "timestamp": datetime.fromtimestamp(timestamp).isoformat(),
            "type": "raw_scale_data",
            "data": {
                "weight": weight,
                "impedance": impedance
            }
        }
        
        # Add battery info if available
        if battery_v is not None:
            payload["data"]["battery_voltage"] = battery_v
        if battery_percent is not None:
            payload["data"]["battery_percent"] = battery_percent
        
        # Publish to topic: health/raw/{device_mac}
        topic = f"health/raw/{device_mac.replace(':', '')}"
        
        try:
            result = self.client.publish(topic, json.dumps(payload), qos=0)
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"MQTT * Published raw scale data to {topic}")
                return True
            else:
                print(f"MQTT * Failed to publish to {topic}, error code: {result.rc}")
                return False
        except Exception as e:
            print(f"MQTT * Publish error: {e}")
            return False