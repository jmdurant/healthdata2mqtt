#!/usr/bin/python3

import os
import asyncio
import time
from datetime import datetime
from bleak import BleakScanner
from xiaomi_ble.parser import XiaomiBluetoothDeviceData
from bluetooth_sensor_state_data import BluetoothServiceInfo

# Version info
print("""
=============================================
Export 2 Garmin Connect v3.0 (s400_ble.py)
=============================================
""")

# Importing bluetooth variables from a file
path = os.path.dirname(os.path.dirname(__file__))
with open(path + '/user/export2garmin.cfg', 'r') as file:
    for line in file:
        line = line.strip()
        if line.startswith('ble_miscale_'):
            name, value = line.split('=')
            globals()[name.strip()] = value.strip()
ble_token = bytes.fromhex(ble_miscale_token)
parser = XiaomiBluetoothDeviceData(bindkey=ble_token)
stop_event = asyncio.Event()
mac_seen_event = asyncio.Event()

# Reading data from a scale using a BLE adapter
def detection_callback(device, advertisement_data):
    if device.address.upper() != ble_miscale_mac.upper():
        return
    print(f"  BLE device found with address: {device.address.upper()}")
    mac_seen_event.set()
    service_data = advertisement_data.service_data.get("0000fe95-0000-1000-8000-00805f9b34fb")
    if not service_data:
        return
    service_info = BluetoothServiceInfo(name=device.name,address=device.address,rssi=advertisement_data.rssi,manufacturer_data=advertisement_data.manufacturer_data,service_data=advertisement_data.service_data,service_uuids=advertisement_data.service_uuids,source=device.address)
    if parser.supported(service_info):
        update = parser.update(service_info)
        if update and update.entity_values:
            fields = {'Mass','Impedance','Impedance Low','Heart Rate'}
            values = {v.name: v.native_value for v in update.entity_values.values() if v.name in fields}
            if fields <= values.keys():
                print(f"{datetime.now().strftime('%d.%m.%Y-%H:%M:%S')} * Reading BLE data complete, finished BLE scan")
                print(f"to_import;{int(time.time())};{values['Mass']};{values['Impedance']:.0f};{values['Impedance Low']:.0f};{values['Heart Rate']}")
                stop_event.set()

async def main():
    print(f"{datetime.now().strftime('%d.%m.%Y-%H:%M:%S')} * Starting BLE scan")
    attempts = 0
    while attempts < 5 and not stop_event.is_set():
        mac_seen_event.clear()
        scanner = BleakScanner(detection_callback=detection_callback)
        await scanner.start()
        try:
            await asyncio.wait_for(mac_seen_event.wait(), timeout=1)
            await stop_event.wait()
            break
        except asyncio.TimeoutError:
            print(f"  BLE device not found with address: {ble_miscale_mac.upper()}")
            attempts += 1
        finally:
            await scanner.stop()
    if not stop_event.is_set():
        print(f"{datetime.now().strftime('%d.%m.%Y-%H:%M:%S')} * Reading BLE data failed, finished BLE scan")

# Main program loop
if __name__ == "__main__":
    asyncio.run(main())