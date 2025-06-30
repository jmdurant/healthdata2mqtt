package com.healthdata.mqtt.data

import java.util.*

enum class OximetryQuality {
    EXCELLENT, GOOD, FAIR, POOR, NO_SIGNAL, UNKNOWN
}

data class PulseOximetryReading(
    val spo2Percentage: Int,           // SpO2 saturation percentage (0-100)
    val pulseRate: Int,                // Heart rate in BPM
    val plethysmogramData: ByteArray,  // Raw PPG waveform data
    val timestamp: Date,
    val signalQuality: OximetryQuality = OximetryQuality.UNKNOWN,
    val deviceAddress: String,
    val deviceName: String = "OxySmart",
    val isValidReading: Boolean = true,
    val batteryLevel: Int? = null
) {
    companion object {
        fun createFromBleData(
            rawData: ByteArray,
            deviceAddress: String,
            deviceName: String = "OxySmart"
        ): PulseOximetryReading? {
            return try {
                // Validate packet structure (11 bytes with header)
                if (rawData.size != 11) return null
                
                // Check for expected header: \xaaU\x0f\x07\x02
                val expectedHeader = byteArrayOf(0xAA.toByte(), 0x55, 0x0F, 0x07, 0x02)
                if (!rawData.sliceArray(0..4).contentEquals(expectedHeader)) return null
                
                // Extract plethysmogram data (bytes 5-9)
                val plethData = rawData.sliceArray(5..9)
                
                // Process plethysmogram data for systolic peaks
                val processedPlethData = plethData.map { byte ->
                    val intValue = byte.toInt() and 0xFF
                    if (intValue > 127) (intValue - 128).toByte() else intValue.toByte()
                }.toByteArray()
                
                // Extract SpO2 and pulse rate from last byte (placeholder implementation)
                val lastByte = rawData[10].toInt() and 0xFF
                
                // Simple extraction - this would need refinement based on actual device protocol
                val spo2 = if (lastByte > 0) (lastByte and 0x7F) + 70 else 0  // Typical SpO2 range 70-100
                val pulseRate = if (lastByte > 0) ((lastByte and 0xF0) shr 4) * 10 + 60 else 0  // Typical HR range 60-150
                
                // Determine signal quality based on data variability
                val signalQuality = determineSignalQuality(processedPlethData)
                
                PulseOximetryReading(
                    spo2Percentage = spo2.coerceIn(0, 100),
                    pulseRate = pulseRate.coerceIn(0, 200),
                    plethysmogramData = processedPlethData,
                    timestamp = Date(),
                    signalQuality = signalQuality,
                    deviceAddress = deviceAddress,
                    deviceName = deviceName,
                    isValidReading = spo2 > 0 && pulseRate > 0
                )
            } catch (e: Exception) {
                null
            }
        }
        
        private fun determineSignalQuality(plethData: ByteArray): OximetryQuality {
            if (plethData.isEmpty()) return OximetryQuality.NO_SIGNAL
            
            // Calculate signal variability as quality indicator
            val values = plethData.map { it.toInt() and 0xFF }
            val mean = values.average()
            val variance = values.map { (it - mean) * (it - mean) }.average()
            val stdDev = kotlin.math.sqrt(variance)
            
            return when {
                stdDev > 20 -> OximetryQuality.EXCELLENT
                stdDev > 15 -> OximetryQuality.GOOD
                stdDev > 10 -> OximetryQuality.FAIR
                stdDev > 5 -> OximetryQuality.POOR
                else -> OximetryQuality.NO_SIGNAL
            }
        }
        
        fun isValidSpo2(spo2: Int): Boolean = spo2 in 70..100
        
        fun isValidPulseRate(pulseRate: Int): Boolean = pulseRate in 40..200
        
        fun getSpo2Category(spo2: Int): String {
            return when {
                spo2 >= 95 -> "Normal"
                spo2 >= 90 -> "Acceptable" 
                spo2 >= 85 -> "Low"
                spo2 > 0 -> "Critical"
                else -> "Unknown"
            }
        }
        
        fun getPulseRateCategory(pulseRate: Int): String {
            return when {
                pulseRate < 60 -> "Bradycardia"
                pulseRate <= 100 -> "Normal"
                pulseRate <= 150 -> "Tachycardia"
                pulseRate > 150 -> "Severe Tachycardia"
                else -> "Unknown"
            }
        }
    }
    
    fun getSpo2CategoryString(): String = getSpo2Category(spo2Percentage)
    
    fun getPulseRateCategoryString(): String = getPulseRateCategory(pulseRate)
    
    fun toSummaryString(): String {
        return "SpO2: ${spo2Percentage}% (${getSpo2CategoryString()}), " +
                "Pulse: ${pulseRate} BPM (${getPulseRateCategoryString()}), " +
                "Quality: ${signalQuality.name}"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as PulseOximetryReading
        
        if (spo2Percentage != other.spo2Percentage) return false
        if (pulseRate != other.pulseRate) return false
        if (!plethysmogramData.contentEquals(other.plethysmogramData)) return false
        if (timestamp != other.timestamp) return false
        if (signalQuality != other.signalQuality) return false
        if (deviceAddress != other.deviceAddress) return false
        if (deviceName != other.deviceName) return false
        if (isValidReading != other.isValidReading) return false
        if (batteryLevel != other.batteryLevel) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = spo2Percentage
        result = 31 * result + pulseRate
        result = 31 * result + plethysmogramData.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + signalQuality.hashCode()
        result = 31 * result + deviceAddress.hashCode()
        result = 31 * result + deviceName.hashCode()
        result = 31 * result + isValidReading.hashCode()
        result = 31 * result + (batteryLevel ?: 0)
        return result
    }
}