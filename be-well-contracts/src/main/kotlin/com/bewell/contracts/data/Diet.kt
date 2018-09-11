package com.bewell.contracts.data

import net.corda.core.serialization.CordaSerializable

/**
 * Basic measures of regular diet.
 * @param vegetables in cups per day
 * @param fruits in cups per day
 * @param meat in grams per day
 */
@CordaSerializable
data class Diet(val vegetables: Int = 0, val fruits: Int = 0, val meat: Int = 0)