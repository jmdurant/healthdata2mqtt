package com.healthdata.mqtt.data

import java.util.Date

data class TemperatureReading(
    val temperatureCelsius: Double,
    val temperatureFahrenheit: Double,
    val timestamp: Date,
    val measurementLocation: MeasurementLocation = MeasurementLocation.UNKNOWN,
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isValid: Boolean = true,
    val deviceAddress: String = "",
    val deviceName: String = ""
) {
    companion object {
        fun fromCelsius(celsius: Double, location: MeasurementLocation = MeasurementLocation.UNKNOWN): TemperatureReading {
            return TemperatureReading(
                temperatureCelsius = celsius,
                temperatureFahrenheit = celsiusToFahrenheit(celsius),
                timestamp = Date(),
                measurementLocation = location,
                unit = TemperatureUnit.CELSIUS
            )
        }
        
        fun fromFahrenheit(fahrenheit: Double, location: MeasurementLocation = MeasurementLocation.UNKNOWN): TemperatureReading {
            return TemperatureReading(
                temperatureCelsius = fahrenheitToCelsius(fahrenheit),
                temperatureFahrenheit = fahrenheit,
                timestamp = Date(),
                measurementLocation = location,
                unit = TemperatureUnit.FAHRENHEIT
            )
        }
        
        private fun celsiusToFahrenheit(celsius: Double): Double {
            return (celsius * 9.0 / 5.0) + 32.0
        }
        
        private fun fahrenheitToCelsius(fahrenheit: Double): Double {
            return (fahrenheit - 32.0) * 5.0 / 9.0
        }
    }
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

enum class MeasurementLocation {
    UNKNOWN,
    BODY,
    FOREHEAD,
    EAR,
    MOUTH,
    RECTUM,
    ARMPIT,
    OBJECT,
    ROOM_AMBIENT
}