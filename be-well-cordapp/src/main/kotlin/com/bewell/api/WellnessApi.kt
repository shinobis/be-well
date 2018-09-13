package com.bewell.api

import com.bewell.contracts.Wellness
import com.bewell.flows.CreateWellnessFlow
import com.bewell.flows.ScoreWellnessFlow
import com.bewell.flows.UpdateWellnessFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.utilities.getOrThrow
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.NOT_FOUND

@Path("wellness")
class WellnessApi(val rpcOps: CordaRPCOps) {
    private val selfIdentity = rpcOps.nodeInfo().legalIdentities.first()
    private val myLegalName = selfIdentity.name
    private val SERVICE_NODE_NAME = CordaX500Name("Notary", "New York", "US")

    /**
     * Returns the node's name.
     */
    @GET
    @Path("identity")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("identity" to myLegalName.organisation)

    /**
     * Returns all parties registered with the network map.
     */
    @GET
    @Path("participants")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("participants" to nodeInfo
                .map { it.legalIdentities.first().name }
                .filter { it !in listOf(myLegalName, SERVICE_NODE_NAME) })
    }

    /**
     * Get all states from the node's vault.
     */
    @GET
    @Path("vault")
    @Produces(MediaType.APPLICATION_JSON)
    fun getVault(): Pair<List<StateAndRef<Wellness.State>>, List<StateAndRef<Wellness.State>>> {
        val unconsumedStates = rpcOps.vaultQuery(Wellness.State::class.java).states
        val consumedStates = rpcOps.vaultQueryByCriteria(VaultQueryCriteria(StateStatus.CONSUMED), Wellness.State::class.java).states
        return Pair(unconsumedStates, consumedStates)
    }

    @POST
    @Path("create-wellness")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    fun createWellness(registrationData: RegistrationData): Response {
        val providerNode = rpcOps.networkMapSnapshot()
                .map { it.legalIdentities.first() }
                .filter { it.name.organisation == registrationData.provider }.singleOrNull()
                ?: return Response.status(NOT_FOUND).entity("No provider found by the name of ${registrationData.provider}") .build()

        val flowFuture = rpcOps.startFlow(CreateWellnessFlow::BrokerFlow, selfIdentity, providerNode,
                registrationData.wellnessData.toWellnessDetails()).returnValue
        val result = try {
            flowFuture.getOrThrow()
        } catch (e: Exception) {
            return Response.status(BAD_REQUEST).entity(e.message).build()
        }

        val accountId = (result.coreTransaction.outputStates.first() as Wellness.State).accountId
        val message = "Added a new wellness report for account id $accountId in transaction ${result.tx.id} committed to ledger."

        return Response.accepted().entity(message).build()
    }

    @POST
    @Path("update-wellness")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    fun updateWellness(accountData: AccountData): Response {
        val accountId = UniqueIdentifier.fromString(accountData.accountId)
        val flowFuture = rpcOps.startFlow(::UpdateWellnessFlow, accountId,
                accountData.wellnessData.toWellnessDetails()).returnValue
        val result = try {
            flowFuture.getOrThrow()
        } catch (e: Exception) {
            return Response.status(BAD_REQUEST).entity(e.message).build()
        }

        val message = "Updated wellness report for account id $accountId in transaction ${result.tx.id} committed to ledger."
        return Response.accepted().entity(message).build()
    }

    @POST
    @Path("score-wellness")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    fun scoreWellness(accountId: String): Response {
        val accountIdentifier = UniqueIdentifier.fromString(accountId)
        val flowFuture = rpcOps.startFlow(ScoreWellnessFlow::BrokerFlow, accountIdentifier).returnValue
        val result = try {
            flowFuture.getOrThrow()
        } catch (e: Exception) {
            return Response.status(BAD_REQUEST).entity(e.message).build()
        }

        val message = "Added new score for account id $accountId in transaction ${result.tx.id} committed to ledger."
        return Response.accepted().entity(message).build()
    }
}