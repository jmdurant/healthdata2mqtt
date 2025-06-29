package com.healthdata.mqtt.data

enum class BloodPressureStandard {
    EU, US
}

enum class BloodPressureCategory {
    NORMAL, HIGH_NORMAL, GRADE_1, GRADE_2, UNKNOWN;
    
    override fun toString(): String = when(this) {
        NORMAL -> "Normal"
        HIGH_NORMAL -> "High-Normal"
        GRADE_1 -> "Grade_1"
        GRADE_2 -> "Grade_2"
        UNKNOWN -> "Unknown"
    }
}

data class BloodPressureReading(
    val timestamp: Long,
    val date: String,
    val time: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val mov: Int, // Movement during measurement
    val ihb: Int, // Irregular heartbeat indicator
    val userEmail: String,
    val category: BloodPressureCategory
)

class BloodPressureMetrics {
    
    companion object {
        /**
         * Categorizes blood pressure reading based on systolic and diastolic values
         * @param systolic Systolic pressure in mmHg
         * @param diastolic Diastolic pressure in mmHg
         * @param standard Either EU or US guidelines
         * @return BloodPressureCategory
         */
        fun categorizeBloodPressure(
            systolic: Int, 
            diastolic: Int, 
            standard: BloodPressureStandard = BloodPressureStandard.EU
        ): BloodPressureCategory {
            return when (standard) {
                BloodPressureStandard.EU -> {
                    when {
                        systolic < 130 && diastolic < 85 -> BloodPressureCategory.NORMAL
                        (systolic in 130..139 && diastolic < 85) || (systolic < 130 && diastolic in 85..89) -> 
                            BloodPressureCategory.HIGH_NORMAL
                        (systolic in 140..159 && diastolic < 90) || (systolic < 140 && diastolic in 90..99) -> 
                            BloodPressureCategory.GRADE_1
                        (systolic in 160..179 && diastolic < 100) || (systolic < 160 && diastolic in 100..109) -> 
                            BloodPressureCategory.GRADE_2
                        systolic >= 180 || diastolic >= 110 -> BloodPressureCategory.GRADE_2
                        else -> BloodPressureCategory.UNKNOWN
                    }
                }
                BloodPressureStandard.US -> {
                    when {
                        systolic < 120 && diastolic < 80 -> BloodPressureCategory.NORMAL
                        systolic in 120..129 && diastolic < 80 -> BloodPressureCategory.HIGH_NORMAL
                        (systolic in 130..139) || (diastolic in 80..89) -> BloodPressureCategory.GRADE_1
                        systolic >= 140 || diastolic >= 90 -> BloodPressureCategory.GRADE_2
                        else -> BloodPressureCategory.UNKNOWN
                    }
                }
            }
        }
        
        /**
         * Creates a complete blood pressure reading with calculated category
         */
        fun createBloodPressureReading(
            timestamp: Long,
            date: String,
            time: String,
            systolic: Int,
            diastolic: Int,
            pulse: Int,
            mov: Int = 0,
            ihb: Int = 0,
            userEmail: String,
            standard: BloodPressureStandard = BloodPressureStandard.EU
        ): BloodPressureReading {
            val category = categorizeBloodPressure(systolic, diastolic, standard)
            
            return BloodPressureReading(
                timestamp = timestamp,
                date = date,
                time = time,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                mov = mov,
                ihb = ihb,
                userEmail = userEmail,
                category = category
            )
        }
        
        /**
         * Validates blood pressure values
         */
        fun validateBloodPressure(systolic: Int, diastolic: Int, pulse: Int): Boolean {
            return systolic in 70..300 && 
                   diastolic in 40..200 && 
                   pulse in 30..200 &&
                   systolic > diastolic
        }
        
        /**
         * Provides health recommendations based on category
         */
        fun getHealthRecommendation(category: BloodPressureCategory): String {
            return when (category) {
                BloodPressureCategory.NORMAL -> 
                    "Your blood pressure is in the normal range. Maintain a healthy lifestyle."
                BloodPressureCategory.HIGH_NORMAL -> 
                    "Your blood pressure is high-normal. Consider lifestyle modifications and monitor regularly."
                BloodPressureCategory.GRADE_1 -> 
                    "Stage 1 hypertension detected. Consult your healthcare provider and consider lifestyle changes."
                BloodPressureCategory.GRADE_2 -> 
                    "Stage 2 hypertension detected. Seek immediate medical attention and treatment."
                BloodPressureCategory.UNKNOWN -> 
                    "Unable to categorize reading. Please verify measurements and consult healthcare provider."
            }
        }
        
        /**
         * Checks if reading indicates movement during measurement
         */
        fun hasMovementError(mov: Int): Boolean = mov != 0
        
        /**
         * Checks if reading indicates irregular heartbeat
         */
        fun hasIrregularHeartbeat(ihb: Int): Boolean = ihb != 0
        
        /**
         * Gets risk level based on category
         */
        fun getRiskLevel(category: BloodPressureCategory): String {
            return when (category) {
                BloodPressureCategory.NORMAL -> "Low"
                BloodPressureCategory.HIGH_NORMAL -> "Low-Medium"
                BloodPressureCategory.GRADE_1 -> "Medium"
                BloodPressureCategory.GRADE_2 -> "High"
                BloodPressureCategory.UNKNOWN -> "Unknown"
            }
        }
    }
}