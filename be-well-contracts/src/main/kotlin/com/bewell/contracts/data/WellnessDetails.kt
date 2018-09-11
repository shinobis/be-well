package com.bewell.contracts.data

import net.corda.core.serialization.CordaSerializable

/**
 * General parameters to represent wellness of a person.
 * @param basics of type [Basics]
 * @param diet of type [Diet]
 * @param exercise minutes per day
 * @param sleep in hours per day
 */
@CordaSerializable
data class WellnessDetails(
        val basics: Basics,
        val diet: Diet = Diet(),
        val exercise: Float = 0.0f,
        val sleep: Float = 0.0f)