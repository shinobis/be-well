package com.bewell.flows

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

class CreateWellnessFlowTest {
    private lateinit var mockNet: MockNetwork
    private lateinit var wellnessProviderNode: StartedMockNode
    private lateinit var userBroker: StartedMockNode
    private lateinit var notary: Party

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
    fun `create wellness reports`() {
        val wellnessDetails = WellnessDetails(Basics(Sex.MALE, 42, 176, 85, 76),
                Diet(2, 1, 125), 0.0f, 6.5f)
        val resultFuture = userBroker.startFlow(CreateWellnessFlow.BrokerFlow(userBroker.info.singleIdentity(),
                wellnessProviderNode.info.singleIdentity(),wellnessDetails))
        mockNet.runNetwork()

        val result = resultFuture.getOrThrow()

        assertNotNull(userBroker.services.validatedTransactions.getTransaction(result.id))
        assertNotNull(wellnessProviderNode.services.validatedTransactions.getTransaction(result.id))
    }

}