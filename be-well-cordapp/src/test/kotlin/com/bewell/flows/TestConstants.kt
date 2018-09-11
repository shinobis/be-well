package com.bewell.flows

import net.corda.core.identity.CordaX500Name

val CONTRACT_PACKAGES = listOf("com.bewell.contracts", "com.bewell.schemas")

val BEWELL_USERS_NAME = CordaX500Name(commonName = "Be Well", organisation = "Be Well Inc", locality = "New York", country = "US")
val MIRANDA_WELLNESS_NAME = CordaX500Name(commonName = "First Wellness", organisation = "First Wellness Inc", locality = "New York", country = "US")