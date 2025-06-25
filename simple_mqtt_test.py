#!/usr/bin/python3

import subprocess
import time

# Use the mosquitto client tools to test MQTT
def test_with_mosquitto_tools():
    """Test MQTT with built-in mosquitto tools."""
    
    print("Testing MQTT with mosquitto tools...")
    
    # Publish a test message
    try:
        result = subprocess.run([
            'docker', 'exec', 'test-mosquitto', 
            'mosquitto_pub', '-t', 'health/test', '-m', 'Test message from healthdata2mqtt'
        ], capture_output=True, text=True, timeout=10)
        
        if result.returncode == 0:
            print("✓ Successfully published test message")
        else:
            print(f"✗ Failed to publish: {result.stderr}")
            return False
    except Exception as e:
        print(f"✗ Error publishing: {e}")
        return False
    
    # Subscribe and check if we receive the message
    try:
        result = subprocess.run([
            'docker', 'exec', 'test-mosquitto',
            'mosquitto_sub', '-t', 'health/test', '-C', '1', '-W', '5'
        ], capture_output=True, text=True, timeout=10)
        
        if result.returncode == 0 and 'Test message' in result.stdout:
            print("✓ Successfully received test message")
            print(f"  Received: {result.stdout.strip()}")
            return True
        else:
            print(f"✗ Failed to receive message: {result.stderr}")
            return False
    except Exception as e:
        print(f"✗ Error subscribing: {e}")
        return False

def test_continuous_subscription():
    """Start a continuous subscription to see all messages."""
    print("\nStarting continuous subscription to health/# (press Ctrl+C to stop)...")
    print("You can publish test messages from another terminal.")
    
    try:
        subprocess.run([
            'docker', 'exec', '-it', 'test-mosquitto',
            'mosquitto_sub', '-t', 'health/#', '-v'
        ])
    except KeyboardInterrupt:
        print("\nSubscription stopped.")

if __name__ == "__main__":
    print("MQTT Broker Test")
    print("================")
    
    if test_with_mosquitto_tools():
        print("\n✓ MQTT broker is working correctly!")
        
        # Ask if user wants to see continuous subscription
        print("\nWould you like to start a continuous subscription to see messages?")
        print("This will listen for all health/* topics. Press Ctrl+C to stop.")
        response = input("Start subscription? (y/n): ")
        
        if response.lower() == 'y':
            test_continuous_subscription()
    else:
        print("\n✗ MQTT broker test failed!")
        print("Check if the mosquitto container is running: docker ps")