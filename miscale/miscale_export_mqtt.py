#!/usr/bin/python3

import os
import csv
import sys
import Xiaomi_Scale_Body_Metrics
from datetime import datetime as dt, date

# Add parent directory to path to import mqtt_publisher
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from mqtt_publisher import MQTTHealthDataPublisher

# Version info
print("""
===============================================
Export 2 MQTT v1.0 (miscale_export_mqtt.py)
===============================================
""")

class User():
    def __init__(self, sex, height, birthdate, email, max_weight, min_weight):
        self.sex = sex
        self.height = height
        self.birthdate = birthdate
        self.email = email
        self.max_weight = max_weight
        self.min_weight = min_weight

    # Calculating age
    @property
    def age(self):
        today = date.today()
        calc_date = dt.strptime(self.birthdate, "%d-%m-%Y")
        return today.year - calc_date.year

# Importing user variables and MQTT config from a file
path = os.path.dirname(os.path.dirname(__file__))
users = []
mqtt_config = {'host': 'localhost', 'port': 1883, 'username': None, 'password': None}

with open(path + '/user/export2mqtt.cfg', 'r') as file:
    for line in file:
        line = line.strip()
        if line.startswith('miscale_export_'):
            user_data = eval(line.split('=')[1].strip())
            users.append(User(*user_data))
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
with open(path + '/user/miscale_backup.csv', 'r') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=';')
    for row in csv_reader:
        if str(row[0]) in ["failed", "to_import"]:
            mi_datetime = int(row[1])
            mi_weight = float(row[2])
            mi_impedance = float(row[3])
            break

# Matching user account to weight
selected_user = None
for user in users:
    if user.min_weight <= mi_weight <= user.max_weight:
        selected_user = user
        break

# Calculating body metrics
if selected_user is not None and 'email@email.com' not in selected_user.email:
    lib = Xiaomi_Scale_Body_Metrics.bodyMetrics(mi_weight, selected_user.height, selected_user.age, selected_user.sex, int(mi_impedance))
    
    # Collect all metrics
    body_data = {
        "weight": mi_weight,
        "bmi": lib.getBMI(),
        "body_fat_percent": lib.getFatPercentage(),
        "muscle_mass": lib.getMuscleMass(),
        "bone_mass": lib.getBoneMass(),
        "water_percent": lib.getWaterPercentage(),
        "physique_rating": lib.getBodyType(),
        "visceral_fat": lib.getVisceralFat(),
        "metabolic_age": lib.getMetabolicAge(),
        "bmr": lib.getBMR(),
        "lbm": lib.getLBMCoefficient(),
        "ideal_weight": lib.getIdealWeight(),
        "fat_mass_to_ideal": lib.getFatMassToIdeal(),
        "protein_percent": lib.getProteinPercentage(),
        "impedance": mi_impedance
    }

    # Print to temp.log file
    formatted_time = dt.fromtimestamp(mi_datetime).strftime("%d.%m.%Y;%H:%M")
    print(f"MISCALE * Import data: {mi_datetime};{mi_weight:.1f};{mi_impedance:.0f}")
    print(f"MISCALE * Calculated data: {formatted_time};{mi_weight:.1f};{body_data['bmi']:.1f};{body_data['body_fat_percent']:.1f};{body_data['muscle_mass']:.1f};{body_data['bone_mass']:.1f};{body_data['water_percent']:.1f};{body_data['physique_rating']:.0f};{body_data['visceral_fat']:.0f};{body_data['metabolic_age']:.0f};{body_data['bmr']:.0f};{body_data['lbm']:.1f};{body_data['ideal_weight']:.1f};{body_data['fat_mass_to_ideal']};{body_data['protein_percent']:.1f};{mi_impedance:.0f};{selected_user.email};{dt.now().strftime('%d.%m.%Y;%H:%M')}")

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
        if mqtt_publisher.publish_body_composition(selected_user.email, mi_datetime, body_data):
            print("MISCALE * Upload status: OK")
        else:
            print("MISCALE * Upload status: FAILED")
            raise Exception("Failed to publish to MQTT")
        
        mqtt_publisher.disconnect()
    else:
        print("MISCALE * Failed to connect to MQTT broker")
        raise Exception("Failed to connect to MQTT broker")
else:
    # Print to temp.log file
    print(f"MISCALE * Import data: {mi_datetime};{mi_weight:.1f};{mi_impedance:.0f}")
    print("MISCALE * There is no user with given weight or undefined user email@email.com, check users section in export2garmin.cfg")