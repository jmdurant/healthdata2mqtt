package com.healthdata.mqtt.data

import android.content.Context
import android.content.SharedPreferences
import com.healthdata.mqtt.service.MQTTConfig

class AppPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "health_data_mqtt_prefs"
        
        // MQTT Settings
        private const val KEY_MQTT_BROKER_MODE = "mqtt_broker_mode"
        private const val KEY_MQTT_HOST = "mqtt_host"
        private const val KEY_MQTT_PORT = "mqtt_port"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD = "mqtt_password"
        private const val KEY_MQTT_CLIENT_ID = "mqtt_client_id"
        
        // User Profile
        private const val KEY_USER_FIRST_NAME = "user_first_name"
        private const val KEY_USER_LAST_NAME = "user_last_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DATE_OF_BIRTH = "user_date_of_birth"
        private const val KEY_USER_HEIGHT = "user_height"
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_SEX = "user_sex"
        
        // Blood Pressure Settings
        private const val KEY_BP_STANDARD = "bp_standard"
        
        // App Settings
        private const val KEY_AUTO_SCAN = "auto_scan"
        private const val KEY_BACKGROUND_SCAN = "background_scan"
        private const val KEY_PUBLISH_RAW_DATA = "publish_raw_data"
        
        // Default values
        private const val DEFAULT_MQTT_HOST = "localhost"
        private const val DEFAULT_MQTT_PORT = 1883
        private const val DEFAULT_HEIGHT = 170
        private const val DEFAULT_AGE = 30
        private const val DEFAULT_SEX = "male"
        private const val DEFAULT_BP_STANDARD = "eu"
    }
    
    // MQTT Configuration
    var mqttBrokerMode: String
        get() = prefs.getString(KEY_MQTT_BROKER_MODE, "internal")!!
        set(value) = prefs.edit().putString(KEY_MQTT_BROKER_MODE, value).apply()
    
    var mqttHost: String
        get() = prefs.getString(KEY_MQTT_HOST, DEFAULT_MQTT_HOST)!!
        set(value) = prefs.edit().putString(KEY_MQTT_HOST, value).apply()
    
    var mqttPort: Int
        get() = prefs.getInt(KEY_MQTT_PORT, DEFAULT_MQTT_PORT)
        set(value) = prefs.edit().putInt(KEY_MQTT_PORT, value).apply()
    
    var mqttUsername: String?
        get() = prefs.getString(KEY_MQTT_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_MQTT_USERNAME, value).apply()
    
    var mqttPassword: String?
        get() = prefs.getString(KEY_MQTT_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_MQTT_PASSWORD, value).apply()
    
    var mqttClientId: String
        get() = prefs.getString(KEY_MQTT_CLIENT_ID, "android_health_client_${System.currentTimeMillis()}")!!
        set(value) = prefs.edit().putString(KEY_MQTT_CLIENT_ID, value).apply()
    
    // User Profile
    var userFirstName: String?
        get() = prefs.getString(KEY_USER_FIRST_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_FIRST_NAME, value).apply()
    
    var userLastName: String?
        get() = prefs.getString(KEY_USER_LAST_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_LAST_NAME, value).apply()
    
    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()
    
    var userDateOfBirth: String?
        get() = prefs.getString(KEY_USER_DATE_OF_BIRTH, null)
        set(value) = prefs.edit().putString(KEY_USER_DATE_OF_BIRTH, value).apply()
    
    var userHeight: Int
        get() = prefs.getInt(KEY_USER_HEIGHT, DEFAULT_HEIGHT)
        set(value) = prefs.edit().putInt(KEY_USER_HEIGHT, value).apply()
    
    var userAge: Int
        get() = prefs.getInt(KEY_USER_AGE, DEFAULT_AGE)
        set(value) = prefs.edit().putInt(KEY_USER_AGE, value).apply()
    
    var userSex: String
        get() = prefs.getString(KEY_USER_SEX, DEFAULT_SEX)!!
        set(value) = prefs.edit().putString(KEY_USER_SEX, value).apply()
    
    // Blood Pressure Standard
    var bloodPressureStandard: String
        get() = prefs.getString(KEY_BP_STANDARD, DEFAULT_BP_STANDARD)!!
        set(value) = prefs.edit().putString(KEY_BP_STANDARD, value).apply()
    
    // App Settings
    var autoScan: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCAN, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCAN, value).apply()
    
    var backgroundScan: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_SCAN, true)
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_SCAN, value).apply()
    
    var publishRawData: Boolean
        get() = prefs.getBoolean(KEY_PUBLISH_RAW_DATA, true)
        set(value) = prefs.edit().putBoolean(KEY_PUBLISH_RAW_DATA, value).apply()
    
    // Convenience methods
    fun getMQTTConfig(): MQTTConfig {
        return MQTTConfig(
            brokerHost = mqttHost,
            brokerPort = mqttPort,
            username = mqttUsername,
            password = mqttPassword,
            clientId = mqttClientId
        )
    }
    
    fun getUserSex(): Sex {
        return when (userSex.lowercase()) {
            "female" -> Sex.FEMALE
            else -> Sex.MALE
        }
    }
    
    fun getBloodPressureStandard(): BloodPressureStandard {
        return when (bloodPressureStandard.lowercase()) {
            "us" -> BloodPressureStandard.US
            else -> BloodPressureStandard.EU
        }
    }
    
    fun isUserProfileComplete(): Boolean {
        return !userFirstName.isNullOrBlank() && 
               !userLastName.isNullOrBlank() && 
               !userEmail.isNullOrBlank() && 
               !userDateOfBirth.isNullOrBlank() &&
               userHeight > 0 && userAge > 0
    }
    
    fun getFullName(): String {
        val firstName = userFirstName ?: ""
        val lastName = userLastName ?: ""
        return "$firstName $lastName".trim()
    }
    
    fun calculateAgeFromDateOfBirth(): Int? {
        return userDateOfBirth?.let { dateString ->
            try {
                // Parse date format YYYY-MM-DD
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val birthYear = parts[0].toInt()
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    currentYear - birthYear
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun isUsingInternalBroker(): Boolean {
        return mqttBrokerMode == "internal"
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    fun exportSettings(): Map<String, Any> {
        return mapOf(
            "mqtt_host" to mqttHost,
            "mqtt_port" to mqttPort,
            "mqtt_username" to (mqttUsername ?: ""),
            "user_email" to (userEmail ?: ""),
            "user_height" to userHeight,
            "user_age" to userAge,
            "user_sex" to userSex,
            "bp_standard" to bloodPressureStandard,
            "auto_scan" to autoScan,
            "background_scan" to backgroundScan,
            "publish_raw_data" to publishRawData
        )
    }
}