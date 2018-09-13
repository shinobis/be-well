package com.bewell.flows

import co.paralleluniverse.fibers.Suspendable
import com.bewell.contracts.Wellness
import com.bewell.schemas.WellnessSchemaV1
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignatureFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveStateAndRefFlow
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.BinaryComparisonOperator
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * This flow computes the wellness score for a monthly aggregate of wellness states for a user.
 * Given an account id, the broker flow retrieves the states for the user and reaches out to the
 * wellness provider to compute the wellness score. This is then used to generate an updated
 * wellness state for the user.
 */
object ScoreWellnessFlow {

    val HEALTHY_SCORE = 8

    @InitiatingFlow
    @StartableByRPC
    class BrokerFlow(val accountId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val wellnessStateAndRefs = serviceHub.accountExists(accountId)
            if (wellnessStateAndRefs.isEmpty())
                throw WellnessFlowException("Unknown account id.")

            val currentWellness = wellnessStateAndRefs.first()
            val recentWellness = serviceHub.retrieveWellness(accountId).plus(currentWellness)

            val provider = currentWellness.state.data.provider
            val providerSession = initiateFlow(provider)
            subFlow(SendStateAndRefFlow(providerSession, recentWellness))

            val receivedData = providerSession.receive<Int>()
            val score = receivedData.unwrap { it }

            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val builder = TransactionBuilder(notary)
            val signer = Wellness.generateScore(builder, currentWellness, score, notary)

            val tx = serviceHub.signInitialTransaction(builder, signer)
            val providerSignature = subFlow(CollectSignatureFlow(tx, providerSession, provider.owningKey))
            val signedTx = tx + providerSignature
            val finalizedTx = subFlow(FinalityFlow(signedTx))

            return finalizedTx
        }
    }

    @InitiatedBy(BrokerFlow::class)
    class ProviderFlow(val brokerSession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val resolvedStateAndRef = subFlow(ReceiveStateAndRefFlow<Wellness.State>(brokerSession))
            // Determine which scoring function should be chosen for the user
            val score = resolveClientScore(resolvedStateAndRef)
            brokerSession.send(score)

            val signTransactionFlow = object : SignTransactionFlow(brokerSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val states: Iterable<Wellness.State> = stx.tx.outputs.map { it.data as Wellness.State }
                    states.forEach { state ->
                        state.participants.forEach { anon ->
                            require(serviceHub.identityService.wellKnownPartyFromAnonymous(anon) != null) {
                                "Transaction state $state involves unknown participant $anon"
                            }
                        }
                        require(state.provider in serviceHub.myInfo.legalIdentities) {
                            "Incorrect provider ${state.provider} on transaction state $state"
                        }
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return waitForLedgerCommit(txId)
        }

        /**
         * Compute a score for each user - for simplicity returns a constant (healthy) score.
         */
        private fun resolveClientScore(recentWellness: List<StateAndRef<Wellness.State>>) : Int {
            val accountId = recentWellness.first().state.data.accountId
            requireThat {
                "all data belongs to single account" using
                        (recentWellness.filter { it.state.data.accountId == accountId }.count() == recentWellness.count())
            }
            // Simplest possible scoring function
            return HEALTHY_SCORE
        }
    }
}