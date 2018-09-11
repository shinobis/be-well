package com.bewell.flows

import co.paralleluniverse.fibers.Suspendable
import com.bewell.contracts.Wellness
import com.bewell.contracts.data.WellnessDetails
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Flow to update one or more details on the wellness state.
 */
@StartableByRPC
class UpdateWellnessFlow(val stateRef: StateRef, val updatedDetails: WellnessDetails) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val stateAndRef = try {
             serviceHub.toStateAndRef<Wellness.State>(stateRef)
        } catch (e: TransactionResolutionException) {
            throw WellnessFlowException("Wellness state could not be found.", e)
        }

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val signer = Wellness.generateUpdate(builder, stateAndRef, updatedDetails, notary)

        val tx = serviceHub.signInitialTransaction(builder, signer)
        val finalizedTx = subFlow(FinalityFlow(tx))

        return finalizedTx
    }
}