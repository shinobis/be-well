package com.bewell.contracts.data

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class Sex {
    MALE,
    FEMALE,
    INTERSEX
}