package com.healthdata.mqtt.data

import kotlin.math.floor

data class BodyComposition(
    val weight: Double,
    val height: Int,
    val age: Int,
    val sex: Sex,
    val impedance: Double,
    val bmi: Double,
    val fatPercentage: Double,
    val waterPercentage: Double,
    val boneMass: Double,
    val muscleMass: Double,
    val visceralFat: Double,
    val bmr: Double,
    val proteinPercentage: Double,
    val bodyType: Int,
    val metabolicAge: Double,
    val idealWeight: Double,
    val fatMassToIdeal: String
)

class BodyMetrics(
    private val weight: Double,
    private val height: Int,
    private val age: Int,
    private val sex: Sex,
    private val impedance: Double
) {
    private val scales = BodyScales(age, height, sex, weight)
    
    init {
        validateInputs()
    }
    
    private fun validateInputs() {
        when {
            height > 220 -> throw IllegalArgumentException("Height is too high (limit: >220cm) or scale is sleeping")
            weight < 10 || weight > 200 -> throw IllegalArgumentException("Weight is either too low or too high (limits: <10kg and >200kg)")
            age > 99 -> throw IllegalArgumentException("Age is too high (limit >99 years)")
            impedance > 3000 -> throw IllegalArgumentException("Impedance is above 3000 Ohm")
        }
    }
    
    // Set the value to a boundary if it overflows
    private fun checkValueOverflow(value: Double, minimum: Double, maximum: Double): Double {
        return when {
            value < minimum -> minimum
            value > maximum -> maximum
            else -> value
        }
    }
    
    // Get LBM coefficient (with impedance)
    private fun getLBMCoefficient(): Double {
        var lbm = (height * 9.058 / 100) * (height / 100.0)
        lbm += weight * 0.32 + 12.226
        lbm -= impedance * 0.0068
        lbm -= age * 0.0542
        return lbm
    }
    
    // Get BMR
    fun getBMR(): Double {
        val bmr = if (sex == Sex.FEMALE) {
            var bmr = 864.6 + weight * 10.2036
            bmr -= height * 0.39336
            bmr -= age * 6.204
            
            // Capping
            if (bmr > 2996) 5000.0 else bmr
        } else {
            var bmr = 877.8 + weight * 14.916
            bmr -= height * 0.726
            bmr -= age * 8.976
            
            // Capping
            if (bmr > 2322) 5000.0 else bmr
        }
        
        return checkValueOverflow(bmr, 500.0, 10000.0)
    }
    
    // Get fat percentage
    fun getFatPercentage(): Double {
        // Set a constant to remove from LBM
        val const = when {
            sex == Sex.FEMALE && age <= 49 -> 9.25
            sex == Sex.FEMALE && age > 49 -> 7.25
            else -> 0.8
        }
        
        // Calculate body fat percentage
        val lbm = getLBMCoefficient()
        
        val coefficient = when {
            sex == Sex.MALE && weight < 61 -> 0.98
            sex == Sex.FEMALE && weight > 60 -> {
                var coeff = 0.96
                if (height > 160) coeff *= 1.03
                coeff
            }
            sex == Sex.FEMALE && weight < 50 -> {
                var coeff = 1.02
                if (height > 160) coeff *= 1.03
                coeff
            }
            else -> 1.0
        }
        
        var fatPercentage = (1.0 - (((lbm - const) * coefficient) / weight)) * 100
        
        // Capping body fat percentage
        if (fatPercentage > 63) {
            fatPercentage = 75.0
        }
        
        return checkValueOverflow(fatPercentage, 5.0, 75.0)
    }
    
    // Get water percentage
    fun getWaterPercentage(): Double {
        var waterPercentage = (100 - getFatPercentage()) * 0.7
        
        val coefficient = if (waterPercentage <= 50) 1.02 else 0.98
        
        // Capping water percentage
        if (waterPercentage * coefficient >= 65) {
            waterPercentage = 75.0
        } else {
            waterPercentage *= coefficient
        }
        
        return checkValueOverflow(waterPercentage, 35.0, 75.0)
    }
    
    // Get bone mass
    fun getBoneMass(): Double {
        val base = if (sex == Sex.FEMALE) 0.245691014 else 0.18016894
        
        var boneMass = (base - (getLBMCoefficient() * 0.05158)) * -1
        
        if (boneMass > 2.2) {
            boneMass += 0.1
        } else {
            boneMass -= 0.1
        }
        
        // Capping boneMass
        when {
            sex == Sex.FEMALE && boneMass > 5.1 -> boneMass = 8.0
            sex == Sex.MALE && boneMass > 5.2 -> boneMass = 8.0
        }
        
        return checkValueOverflow(boneMass, 0.5, 8.0)
    }
    
    // Get muscle mass
    fun getMuscleMass(): Double {
        var muscleMass = weight - ((getFatPercentage() * 0.01) * weight) - getBoneMass()
        
        // Capping muscle mass
        when {
            sex == Sex.FEMALE && muscleMass >= 84 -> muscleMass = 120.0
            sex == Sex.MALE && muscleMass >= 93.5 -> muscleMass = 120.0
        }
        
        return checkValueOverflow(muscleMass, 10.0, 120.0)
    }
    
    // Get Visceral Fat
    fun getVisceralFat(): Double {
        val vfal = if (sex == Sex.FEMALE) {
            if (weight > (13 - (height * 0.5)) * -1) {
                val subsubcalc = ((height * 1.45) + (height * 0.1158) * height) - 120
                val subcalc = weight * 500 / subsubcalc
                (subcalc - 6) + (age * 0.07)
            } else {
                val subcalc = 0.691 + (height * -0.0024) + (height * -0.0024)
                (((height * 0.027) - (subcalc * weight)) * -1) + (age * 0.07) - age
            }
        } else {
            if (height < weight * 1.6) {
                val subcalc = ((height * 0.4) - (height * (height * 0.0826))) * -1
                ((weight * 305) / (subcalc + 48)) - 2.9 + (age * 0.15)
            } else {
                val subcalc = 0.765 + height * -0.0015
                (((height * 0.143) - (weight * subcalc)) * -1) + (age * 0.15) - 5.0
            }
        }
        
        return checkValueOverflow(vfal, 1.0, 50.0)
    }
    
    // Get BMI
    fun getBMI(): Double {
        return checkValueOverflow(weight / ((height / 100.0) * (height / 100.0)), 10.0, 90.0)
    }
    
    // Get ideal weight
    fun getIdealWeight(original: Boolean = true): Double {
        return if (original) {
            when (sex) {
                Sex.FEMALE -> (height - 70) * 0.6
                Sex.MALE -> (height - 80) * 0.7
            }
        } else {
            checkValueOverflow((22 * height) * height / 10000.0, 5.5, 198.0)
        }
    }
    
    // Get fat mass to ideal
    fun getFatMassToIdeal(): String {
        val fatScales = scales.getFatPercentageScale()
        if (fatScales.size < 3) return "unknown"
        
        val mass = (weight * (getFatPercentage() / 100)) - (weight * (fatScales[2] / 100))
        return if (mass < 0) {
            "to_gain:${String.format("%.1f", mass * -1)}"
        } else {
            "to_lose:${String.format("%.1f", mass)}"
        }
    }
    
    // Get protein percentage
    fun getProteinPercentage(original: Boolean = true): Double {
        val proteinPercentage = if (original) {
            var protein = (getMuscleMass() / weight) * 100
            protein -= getWaterPercentage()
            protein
        } else {
            var protein = 100 - (floor(getFatPercentage() * 100) / 100)
            protein -= floor(getWaterPercentage() * 100) / 100
            protein -= floor((getBoneMass() / weight * 100) * 100) / 100
            protein
        }
        
        return checkValueOverflow(proteinPercentage, 5.0, 32.0)
    }
    
    // Get body type (out of nine possible)
    fun getBodyType(): Int {
        val fatScales = scales.getFatPercentageScale()
        val muscleScales = scales.getMuscleMassScale()
        
        if (fatScales.size < 3 || muscleScales.size < 2) return 5 // default balanced
        
        val factor = when {
            getFatPercentage() > fatScales[2] -> 0
            getFatPercentage() < fatScales[1] -> 2
            else -> 1
        }
        
        return when {
            getMuscleMass() > muscleScales[1] -> 3 + (factor * 3)
            getMuscleMass() < muscleScales[0] -> 1 + (factor * 3)
            else -> 2 + (factor * 3)
        }
    }
    
    // Get Metabolic Age
    fun getMetabolicAge(): Double {
        val metabolicAge = if (sex == Sex.FEMALE) {
            (height * -1.1165) + (weight * 1.5784) + (age * 0.4615) + (impedance * 0.0415) + 83.2548
        } else {
            (height * -0.7471) + (weight * 0.9161) + (age * 0.4184) + (impedance * 0.0517) + 54.2267
        }
        
        return checkValueOverflow(metabolicAge, 15.0, 80.0)
    }
    
    // Get complete body composition
    fun getBodyComposition(): BodyComposition {
        return BodyComposition(
            weight = weight,
            height = height,
            age = age,
            sex = sex,
            impedance = impedance,
            bmi = getBMI(),
            fatPercentage = getFatPercentage(),
            waterPercentage = getWaterPercentage(),
            boneMass = getBoneMass(),
            muscleMass = getMuscleMass(),
            visceralFat = getVisceralFat(),
            bmr = getBMR(),
            proteinPercentage = getProteinPercentage(),
            bodyType = getBodyType(),
            metabolicAge = getMetabolicAge(),
            idealWeight = getIdealWeight(),
            fatMassToIdeal = getFatMassToIdeal()
        )
    }
}