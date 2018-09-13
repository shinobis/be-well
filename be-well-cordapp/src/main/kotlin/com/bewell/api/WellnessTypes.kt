package com.bewell.api

import com.bewell.contracts.data.Basics
import com.bewell.contracts.data.Diet
import com.bewell.contracts.data.Sex
import com.bewell.contracts.data.WellnessDetails

class WellnessData(
        val sex: String,
        val age: Int,
        val height: Int = 0,
        val weight: Int = 0,
        val heartRate: Int = 0,
        val vegetables: Int = 0,
        val fruits: Int = 0,
        val meat: Int = 0,
        val exercise: Float = 0f,
        val sleep: Float = 0f
) {
    fun toWellnessDetails() : WellnessDetails {
        val sexEnumValue = Sex.valueOf(sex.toUpperCase())
        return WellnessDetails(Basics(sexEnumValue, age, height, weight, heartRate),
                Diet(vegetables, fruits, meat), exercise, sleep)
    }
}

class RegistrationData(val provider: String, val wellnessData: WellnessData)

class AccountData(val accountId: String, val wellnessData: WellnessData)