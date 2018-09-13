package com.bewell.flows

import com.bewell.contracts.Wellness
import com.bewell.contracts.data.Basics
import com.bewell.contracts.data.Diet
import com.bewell.contracts.data.Sex
import com.bewell.contracts.data.WellnessDetails
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.InMemoryMessagingNetwork
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotNull

class UpdateWellnessFlowTest {

    private lateinit var mockNet: MockNetwork
    private lateinit var wellnessProviderNode: StartedMockNode
    private lateinit var userBroker: StartedMockNode
    private lateinit var notary: Party

    private val WELLNESS_DETAILS = WellnessDetails(Basics(Sex.FEMALE, 38, 164, 60, 58),
            Diet(3, 2, 90), 3.5f, 8f)
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

    @Test
    fun `update wellness report`() {
        var resultFuture = userBroker.startFlow(CreateWellnessFlow.BrokerFlow(userBroker.info.singleIdentity(),
                wellnessProviderNode.info.singleIdentity(), WELLNESS_DETAILS))
        mockNet.runNetwork()
        var result = resultFuture.getOrThrow()

        val accountId = (result.coreTransaction.outputStates.first() as Wellness.State).accountId
        resultFuture = userBroker.startFlow(UpdateWellnessFlow(accountId, WELLNESS_DETAILS.copy(exercise = 4f, sleep = 7.5f)))
        mockNet.runNetwork()

        result = resultFuture.getOrThrow()

        assertNotNull(userBroker.services.validatedTransactions.getTransaction(result.id))
    }
}