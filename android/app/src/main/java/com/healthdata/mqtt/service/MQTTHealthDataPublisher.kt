package com.healthdata.mqtt.service

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.healthdata.mqtt.data.BloodPressureReading
import com.healthdata.mqtt.data.BodyComposition
import com.healthdata.mqtt.data.PulseOximetryReading
import com.healthdata.mqtt.data.TemperatureReading
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.concurrent.CompletableFuture
import java.text.SimpleDateFormat
import java.util.*

data class MQTTConfig(
    val brokerHost: String = "localhost",
    val brokerPort: Int = 1883,
    val username: String? = null,
    val password: String? = null,
    val clientId: String = "android_health_client_${System.currentTimeMillis()}"
)

class MQTTHealthDataPublisher(
    private val context: Context,
    private val config: MQTTConfig = MQTTConfig()
) {
    private val TAG = "MQTTHealthPublisher"
    
    // Initialize these safely in init block instead
    private lateinit var gson: Gson
    private lateinit var dateFormat: SimpleDateFormat
    
    private var mqttClient: Mqtt3AsyncClient? = null
    private var isConnected = false
    
    interface ConnectionCallback {
        fun onConnected()
        fun onConnectionFailed(error: String)
        fun onDisconnected()
    }
    
    interface PublishCallback {
        fun onPublishSuccess(topic: String)
        fun onPublishFailed(topic: String, error: String)
    }
    
    init {
        Log.d(TAG, "🔧 MQTTHealthDataPublisher init started")
        try {
            gson = GsonBuilder().setPrettyPrinting().create()
            Log.d(TAG, "✅ Gson created")
            dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            Log.d(TAG, "✅ DateFormat created")
            Log.d(TAG, "✅ MQTTHealthDataPublisher init completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Init failed", e)
            throw e
        }
    }
    
    private fun initializeMqttClient(): Boolean {
        return try {
            val serverUri = "tcp://${config.brokerHost}:${config.brokerPort}"
            Log.d(TAG, "Initializing MQTT client for $serverUri")
            Log.d(TAG, "Client ID: ${config.clientId}")
            
            // Deferred initialization - don't create MqttAndroidClient until connect() is called
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MQTT client: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }
    
    fun connect(callback: ConnectionCallback? = null) {
        Log.d(TAG, "🔄 Connect called - current state: isConnected=$isConnected")
        
        if (isConnected) {
            Log.d(TAG, "Already connected, skipping connection")
            callback?.onConnected()
            return
        }
        
        try {
            Log.i(TAG, "🌐 Creating HiveMQ client for ${config.brokerHost}:${config.brokerPort} with clientId: ${config.clientId}")
            
            val clientBuilder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(config.clientId)
                .serverHost(config.brokerHost)
                .serverPort(config.brokerPort)
            
            if (!config.username.isNullOrEmpty() && !config.password.isNullOrEmpty()) {
                Log.d(TAG, "🔑 Setting authentication: username=${config.username}")
                clientBuilder.simpleAuth()
                    .username(config.username)
                    .password(config.password.toByteArray())
                    .applySimpleAuth()
            } else {
                Log.d(TAG, "🔓 No authentication configured - using anonymous")
            }
            
            mqttClient = clientBuilder.buildAsync()
            Log.d(TAG, "✅ HiveMQ client created successfully")
            
            Log.i(TAG, "🚀 Attempting to connect to MQTT broker...")
            
            mqttClient!!.connect()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        isConnected = false
                        val errorMsg = "Failed to connect to MQTT broker: ${throwable.javaClass.simpleName}: ${throwable.message}"
                        Log.e(TAG, "❌ $errorMsg", throwable)
                        callback?.onConnectionFailed(errorMsg)
                    } else {
                        isConnected = true
                        Log.i(TAG, "🎉 Successfully connected to MQTT broker!")
                        callback?.onConnected()
                    }
                }
        } catch (e: Exception) {
            val errorMsg = "Connection error: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onConnectionFailed(errorMsg)
        }
    }
    
    fun disconnect() {
        try {
            mqttClient?.disconnect()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Error disconnecting: ${throwable.message}", throwable)
                } else {
                    Log.i(TAG, "Disconnected from MQTT broker")
                }
            }
            isConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }
    
    fun isConnected(): Boolean = isConnected && mqttClient?.state?.isConnected == true
    
    // Include all the publish methods from the original implementation
    fun publishBodyComposition(
        userEmail: String,
        bodyComposition: BodyComposition,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("body_composition", errorMsg)
            return
        }

        try {
            val topic = "healthdata/${sanitizeEmailForTopic(userEmail)}/body_composition"
            
            val bodyData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "weight" to bodyComposition.weight,
                "height" to bodyComposition.height,
                "age" to bodyComposition.age,
                "sex" to bodyComposition.sex.name,
                "impedance" to bodyComposition.impedance,
                "bmi" to bodyComposition.bmi,
                "fat_percentage" to bodyComposition.fatPercentage,
                "water_percentage" to bodyComposition.waterPercentage,
                "bone_mass" to bodyComposition.boneMass,
                "muscle_mass" to bodyComposition.muscleMass,
                "visceral_fat" to bodyComposition.visceralFat,
                "bmr" to bodyComposition.bmr,
                "protein_percentage" to bodyComposition.proteinPercentage,
                "body_type" to bodyComposition.bodyType,
                "metabolic_age" to bodyComposition.metabolicAge,
                "ideal_weight" to bodyComposition.idealWeight,
                "fat_mass_to_ideal" to bodyComposition.fatMassToIdeal,
                "data_type" to "body_composition"
            )

            val payload = gson.toJson(bodyData)
            Log.d(TAG, "Publishing body composition to topic: $topic")
            Log.d(TAG, "Body composition payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish body composition: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published body composition: BMI=${bodyComposition.bmi}, Weight=${bodyComposition.weight}kg")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing body composition: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("body_composition", errorMsg)
        }
    }
    
    fun publishBloodPressure(
        userEmail: String,
        bloodPressure: BloodPressureReading,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("blood_pressure", errorMsg)
            return
        }

        try {
            val topic = "healthdata/${sanitizeEmailForTopic(userEmail)}/blood_pressure"
            
            val bloodPressureData = mapOf(
                "timestamp" to bloodPressure.timestamp,
                "date" to bloodPressure.date,
                "time" to bloodPressure.time,
                "systolic" to bloodPressure.systolic,
                "diastolic" to bloodPressure.diastolic,
                "pulse" to bloodPressure.pulse,
                "movement_error" to bloodPressure.mov,
                "irregular_heartbeat" to bloodPressure.ihb,
                "category" to bloodPressure.category.toString(),
                "data_type" to "blood_pressure_measurement"
            )

            val payload = gson.toJson(bloodPressureData)
            Log.d(TAG, "Publishing blood pressure to topic: $topic")
            Log.d(TAG, "Blood pressure payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish blood pressure: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published blood pressure: ${bloodPressure.systolic}/${bloodPressure.diastolic} mmHg, Pulse: ${bloodPressure.pulse} bpm")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing blood pressure: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("blood_pressure", errorMsg)
        }
    }
    
    fun publishTemperatureReading(
        userEmail: String,
        temperatureReading: TemperatureReading,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("temperature", errorMsg)
            return
        }
        
        try {
            val topic = "healthdata/${sanitizeEmailForTopic(userEmail)}/temperature"
            
            val temperatureData = mapOf(
                "timestamp" to dateFormat.format(temperatureReading.timestamp),
                "temperature_celsius" to temperatureReading.temperatureCelsius,
                "temperature_fahrenheit" to temperatureReading.temperatureFahrenheit,
                "measurement_location" to temperatureReading.measurementLocation.name.lowercase(),
                "unit" to temperatureReading.unit.name.lowercase(),
                "device_address" to temperatureReading.deviceAddress,
                "device_name" to temperatureReading.deviceName,
                "is_valid" to temperatureReading.isValid,
                "data_type" to "temperature_measurement"
            )
            
            val payload = gson.toJson(temperatureData)
            Log.d(TAG, "Publishing temperature reading to topic: $topic")
            Log.d(TAG, "Temperature payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish temperature reading: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published temperature reading: ${temperatureReading.temperatureCelsius}°C")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing temperature reading: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("temperature", errorMsg)
        }
    }
    
    fun publishRawScaleData(
        deviceMac: String,
        weight: Double,
        impedance: Double,
        batteryVoltage: Double? = null,
        batteryPercent: Int? = null,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("raw_scale_data", errorMsg)
            return
        }

        try {
            val topic = "healthdata/devices/${deviceMac.replace(":", "_")}/raw_scale_data"
            
            val rawScaleData = mutableMapOf<String, Any?>(
                "timestamp" to System.currentTimeMillis(),
                "device_mac" to deviceMac,
                "weight" to weight,
                "impedance" to impedance,
                "data_type" to "raw_scale_measurement"
            )
            
            batteryVoltage?.let { rawScaleData["battery_voltage"] = it }
            batteryPercent?.let { rawScaleData["battery_percent"] = it }

            val payload = gson.toJson(rawScaleData)
            Log.d(TAG, "Publishing raw scale data to topic: $topic")
            Log.d(TAG, "Raw scale data payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish raw scale data: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published raw scale data: Weight=${weight}kg, Impedance=${impedance}Ω")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing raw scale data: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("raw_scale_data", errorMsg)
        }
    }
    
    fun publishDeviceDiscovered(
        deviceMac: String,
        deviceName: String?,
        rssi: Int,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("device_discovery", errorMsg)
            return
        }

        try {
            val topic = "healthdata/devices/discovery"
            
            val discoveryData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "device_mac" to deviceMac,
                "device_name" to (deviceName ?: "Unknown"),
                "rssi" to rssi,
                "data_type" to "device_discovery"
            )

            val payload = gson.toJson(discoveryData)
            Log.d(TAG, "Publishing device discovery to topic: $topic")
            Log.d(TAG, "Device discovery payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish device discovery: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published device discovery: ${deviceName ?: "Unknown"} ($deviceMac) RSSI: ${rssi}dBm")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing device discovery: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("device_discovery", errorMsg)
        }
    }
    
    fun publishPulseOximetryReading(
        userEmail: String,
        pulseOxReading: PulseOximetryReading,
        callback: PublishCallback? = null
    ) {
        if (!isConnected()) {
            val errorMsg = "MQTT client not connected"
            Log.e(TAG, errorMsg)
            callback?.onPublishFailed("pulse_oximetry", errorMsg)
            return
        }

        try {
            val topic = "healthdata/${sanitizeEmailForTopic(userEmail)}/pulse_oximetry"
            
            val pulseOxData = mapOf(
                "timestamp" to dateFormat.format(pulseOxReading.timestamp),
                "spo2_percentage" to pulseOxReading.spo2Percentage,
                "pulse_rate" to pulseOxReading.pulseRate,
                "signal_quality" to pulseOxReading.signalQuality.name.lowercase(),
                "spo2_category" to pulseOxReading.getSpo2CategoryString(),
                "pulse_rate_category" to pulseOxReading.getPulseRateCategoryString(),
                "device_address" to pulseOxReading.deviceAddress,
                "device_name" to pulseOxReading.deviceName,
                "is_valid_reading" to pulseOxReading.isValidReading,
                "plethysmogram_data_size" to pulseOxReading.plethysmogramData.size,
                "battery_level" to pulseOxReading.batteryLevel,
                "data_type" to "pulse_oximetry_measurement"
            )

            val payload = gson.toJson(pulseOxData)
            Log.d(TAG, "Publishing pulse oximetry reading to topic: $topic")
            Log.d(TAG, "Pulse oximetry payload: $payload")
            
            mqttClient?.publish(
                Mqtt3Publish.builder()
                    .topic(topic)
                    .payload(payload.toByteArray())
                    .build()
            )?.whenComplete { _, throwable ->
                if (throwable != null) {
                    val errorMsg = "Failed to publish pulse oximetry reading: ${throwable.message}"
                    Log.e(TAG, errorMsg, throwable)
                    callback?.onPublishFailed(topic, errorMsg)
                } else {
                    Log.i(TAG, "Successfully published pulse oximetry: SpO2=${pulseOxReading.spo2Percentage}%, HR=${pulseOxReading.pulseRate} BPM")
                    callback?.onPublishSuccess(topic)
                }
            }
        } catch (e: Exception) {
            val errorMsg = "Error publishing pulse oximetry reading: ${e.message}"
            Log.e(TAG, errorMsg, e)
            callback?.onPublishFailed("pulse_oximetry", errorMsg)
        }
    }
    
    private fun sanitizeEmailForTopic(email: String): String {
        return email.replace("@", "_at_").replace(".", "_")
    }
    
    fun getConnectionInfo(): String {
        return "Connected: ${isConnected()}, Broker: ${config.brokerHost}:${config.brokerPort}"
    }
}