package com.healthdata.mqtt.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.healthdata.mqtt.MainActivity
import com.healthdata.mqtt.R
import com.healthdata.mqtt.data.*
import java.util.*

data class ScannedDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val scanRecord: ScanRecord?
)

class BleHealthScannerService : Service() {
    
    private val TAG = "BleHealthScanner"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "health_scanner_channel"
    
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var mqttPublisher: MQTTHealthDataPublisher
    private lateinit var notificationManager: NotificationManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private val scannedDevices = mutableMapOf<String, ScannedDevice>()
    private val connectedDevices = mutableMapOf<String, BluetoothGatt>()
    
    // Known device UUIDs for health devices
    companion object {
        private val MI_SCALE_SERVICE_UUID = UUID.fromString("0000181B-0000-1000-8000-00805F9B34FB") // Body Composition Service
        private val OMRON_BP_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB") // Battery Service (common in BP monitors)
        
        // FT95 Thermometer UUIDs
        private val HEALTH_THERMOMETER_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805F9B34FB") // Health Thermometer Service
        private val TEMPERATURE_MEASUREMENT_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805F9B34FB") // Temperature Measurement characteristic
        private val TEMPERATURE_TYPE_UUID = UUID.fromString("00002A1D-0000-1000-8000-00805F9B34FB") // Temperature Type characteristic (optional)
        private val INTERMEDIATE_TEMPERATURE_UUID = UUID.fromString("00002A1E-0000-1000-8000-00805F9B34FB") // Intermediate Temperature (optional)
        private val MEASUREMENT_INTERVAL_UUID = UUID.fromString("00002A21-0000-1000-8000-00805F9B34FB") // Measurement Interval (optional)
        
        // Mi Scale characteristics
        private val MI_SCALE_MEASUREMENT_UUID = UUID.fromString("00002A9C-0000-1000-8000-00805F9B34FB")
        
        // Generic UUIDs that health devices might use
        private val DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
        private val BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        
        // Client Characteristic Configuration Descriptor (for notifications)
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): BleHealthScannerService = this@BleHealthScannerService
    }
    
    interface DeviceScanCallback {
        fun onDeviceFound(device: ScannedDevice)
        fun onHealthDataReceived(deviceAddress: String, data: Any)
        fun onScanError(error: String)
    }
    
    private var scanCallback: DeviceScanCallback? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BLE Health Scanner Service created")
        
        try {
            initializeBluetooth()
            createNotificationChannel()
            initializeMQTT() // Initialize but don't connect immediately
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service", e)
            // Don't crash the service, just log the error
        }
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting BLE Health Scanner Service")
        
        try {
            startForeground(NOTIFICATION_ID, createNotification("Starting health device scanner..."))
            
            // Connect to MQTT when we actually start scanning
            connectMQTTIfNeeded()
            
            // Start BLE scanning
            startScanning()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scanning service", e)
            updateNotification("Failed to start scanning: ${e.message}")
        }
        
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BLE Health Scanner Service destroyed")
        
        try {
            stopScanning()
            disconnectAllDevices()
            if (::mqttPublisher.isInitialized) {
                mqttPublisher.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during service cleanup", e)
        }
    }
    
    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (!bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }
    
    private fun initializeMQTT() {
        try {
            // Load MQTT config from SharedPreferences
            val prefs = AppPreferences(this)
            val config = MQTTConfig(
                brokerHost = prefs.mqttHost,
                brokerPort = prefs.mqttPort,
                username = prefs.mqttUsername,
                password = prefs.mqttPassword
            )
            
            mqttPublisher = MQTTHealthDataPublisher(this, config)
            
            // Don't connect immediately during service creation
            // Connection will happen when needed for publishing data
            Log.d(TAG, "MQTT publisher initialized with broker: ${config.brokerHost}:${config.brokerPort}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MQTT publisher", e)
            // Create a dummy publisher to prevent null pointer exceptions
            mqttPublisher = MQTTHealthDataPublisher(this, MQTTConfig())
        }
    }
    
    private fun connectMQTTIfNeeded() {
        try {
            if (!::mqttPublisher.isInitialized) {
                initializeMQTT()
            }
            
            mqttPublisher.connect(object : MQTTHealthDataPublisher.ConnectionCallback {
                override fun onConnected() {
                    Log.i(TAG, "MQTT connected successfully")
                    updateNotification("Connected to MQTT broker")
                }
                
                override fun onConnectionFailed(error: String) {
                    Log.e(TAG, "MQTT connection failed: $error")
                    updateNotification("MQTT connection failed")
                }
                
                override fun onDisconnected() {
                    Log.w(TAG, "MQTT disconnected")
                    updateNotification("MQTT disconnected")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to MQTT", e)
            updateNotification("MQTT connection error")
        }
    }
    
    fun setScanCallback(callback: DeviceScanCallback?) {
        this.scanCallback = callback
    }
    
    fun startScanning() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            scanCallback?.onScanError("Missing Bluetooth permissions")
            return
        }
        
        if (isScanning) {
            Log.d(TAG, "Already scanning")
            return
        }
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        
        val scanFilters = listOf(
            // Filter for Mi Scale (Body Composition Service)
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(MI_SCALE_SERVICE_UUID))
                .build(),
            // Filter for FT95 Thermometer (Health Thermometer Service)
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(HEALTH_THERMOMETER_SERVICE_UUID))
                .build(),
            // Filter for devices with Device Information Service (common in health devices)
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(DEVICE_INFORMATION_SERVICE))
                .build(),
            // Filter by device name patterns
            ScanFilter.Builder()
                .setDeviceName("MI_SCALE")
                .build(),
            ScanFilter.Builder()
                .setDeviceName("OMRON")
                .build(),
            ScanFilter.Builder()
                .setDeviceName("FT95")
                .build()
        )
        
        try {
            bluetoothLeScanner.startScan(scanFilters, scanSettings, leScanCallback)
            isScanning = true
            Log.i(TAG, "Started BLE scanning for health devices")
            updateNotification("Scanning for health devices...")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for BLE scanning", e)
            scanCallback?.onScanError("Permission denied for BLE scanning")
        }
    }
    
    fun stopScanning() {
        if (!isScanning) return
        
        try {
            bluetoothLeScanner.stopScan(leScanCallback)
            isScanning = false
            Log.i(TAG, "Stopped BLE scanning")
            updateNotification("Scan stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping BLE scan", e)
        }
    }
    
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            val device = result.device
            val deviceAddress = device.address
            val deviceName = device.name
            val rssi = result.rssi
            val scanRecord = result.scanRecord
            
            Log.d(TAG, "BLE * Device found: $deviceName ($deviceAddress) RSSI: $rssi")
            
            val scannedDevice = ScannedDevice(deviceAddress, deviceName, rssi, scanRecord)
            scannedDevices[deviceAddress] = scannedDevice
            
            // Notify callback
            scanCallback?.onDeviceFound(scannedDevice)
            
            // Publish device discovery to MQTT
            mqttPublisher.publishDeviceDiscovered(deviceAddress, deviceName, rssi)
            
            // Check if this is a known health device and connect
            if (isHealthDevice(deviceName, scanRecord)) {
                connectToDevice(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val error = "BLE scan failed with error code: $errorCode"
            Log.e(TAG, error)
            scanCallback?.onScanError(error)
            isScanning = false
        }
    }
    
    private fun isHealthDevice(deviceName: String?, scanRecord: ScanRecord?): Boolean {
        // Check device name
        deviceName?.let { name ->
            val lowerName = name.lowercase()
            if (lowerName.contains("mi") && lowerName.contains("scale") ||
                lowerName.contains("omron") ||
                lowerName.contains("blood") && lowerName.contains("pressure") ||
                lowerName.contains("ft95") ||
                lowerName.contains("beurer")) {
                return true
            }
        }
        
        // Check service UUIDs in scan record
        scanRecord?.serviceUuids?.let { uuids ->
            return uuids.any { parcelUuid ->
                val uuid = parcelUuid.uuid
                uuid == MI_SCALE_SERVICE_UUID || 
                uuid == OMRON_BP_SERVICE_UUID ||
                uuid == HEALTH_THERMOMETER_SERVICE_UUID ||
                uuid == DEVICE_INFORMATION_SERVICE
            }
        }
        
        return false
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        if (connectedDevices.containsKey(device.address)) {
            Log.d(TAG, "Already connected to device: ${device.address}")
            return
        }
        
        Log.i(TAG, "Connecting to health device: ${device.name} (${device.address})")
        
        try {
            val gatt = device.connectGatt(this, false, gattCallback)
            connectedDevices[device.address] = gatt
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for GATT connection", e)
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server: ${gatt.device.address}")
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Permission denied for service discovery", e)
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server: ${gatt.device.address}")
                    connectedDevices.remove(gatt.device.address)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for device: ${gatt.device.address}")
                
                // Look for health-related services and characteristics
                for (service in gatt.services) {
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    
                    when (service.uuid) {
                        MI_SCALE_SERVICE_UUID -> {
                            handleMiScaleService(gatt, service)
                        }
                        HEALTH_THERMOMETER_SERVICE_UUID -> {
                            handleHealthThermometerService(gatt, service)
                        }
                        DEVICE_INFORMATION_SERVICE -> {
                            // Read device information if needed
                        }
                    }
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            
            when (characteristic.uuid) {
                MI_SCALE_MEASUREMENT_UUID -> {
                    processMiScaleData(gatt.device.address, characteristic.value)
                }
                TEMPERATURE_MEASUREMENT_UUID -> {
                    processTemperatureData(gatt.device.address, gatt.device.name, characteristic.value)
                }
                INTERMEDIATE_TEMPERATURE_UUID -> {
                    processTemperatureData(gatt.device.address, gatt.device.name, characteristic.value)
                }
            }
        }
    }
    
    private fun handleMiScaleService(gatt: BluetoothGatt, service: BluetoothGattService) {
        val measurementCharacteristic = service.getCharacteristic(MI_SCALE_MEASUREMENT_UUID)
        
        if (measurementCharacteristic != null) {
            try {
                // Enable notifications for weight measurements
                gatt.setCharacteristicNotification(measurementCharacteristic, true)
                
                val descriptor = measurementCharacteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for characteristic notification", e)
            }
        }
    }
    
    private fun processMiScaleData(deviceAddress: String, data: ByteArray) {
        try {
            // Parse Mi Scale data format (this is a simplified example)
            // Real implementation would need to parse the actual BLE protocol
            
            if (data.size >= 8) {
                val weight = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
                val impedance = (data[3].toInt() and 0xFF) or ((data[4].toInt() and 0xFF) shl 8)
                val weightKg = weight / 100.0 // Convert to kg
                
                Log.i(TAG, "MISCALE * Reading BLE data complete: Weight=${weightKg}kg, Impedance=${impedance}Ω")
                
                // Publish raw data to MQTT
                mqttPublisher.publishRawScaleData(deviceAddress, weightKg, impedance.toDouble())
                
                // Calculate body composition (would need user profile)
                // This is a placeholder - real implementation would get user data from preferences
                /*
                try {
                    val bodyMetrics = BodyMetrics(weightKg, 170, 30, Sex.MALE, impedance.toDouble())
                    val composition = bodyMetrics.getBodyComposition()
                    
                    mqttPublisher.publishBodyComposition("user@example.com", composition)
                    scanCallback?.onHealthDataReceived(deviceAddress, composition)
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating body composition", e)
                }
                */
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Mi Scale data", e)
        }
    }
    
    private fun handleHealthThermometerService(gatt: BluetoothGatt, service: BluetoothGattService) {
        Log.i(TAG, "THERMOMETER * Setting up Health Thermometer Service for device: ${gatt.device.address}")
        
        // Try to find the Temperature Measurement characteristic
        val tempMeasurementChar = service.getCharacteristic(TEMPERATURE_MEASUREMENT_UUID)
        if (tempMeasurementChar != null) {
            try {
                Log.d(TAG, "Found Temperature Measurement characteristic, enabling notifications...")
                // Enable notifications for temperature measurements
                gatt.setCharacteristicNotification(tempMeasurementChar, true)
                
                // Set the notification descriptor
                val descriptor = tempMeasurementChar.getDescriptor(CCCD_UUID)
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                    Log.d(TAG, "Temperature measurement notifications enabled")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for temperature characteristic notification", e)
            }
        } else {
            Log.w(TAG, "Temperature Measurement characteristic not found")
        }
        
        // Also try to find the Intermediate Temperature characteristic (optional)
        val intermediateTempChar = service.getCharacteristic(INTERMEDIATE_TEMPERATURE_UUID)
        if (intermediateTempChar != null) {
            try {
                Log.d(TAG, "Found Intermediate Temperature characteristic, enabling notifications...")
                gatt.setCharacteristicNotification(intermediateTempChar, true)
                
                val descriptor = intermediateTempChar.getDescriptor(CCCD_UUID)
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                    Log.d(TAG, "Intermediate temperature notifications enabled")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for intermediate temperature characteristic notification", e)
            }
        }
    }
    
    private fun processTemperatureData(deviceAddress: String, deviceName: String?, data: ByteArray) {
        try {
            Log.d(TAG, "Processing temperature data from $deviceAddress (${data.size} bytes)")
            
            if (data.isEmpty()) {
                Log.w(TAG, "Empty temperature data received")
                return
            }
            
            // Parse the temperature measurement data according to Bluetooth SIG specification
            // Temperature Measurement characteristic format:
            // Flags (1 byte) + Temperature Measurement Value (4 bytes IEEE-11073 FLOAT) + optional fields
            
            val flags = data[0].toInt() and 0xFF
            Log.d(TAG, "Temperature measurement flags: 0x${flags.toString(16).uppercase()}")
            
            // Check temperature unit (bit 0 of flags)
            val temperatureUnitFahrenheit = (flags and 0x01) != 0
            val unit = if (temperatureUnitFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
            
            // Check if timestamp is present (bit 1 of flags)
            val timestampPresent = (flags and 0x02) != 0
            
            // Check if temperature type is present (bit 2 of flags)
            val temperatureTypePresent = (flags and 0x04) != 0
            
            if (data.size < 5) {
                Log.w(TAG, "Temperature data too short (${data.size} bytes)")
                return
            }
            
            // Parse temperature value (IEEE-11073 32-bit FLOAT format)
            val temperatureValue = parseIEEE11073Float(data, 1)
            
            Log.i(TAG, "Temperature reading: ${temperatureValue}° ${unit.name}")
            
            // Create temperature reading
            val temperatureReading = if (temperatureUnitFahrenheit) {
                TemperatureReading.fromFahrenheit(temperatureValue, MeasurementLocation.FOREHEAD)
            } else {
                TemperatureReading.fromCelsius(temperatureValue, MeasurementLocation.FOREHEAD)
            }.copy(
                deviceAddress = deviceAddress,
                deviceName = deviceName ?: "Unknown"
            )
            
            // Publish to MQTT
            if (::mqttPublisher.isInitialized) {
                val prefs = AppPreferences(this)
                val userEmail = prefs.userEmail ?: "unknown@example.com"
                mqttPublisher.publishTemperatureReading(userEmail, temperatureReading)
            }
            
            // Notify callback
            scanCallback?.onHealthDataReceived(deviceAddress, temperatureReading)
            
            Log.i(TAG, "THERMOMETER * Temperature reading complete: ${temperatureReading.temperatureCelsius}°C / ${temperatureReading.temperatureFahrenheit}°F from ${deviceName ?: "Unknown"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing temperature data", e)
        }
    }
    
    private fun parseIEEE11073Float(data: ByteArray, offset: Int): Double {
        if (data.size < offset + 4) {
            throw IllegalArgumentException("Insufficient data for IEEE-11073 FLOAT")
        }
        
        // IEEE-11073 32-bit FLOAT format:
        // Mantissa (24 bits) + Exponent (8 bits)
        val value = (data[offset].toInt() and 0xFF) or
                   ((data[offset + 1].toInt() and 0xFF) shl 8) or
                   ((data[offset + 2].toInt() and 0xFF) shl 16) or
                   ((data[offset + 3].toInt() and 0xFF) shl 24)
        
        val mantissa = value and 0x00FFFFFF
        val exponent = (value shr 24).toByte()
        
        // Convert signed mantissa
        val signedMantissa = if ((mantissa and 0x00800000) != 0) {
            mantissa or 0xFF000000.toInt() // Sign extend
        } else {
            mantissa
        }
        
        return signedMantissa * Math.pow(10.0, exponent.toDouble())
    }
    
    private fun disconnectAllDevices() {
        for (gatt in connectedDevices.values) {
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied for disconnecting GATT", e)
            }
        }
        connectedDevices.clear()
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun createNotificationChannel() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Scanner Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of health device scanning"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(content: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Health Device Scanner")
        .setContentText(content)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
    
    private fun updateNotification(content: String) {
        if (::notificationManager.isInitialized) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(content))
        }
    }
    
    // Public interface methods
    fun getScannedDevices(): List<ScannedDevice> = scannedDevices.values.toList()
    fun getConnectedDeviceCount(): Int = connectedDevices.size
    fun isCurrentlyScanning(): Boolean = isScanning
    fun getMQTTConnectionStatus(): String = mqttPublisher.getConnectionInfo()
}