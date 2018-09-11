package com.bewell.flows

import net.corda.core.flows.FlowException

class WellnessFlowException(message: String, cause: Throwable? = null) : FlowException(message, cause)