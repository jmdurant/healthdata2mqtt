#!/usr/bin/python3

import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from mqtt_publisher import MQTTHealthDataPublisher
import time

# Simple test without body metrics calculations
def test_simple_export():
    print("Testing simple health data export to MQTT...")
    
    # Initialize MQTT publisher
    mqtt_publisher = MQTTHealthDataPublisher(
        broker_host='localhost',
        broker_port=1883
    )
    
    # Connect to MQTT broker
    if mqtt_publisher.connect():
        print("✓ Connected to MQTT broker")
        
        # Sample body composition data (simulated)
        body_data = {
            "weight": 75.5,
            "bmi": 23.4,
            "body_fat_percent": 18.5,
            "muscle_mass": 58.2,
            "bone_mass": 3.1,
            "water_percent": 55.3,
            "physique_rating": 5,
            "visceral_fat": 7,
            "metabolic_age": 28,
            "bmr": 1650,
            "lbm": 61.5,
            "ideal_weight": 72.5,
            "fat_mass_to_ideal": "Normal:3.0",
            "protein_percent": 17.8,
            "impedance": 500
        }
        
        # Publish body composition data
        timestamp = int(time.time())
        if mqtt_publisher.publish_body_composition("user1@example.com", timestamp, body_data):
            print("✓ Body composition data published successfully!")
            
            # Publish blood pressure data
            if mqtt_publisher.publish_blood_pressure(
                user_email="user1@example.com",
                timestamp=timestamp + 1,
                systolic=120,
                diastolic=80,
                pulse=65,
                category="Normal",
                mov=0,
                ihb=0
            ):
                print("✓ Blood pressure data published successfully!")
            else:
                print("✗ Failed to publish blood pressure data")
        else:
            print("✗ Failed to publish body composition data")
        
        mqtt_publisher.disconnect()
        print("✓ Disconnected from MQTT broker")
        return True
    else:
        print("✗ Failed to connect to MQTT broker")
        return False

if __name__ == "__main__":
    test_simple_export()