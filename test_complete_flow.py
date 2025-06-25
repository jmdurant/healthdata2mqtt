#!/usr/bin/python3

import time
import threading
import requests
import json
from mqtt_publisher import MQTTHealthDataPublisher

def start_simple_listener():
    """Start a simple MQTT listener to simulate the API"""
    import paho.mqtt.client as mqtt
    
    received_data = {}
    
    def on_connect(client, userdata, flags, rc):
        if rc == 0:
            print("🎧 Listener connected to MQTT broker")
            client.subscribe("health/+/+")
        else:
            print(f"🎧 Listener failed to connect: {rc}")
    
    def on_message(client, userdata, msg):
        try:
            data = json.loads(msg.payload.decode())
            topic_parts = msg.topic.split('/')
            
            if len(topic_parts) >= 3:
                data_type = topic_parts[1]
                user = data.get('user', 'unknown')
                
                if user not in received_data:
                    received_data[user] = {}
                
                received_data[user][data_type] = data
                print(f"🎧 Received {data_type} data for {user}")
                
                # Simulate API endpoint response
                if data_type == 'body_composition':
                    weight = data['data'].get('weight')
                    bmi = data['data'].get('bmi')
                    print(f"   📊 Weight: {weight} kg, BMI: {bmi:.1f}")
                elif data_type == 'blood_pressure':
                    sys = data['data'].get('systolic')
                    dia = data['data'].get('diastolic')
                    print(f"   🩺 BP: {sys}/{dia} mmHg")
                    
        except Exception as e:
            print(f"🎧 Error processing message: {e}")
    
    try:
        client = mqtt.Client(callback_api_version=mqtt.CallbackAPIVersion.VERSION1)
    except AttributeError:
        client = mqtt.Client()
    
    client.on_connect = on_connect
    client.on_message = on_message
    
    try:
        client.connect('localhost', 1883, 60)
        client.loop_forever()
    except Exception as e:
        print(f"🎧 Listener error: {e}")

def test_complete_flow():
    """Test the complete health data flow"""
    print("🧪 Testing Complete Health Data Flow")
    print("=" * 50)
    
    # Start listener in background
    print("🎧 Starting MQTT listener...")
    listener_thread = threading.Thread(target=start_simple_listener, daemon=True)
    listener_thread.start()
    time.sleep(2)
    
    # Initialize publisher
    print("📡 Initializing MQTT publisher...")
    publisher = MQTTHealthDataPublisher(
        broker_host='localhost',
        broker_port=1883
    )
    
    if not publisher.connect():
        print("❌ Failed to connect publisher")
        return False
    
    print("✅ Publisher connected")
    
    # Test 1: Publish body composition data
    print("\n📊 Test 1: Publishing body composition data...")
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
    
    success = publisher.publish_body_composition("user1@example.com", int(time.time()), body_data)
    print(f"   {'✅' if success else '❌'} Body composition: {'Published' if success else 'Failed'}")
    
    time.sleep(1)
    
    # Test 2: Publish blood pressure data
    print("\n🩺 Test 2: Publishing blood pressure data...")
    success = publisher.publish_blood_pressure(
        user_email="user1@example.com",
        timestamp=int(time.time()),
        systolic=120,
        diastolic=80,
        pulse=65,
        category="Normal",
        mov=0,
        ihb=0
    )
    print(f"   {'✅' if success else '❌'} Blood pressure: {'Published' if success else 'Failed'}")
    
    time.sleep(1)
    
    # Test 3: Multiple users
    print("\n👥 Test 3: Publishing data for multiple users...")
    success = publisher.publish_body_composition("user2@example.com", int(time.time()), {
        "weight": 68.2, "bmi": 22.1, "body_fat_percent": 20.3
    })
    print(f"   {'✅' if success else '❌'} User 2 data: {'Published' if success else 'Failed'}")
    
    publisher.disconnect()
    
    # Simulate API access
    print("\n🌐 Test 4: Simulating REST API access...")
    print("   (In real deployment, you would access http://localhost:5001/user/user1@example.com/latest)")
    print("   Expected response: JSON with body_composition and blood_pressure data")
    
    print("\n🎉 Complete flow test finished!")
    print("\n📋 Next steps:")
    print("   1. Start the Health Data API: python3 mqtt-listener/health_data_api.py")
    print("   2. Access API endpoints: http://localhost:5001/")
    print("   3. Integrate with OpenEMR using the provided HTML snippet")
    print("   4. Deploy with Docker: docker-compose up -d")
    
    return True

if __name__ == "__main__":
    test_complete_flow()
    
    # Keep the listener running for a bit to see messages
    print("\nPress Ctrl+C to stop...")
    try:
        time.sleep(30)
    except KeyboardInterrupt:
        print("\n👋 Test completed!")