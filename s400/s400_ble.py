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
        if line.startswith('s400_'):
            name, value = line.split('=')
            globals()[name.strip()] = value.strip()

# Reading data from a scale using a BLE adapter
ble_token = bytes.fromhex(s400_token)
parser = XiaomiBluetoothDeviceData(bindkey=ble_token)
stop_event = asyncio.Event()

def detection_callback(device,advertisement_data):
    if device.address.upper() != s400_mac:
        return
    print(f"BLE device found with address: {device.address.upper()} <= target device")

    service_data = advertisement_data.service_data.get("0000fe95-0000-1000-8000-00805f9b34fb")
    if not service_data:
        return

    service_info = BluetoothServiceInfo(name=device.name,address=device.address,rssi=advertisement_data.rssi,manufacturer_data=advertisement_data.manufacturer_data,service_data=advertisement_data.service_data,service_uuids=advertisement_data.service_uuids,source=device.address)
    if not parser.supported(service_info):
        return

    update = parser.update(service_info)
    if not update or not update.entity_values:
        return

    fields = {'Mass','Impedance','Impedance Low','Heart Rate'}
    values = {v.name: v.native_value for v in update.entity_values.values() if v.name in fields}

    if fields <= values.keys():
        print(f"{datetime.now().strftime('%d.%m.%Y-%H:%M:%S')} * Reading BLE data complete, finished BLE scan")
        print(f"{int(time.time())};{values['Mass']};{values['Impedance']};{values['Impedance Low']};{values['Heart Rate']}")
        stop_event.set()

async def main():
    print(f"{datetime.now().strftime('%d.%m.%Y-%H:%M:%S')} * Starting BLE scan:")
    scanner = BleakScanner(detection_callback=detection_callback)
    await scanner.start()
    await stop_event.wait()
    await scanner.stop()

# Main program loop
if __name__ == "__main__":
    asyncio.run(main())