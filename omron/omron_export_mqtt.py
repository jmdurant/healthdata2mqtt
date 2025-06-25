#!/usr/bin/python3

import os
import csv
import sys
from datetime import datetime as dt

# Add parent directory to path to import mqtt_publisher
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from mqtt_publisher import MQTTHealthDataPublisher

# Version info
print("""
=============================================
Export 2 MQTT v1.0 (omron_export_mqtt.py)
=============================================
""")

# Importing user variables and MQTT config from a file
path = os.path.dirname(os.path.dirname(__file__))
mqtt_config = {'host': 'localhost', 'port': 1883, 'username': None, 'password': None}

with open(path + '/user/export2mqtt.cfg', 'r') as file:
    for line in file:
        line = line.strip()
        if line.startswith('omron_export_category'):
            name, value = line.split('=')
            globals()[name.strip()] = value.strip()
        elif line.startswith('mqtt_host='):
            mqtt_config['host'] = line.split('=')[1].strip()
        elif line.startswith('mqtt_port='):
            mqtt_config['port'] = int(line.split('=')[1].strip())
        elif line.startswith('mqtt_username='):
            username = line.split('=')[1].strip()
            mqtt_config['username'] = username if username else None
        elif line.startswith('mqtt_password='):
            password = line.split('=')[1].strip()
            mqtt_config['password'] = password if password else None

# Import data variables from a file
with open(path + '/user/omron_backup.csv', 'r') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=';')
    for row in csv_reader:
        if str(row[0]) in ["failed", "to_import"]:
            unixtime = int(row[1])
            omrondate = str(row[2])
            omrontime = str(row[3])
            systolic = int(row[4])
            diastolic = int(row[5])
            pulse = int(row[6])
            MOV = int(row[7])
            IHB = int(row[8])
            emailuser = str(row[9])

            # Determine blood pressure category
            omron_export_category = str(omron_export_category)
            category = "None"
            if omron_export_category == 'eu':
                if systolic < 130 and diastolic < 85:
                    category = "Normal"
                elif (130 <= systolic <= 139 and diastolic < 85) or (systolic < 130 and 85 <= diastolic <= 89):
                    category = "High-Normal"
                elif (140 <= systolic <= 159 and diastolic < 90) or (systolic < 140 and 90 <= diastolic <= 99):
                    category = "Grade_1"
                elif (160 <= systolic <= 179 and diastolic < 100) or (systolic < 160 and 100 <= diastolic <= 109):
                    category = "Grade_2"                 
            elif omron_export_category == 'us':
                if systolic < 120 and diastolic < 80:
                    category = "Normal"
                elif (120 <= systolic <= 129) and diastolic < 80:
                    category = "High-Normal"
                elif (130 <= systolic <= 139) or (80 <= diastolic <= 89):
                    category = "Grade_1"
                elif (systolic >= 140) or (diastolic >= 90):
                    category = "Grade_2"

            # Print to temp.log file
            print(f"OMRON * Import data: {unixtime};{omrondate};{omrontime};{systolic:.0f};{diastolic:.0f};{pulse:.0f};{MOV:.0f};{IHB:.0f};{emailuser}")
            print(f"OMRON * Calculated data: {category};{MOV:.0f};{IHB:.0f};{emailuser};{dt.now().strftime('%d.%m.%Y;%H:%M')}")

            # Initialize MQTT publisher
            mqtt_publisher = MQTTHealthDataPublisher(
                broker_host=mqtt_config['host'],
                broker_port=mqtt_config['port'],
                username=mqtt_config['username'],
                password=mqtt_config['password']
            )

            # Connect to MQTT broker
            if mqtt_publisher.connect():
                # Publish data to MQTT
                if mqtt_publisher.publish_blood_pressure(
                    user_email=emailuser,
                    timestamp=unixtime,
                    systolic=systolic,
                    diastolic=diastolic,
                    pulse=pulse,
                    category=category,
                    mov=MOV,
                    ihb=IHB
                ):
                    print("OMRON * Upload status: OK")
                else:
                    print("OMRON * Upload status: FAILED")
                    raise Exception("Failed to publish to MQTT")
                
                mqtt_publisher.disconnect()
            else:
                print("OMRON * Failed to connect to MQTT broker")
                raise Exception("Failed to connect to MQTT broker")
            break