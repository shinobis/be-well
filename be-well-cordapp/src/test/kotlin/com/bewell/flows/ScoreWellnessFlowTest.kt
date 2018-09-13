package com.bewell.flows

import com.bewell.contracts.Wellness
import com.bewell.contracts.data.Basics
import com.bewell.contracts.data.Diet
import com.bewell.contracts.data.Sex
import com.bewell.contracts.data.WellnessDetails
import com.bewell.flows.ScoreWellnessFlow.HEALTHY_SCORE
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.InMemoryMessagingNetwork
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ScoreWellnessFlowTest {
    private lateinit var mockNet: MockNetwork
    private lateinit var wellnessProviderNode: StartedMockNode
    private lateinit var userBroker: StartedMockNode
    private lateinit var notary: Party

    private val WELLNESS_DETAILS = WellnessDetails(Basics(Sex.MALE, 34, 172, 75, 62),
            Diet(3, 2, 110), 2f, 8.5f)
    @Before
    fun setup() {
        mockNet = MockNetwork(servicePeerAllocationStrategy = InMemoryMessagingNetwork.ServicePeerAllocationStrategy.RoundRobin(),
                cordappPackages = CONTRACT_PACKAGES)
        userBroker = mockNet.createNode(BEWELL_USERS_NAME)
        wellnessProviderNode = mockNet.createNode(MIRANDA_WELLNESS_NAME)
        notary = mockNet.defaultNotaryIdentity

        wellnessProviderNode.registerInitiatedFlow(CreateWellnessFlow.ProviderFlow::class.java)
    }

    @After
    fun cleanup() {
        mockNet.stopNodes()
    }

    fun createWellnessReport() : SignedTransaction {
        val resultFuture = userBroker.startFlow(CreateWellnessFlow.BrokerFlow(userBroker.info.singleIdentity(),
                wellnessProviderNode.info.singleIdentity(), WELLNESS_DETAILS))
        mockNet.runNetwork()

        return resultFuture.getOrThrow()
    }

    fun updateWellnessReport(accountId: UniqueIdentifier, updatedDetails: WellnessDetails) : SignedTransaction {
        val resultFuture = userBroker.startFlow(UpdateWellnessFlow(accountId, updatedDetails))
        mockNet.runNetwork()

        return resultFuture.getOrThrow()
    }

    fun currentDetails(tx: SignedTransaction) : WellnessDetails {
        val wellnessOutput = tx.coreTransaction.outputStates.first() as Wellness.State
        return wellnessOutput.details
    }

    @Test
    fun `score wellness report`() {
        var tx = createWellnessReport()
        val accountId = (tx.coreTransaction.outputStates.first() as Wellness.State).accountId

        val random = Random()

        for (i in 0..30) {
            val details = currentDetails(tx)
            val change = random.nextInt(10) + 1
            val updatedWeight = if (i % 2 == 0) (details.basics.weight + change) else (details.basics.weight - change)
            val updated = details.copy(basics = details.basics.copy(weight = updatedWeight))
            tx = updateWellnessReport(accountId, updated)
        }

        wellnessProviderNode.registerInitiatedFlow(ScoreWellnessFlow.ProviderFlow::class.java)
        val resultFuture = userBroker.startFlow(ScoreWellnessFlow.BrokerFlow(accountId))
        mockNet.runNetwork()

        val result = resultFuture.getOrThrow()

        assertNotNull(userBroker.services.validatedTransactions.getTransaction(result.id))
        assertNotNull(wellnessProviderNode.services.validatedTransactions.getTransaction(result.id))

        userBroker.transaction {
            val wellness = userBroker.services.vaultService
                    .queryBy<Wellness.State>(VaultQueryCriteria(Vault.StateStatus.UNCONSUMED))
                    .states.map { it.state.data }
            val latestScore = wellness.first().score
            assertEquals(HEALTHY_SCORE, latestScore)
        }
    }
}