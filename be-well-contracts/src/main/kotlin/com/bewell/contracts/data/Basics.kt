package com.bewell.contracts.data

import net.corda.core.serialization.CordaSerializable

/**
 * @param sex male, female or intersex
 * @param age in years
 * @param height in centimeters
 * @param weight in kilograms
 * @param heartRate average rate in beats per minute
 */
@CordaSerializable
data class Basics(val sex: Sex, val age: Int, val height: Int = 0, val weight: Int = 0, val heartRate: Int = 0)