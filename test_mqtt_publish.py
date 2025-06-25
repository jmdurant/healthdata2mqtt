#!/usr/bin/python3

import sys
import time
from datetime import datetime
from mqtt_publisher import MQTTHealthDataPublisher

def test_mqtt_publishing():
    """Test MQTT publishing with sample data."""
    
    # Initialize publisher
    publisher = MQTTHealthDataPublisher(
        broker_host='localhost',
        broker_port=1883
    )
    
    print("Connecting to MQTT broker...")
    if not publisher.connect():
        print("Failed to connect to MQTT broker!")
        return False
    
    print("Connected successfully!")
    time.sleep(1)
    
    # Test body composition data
    print("\nPublishing test body composition data...")
    test_body_data = {
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
    
    if publisher.publish_body_composition("test@example.com", int(time.time()), test_body_data):
        print("✓ Body composition data published successfully!")
    else:
        print("✗ Failed to publish body composition data")
    
    time.sleep(1)
    
    # Test blood pressure data
    print("\nPublishing test blood pressure data...")
    if publisher.publish_blood_pressure(
        user_email="test@example.com",
        timestamp=int(time.time()),
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
    
    # Disconnect
    publisher.disconnect()
    print("\nTest completed!")
    return True

if __name__ == "__main__":
    print("MQTT Publishing Test")
    print("===================")
    test_mqtt_publishing()