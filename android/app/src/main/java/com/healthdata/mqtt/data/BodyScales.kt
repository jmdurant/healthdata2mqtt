package com.healthdata.mqtt.data

data class ScaleRange(
    val min: Int,
    val max: Int,
    val female: List<Double>,
    val male: List<Double>
)

data class MuscleMassScale(
    val minHeight: Map<String, Int>,
    val female: List<Double>,
    val male: List<Double>
)

data class BoneMassScale(
    val minWeight: Map<String, Double>,
    val female: List<Double>,
    val male: List<Double>
)

enum class ScaleType {
    XIAOMI, HOLTEK
}

enum class Sex {
    MALE, FEMALE;
    
    override fun toString(): String = name.lowercase()
}

class BodyScales(
    private val age: Int,
    private val height: Int,
    private val sex: Sex,
    private val weight: Double,
    private val scaleType: ScaleType = ScaleType.XIAOMI
) {
    
    // Get BMI scale
    fun getBMIScale(): List<Double> {
        return when (scaleType) {
            ScaleType.XIAOMI -> listOf(18.5, 25.0, 28.0, 32.0)
            ScaleType.HOLTEK -> listOf(18.5, 25.0, 30.0)
        }
    }
    
    // Get fat percentage scale
    fun getFatPercentageScale(): List<Double> {
        val scales = when (scaleType) {
            ScaleType.XIAOMI -> listOf(
                ScaleRange(0, 12, listOf(12.0, 21.0, 30.0, 34.0), listOf(7.0, 16.0, 25.0, 30.0)),
                ScaleRange(12, 14, listOf(15.0, 24.0, 33.0, 37.0), listOf(7.0, 16.0, 25.0, 30.0)),
                ScaleRange(14, 16, listOf(18.0, 27.0, 36.0, 40.0), listOf(7.0, 16.0, 25.0, 30.0)),
                ScaleRange(16, 18, listOf(20.0, 28.0, 37.0, 41.0), listOf(7.0, 16.0, 25.0, 30.0)),
                ScaleRange(18, 40, listOf(21.0, 28.0, 35.0, 40.0), listOf(11.0, 17.0, 22.0, 27.0)),
                ScaleRange(40, 60, listOf(22.0, 29.0, 36.0, 41.0), listOf(12.0, 18.0, 23.0, 28.0)),
                ScaleRange(60, 100, listOf(23.0, 30.0, 37.0, 42.0), listOf(14.0, 20.0, 25.0, 30.0))
            )
            ScaleType.HOLTEK -> listOf(
                ScaleRange(0, 21, listOf(18.0, 23.0, 30.0, 35.0), listOf(8.0, 14.0, 21.0, 25.0)),
                ScaleRange(21, 26, listOf(19.0, 24.0, 30.0, 35.0), listOf(10.0, 15.0, 22.0, 26.0)),
                ScaleRange(26, 31, listOf(20.0, 25.0, 31.0, 36.0), listOf(11.0, 16.0, 21.0, 27.0)),
                ScaleRange(31, 36, listOf(21.0, 26.0, 33.0, 36.0), listOf(13.0, 17.0, 25.0, 28.0)),
                ScaleRange(36, 41, listOf(22.0, 27.0, 34.0, 37.0), listOf(15.0, 20.0, 26.0, 29.0)),
                ScaleRange(41, 46, listOf(23.0, 28.0, 35.0, 38.0), listOf(16.0, 22.0, 27.0, 30.0)),
                ScaleRange(46, 51, listOf(24.0, 30.0, 36.0, 38.0), listOf(17.0, 23.0, 29.0, 31.0)),
                ScaleRange(51, 56, listOf(26.0, 31.0, 36.0, 39.0), listOf(19.0, 25.0, 30.0, 33.0)),
                ScaleRange(56, 100, listOf(27.0, 32.0, 37.0, 40.0), listOf(21.0, 26.0, 31.0, 34.0))
            )
        }
        
        return scales.find { age >= it.min && age < it.max }?.let {
            when (sex) {
                Sex.FEMALE -> it.female
                Sex.MALE -> it.male
            }
        } ?: emptyList()
    }
    
    // Get muscle mass scale
    fun getMuscleMassScale(): List<Double> {
        val scales = when (scaleType) {
            ScaleType.XIAOMI -> listOf(
                MuscleMassScale(mapOf("male" to 170, "female" to 160), listOf(36.5, 42.6), listOf(49.4, 59.5)),
                MuscleMassScale(mapOf("male" to 160, "female" to 150), listOf(32.9, 37.6), listOf(44.0, 52.5)),
                MuscleMassScale(mapOf("male" to 0, "female" to 0), listOf(29.1, 34.8), listOf(38.5, 46.6))
            )
            ScaleType.HOLTEK -> listOf(
                MuscleMassScale(mapOf("male" to 170, "female" to 170), listOf(36.5, 42.5), listOf(49.5, 59.4)),
                MuscleMassScale(mapOf("male" to 160, "female" to 160), listOf(32.9, 37.5), listOf(44.0, 52.4)),
                MuscleMassScale(mapOf("male" to 0, "female" to 0), listOf(29.1, 34.7), listOf(38.5, 46.5))
            )
        }
        
        return scales.find { height >= it.minHeight[sex.toString()]!! }?.let {
            when (sex) {
                Sex.FEMALE -> it.female
                Sex.MALE -> it.male
            }
        } ?: emptyList()
    }
    
    // Get water percentage scale
    fun getWaterPercentageScale(): List<Double> {
        return when (scaleType) {
            ScaleType.XIAOMI -> when (sex) {
                Sex.MALE -> listOf(55.0, 65.1)
                Sex.FEMALE -> listOf(45.0, 60.1)
            }
            ScaleType.HOLTEK -> listOf(53.0, 67.0)
        }
    }
    
    // Get visceral fat scale
    fun getVisceralFatScale(): List<Double> {
        return listOf(10.0, 15.0)
    }
    
    // Get bone mass scale
    fun getBoneMassScale(): List<Double> {
        return when (scaleType) {
            ScaleType.XIAOMI -> {
                val scales = listOf(
                    BoneMassScale(mapOf("male" to 75.0, "female" to 60.0), listOf(1.8, 3.9), listOf(2.0, 4.2)),
                    BoneMassScale(mapOf("male" to 60.0, "female" to 45.0), listOf(1.5, 3.8), listOf(1.9, 4.1)),
                    BoneMassScale(mapOf("male" to 0.0, "female" to 0.0), listOf(1.3, 3.6), listOf(1.6, 3.9))
                )
                
                scales.find { weight >= it.minWeight[sex.toString()]!! }?.let {
                    when (sex) {
                        Sex.FEMALE -> it.female
                        Sex.MALE -> it.male
                    }
                } ?: emptyList()
            }
            ScaleType.HOLTEK -> {
                val scales = listOf(
                    Triple(mapOf("female" to 60.0, "male" to 75.0), mapOf("female" to 2.5, "male" to 3.2), "high"),
                    Triple(mapOf("female" to 45.0, "male" to 69.0), mapOf("female" to 2.2, "male" to 2.9), "medium"),
                    Triple(mapOf("female" to 0.0, "male" to 0.0), mapOf("female" to 1.8, "male" to 2.5), "low")
                )
                
                scales.find { weight >= it.first[sex.toString()]!! }?.let { scale ->
                    val optimal = scale.second[sex.toString()]!!
                    listOf(optimal - 1, optimal + 1)
                } ?: emptyList()
            }
        }
    }
    
    // Get BMR scale
    fun getBMRScale(): List<Double> {
        val coefficients = when (scaleType) {
            ScaleType.XIAOMI -> when (sex) {
                Sex.MALE -> mapOf(30 to 21.6, 50 to 20.07, 100 to 19.35)
                Sex.FEMALE -> mapOf(30 to 21.24, 50 to 19.53, 100 to 18.63)
            }
            ScaleType.HOLTEK -> when (sex) {
                Sex.FEMALE -> mapOf(12 to 34.0, 15 to 29.0, 17 to 24.0, 29 to 22.0, 50 to 20.0, 120 to 19.0)
                Sex.MALE -> mapOf(12 to 36.0, 15 to 30.0, 17 to 26.0, 29 to 23.0, 50 to 21.0, 120 to 20.0)
            }
        }
        
        return coefficients.entries.find { age < it.key }?.let { 
            listOf(weight * it.value) 
        } ?: emptyList()
    }
    
    // Get protein scale
    fun getProteinPercentageScale(): List<Double> {
        return listOf(16.0, 20.0)
    }
    
    // Get ideal weight scale
    fun getIdealWeightScale(): List<Double> {
        return getBMIScale().map { bmi ->
            (bmi * height) * height / 10000
        }
    }
    
    // Get body score scale
    fun getBodyScoreScale(): List<Double> {
        return listOf(50.0, 60.0, 80.0, 90.0)
    }
    
    // Get body type scale
    fun getBodyTypeScale(): List<String> {
        return listOf(
            "obese", "overweight", "thick-set", "lack-exercise", 
            "balanced", "balanced-muscular", "skinny", 
            "balanced-skinny", "skinny-muscular"
        )
    }
}