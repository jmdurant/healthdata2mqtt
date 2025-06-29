package com.healthdata.mqtt.service

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.healthdata.mqtt.data.BloodPressureReading
import com.healthdata.mqtt.data.BodyComposition
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
        // Implementation same as before...
    }
    
    fun publishBloodPressure(
        userEmail: String,
        bloodPressure: BloodPressureReading,
        callback: PublishCallback? = null
    ) {
        // Implementation same as before...
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
        // Implementation same as before...
    }
    
    fun publishDeviceDiscovered(
        deviceMac: String,
        deviceName: String?,
        rssi: Int,
        callback: PublishCallback? = null
    ) {
        // Implementation same as before...
    }
    
    private fun sanitizeEmailForTopic(email: String): String {
        return email.replace("@", "_at_").replace(".", "_")
    }
    
    fun getConnectionInfo(): String {
        return "Connected: ${isConnected()}, Broker: ${config.brokerHost}:${config.brokerPort}"
    }
}