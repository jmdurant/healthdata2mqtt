#!/usr/bin/python3

import os
import csv
from datetime import datetime as dt, date

class User():
    def __init__(self, sex, height, birthdate, email, max_weight, min_weight):
        self.sex = sex
        self.height = height
        self.birthdate = birthdate
        self.email = email
        self.max_weight = max_weight
        self.min_weight = min_weight

# Importing user variables from a file
path = os.path.dirname(os.path.abspath(__file__))
users = []

with open(path + '/user/export2mqtt.cfg', 'r') as file:
    for line in file:
        line = line.strip()
        if line.startswith('miscale_export_'):
            print(f"Found user line: {line}")
            user_data = eval(line.split('=')[1].strip())
            print(f"Parsed user data: {user_data}")
            users.append(User(*user_data))

print(f"Number of users loaded: {len(users)}")
for i, user in enumerate(users):
    print(f"User {i+1}: email={user.email}, min_weight={user.min_weight}, max_weight={user.max_weight}")

# Import data from CSV
try:
    with open(path + '/user/miscale_backup.csv', 'r') as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=';')
        for row in csv_reader:
            print(f"CSV row: {row}")
            if str(row[0]) in ["failed", "to_import"]:
                mi_datetime = int(row[1])
                mi_weight = float(row[2])
                mi_impedance = float(row[3])
                print(f"Found weight data: {mi_weight} kg")
                break
    
    # Test user matching
    print(f"\nTesting weight {mi_weight} kg against users:")
    selected_user = None
    for user in users:
        print(f"  User {user.email}: min={user.min_weight}, max={user.max_weight}")
        print(f"    Check: {user.min_weight} <= {mi_weight} <= {user.max_weight} = {user.min_weight <= mi_weight <= user.max_weight}")
        if user.min_weight <= mi_weight <= user.max_weight:
            selected_user = user
            print(f"    MATCH!")
            break
    
    if selected_user:
        print(f"\nSelected user: {selected_user.email}")
    else:
        print("\nNo matching user found!")
        
except Exception as e:
    print(f"Error: {e}")