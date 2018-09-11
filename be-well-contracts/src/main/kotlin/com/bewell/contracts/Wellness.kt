package com.bewell.contracts

import com.bewell.schemas.WellnessSchemaV1
import com.bewell.contracts.data.WellnessDetails
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey

val DEFAULT_SCORE = 0

/**
 * Contract to define wellness state, commands and verifications.
 */
class Wellness : Contract {
    companion object {
        const val WELLNESS_ID: ContractClassName = "com.bewell.contracts.Wellness"

        @JvmStatic
        fun generateCreate(tx: TransactionBuilder, owner: AbstractParty, provider: Party,
                details: WellnessDetails, notary: Party) : PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addOutputState(TransactionState(Wellness.State(owner, provider, UniqueIdentifier(), details),
                    WELLNESS_ID, notary))
            tx.addCommand(Wellness.Commands.Create(), owner.owningKey, provider.owningKey)

            return owner.owningKey
        }

        @JvmStatic
        fun generateUpdate(tx: TransactionBuilder, stateAndRef: StateAndRef<State>,
                           updatedDetails: WellnessDetails, notary: Party) : PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addInputState(stateAndRef)
            val updatedWellness = stateAndRef.state.data.copy(details = updatedDetails)

            tx.addOutputState(TransactionState(updatedWellness, WELLNESS_ID, notary))
            tx.addCommand(Wellness.Commands.Update(), updatedWellness.owner.owningKey)

            return updatedWellness.owner.owningKey
        }

        fun generateScore(tx: TransactionBuilder, stateAndRef: StateAndRef<State>,
                          score: Int, notary: Party) : PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addInputState(stateAndRef)
            val scoredWellness = stateAndRef.state.data.copy(score = score)

            tx.addOutputState(TransactionState(scoredWellness, WELLNESS_ID, notary))
            tx.addCommand(Wellness.Commands.Score(), scoredWellness.owner.owningKey, scoredWellness.provider.owningKey)

            return scoredWellness.owner.owningKey
        }
    }

    override fun verify(tx: LedgerTransaction) {
        val groups = tx.groupStates { it: Wellness.State -> it.accountId }

        val command = tx.commands.requireSingleCommand<Wellness.Commands>()
        for ((inputs, outputs, _) in groups) {
            when (command.value) {
                is Commands.Create -> verifyCreateCommand(tx, inputs, outputs)
                is Commands.Update -> verifyUpdateCommand(tx, inputs, outputs)
                is Commands.Score -> verifyScoreCommand(tx, inputs, outputs)
            }
        }
    }

    private fun verifyCreateCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val createCommand = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            "there are no input states" using (inputs.count() == 0)
            "there is a single output state" using (outputs.count() == 1)
            "broker and provider are signers on the command" using
                    (createCommand.signers.containsAll(
                            listOf(outputs.first().owner.owningKey, outputs.first().provider.owningKey)
                    ))
        }

        verifyWellnessDetails(outputs.first().details)
    }

    private fun verifyUpdateCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val updateCommand = tx.commands.requireSingleCommand<Commands.Update>()
        requireThat {
            "there is a single input state" using (inputs.count() == 1)
            "there is a single output state" using (outputs.count() == 1)
            "user broker is a command signer" using (outputs.first().owner.owningKey in updateCommand.signers)
        }

        val inputDetails = inputs.first().details
        val outputDetails = outputs.first().details

        requireThat {
            "details have been updated" using (inputDetails != outputDetails)
        }

        verifyWellnessDetails(outputDetails)
    }

    private fun verifyScoreCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val scoreCommand = tx.commands.requireSingleCommand<Commands.Score>()
        requireThat {
            "there is a single input state" using (inputs.count() == 1)
            "there is a single output state" using (outputs.count() == 1)
            "broker and provider are signers on the command" using
                    (scoreCommand.signers.containsAll(
                            listOf(outputs.first().owner.owningKey, outputs.first().provider.owningKey)
                    ))
        }

        val output = outputs.first()
        requireThat {
            "valid score has been computed" using ((output.score > 0) && (output.score < 10))
        }
    }

    private fun verifyWellnessDetails(details: WellnessDetails) {
        requireThat {
            "age is within human range" using (details.basics.age in 0..120)
            "height is within expected range" using (details.basics.height in 0..275)
            "weight is within expected range" using (details.basics.weight in 0..450)
        }

        if (details.basics.heartRate > 0) {
            requireThat {
                "heart rate is in normal range" using (details.basics.heartRate in 40..120)
            }
        }
    }

    data class State(override val owner: AbstractParty,
                     val provider: Party,
                     val accountId: UniqueIdentifier,
                     val details: WellnessDetails,
                     val score: Int = DEFAULT_SCORE) : OwnableState, QueryableState {

        override val participants = listOf(owner, provider)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState =
                CommandAndState(Commands.Update(), copy(owner = newOwner))

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is WellnessSchemaV1 -> WellnessSchemaV1.PersistentWellnessState(
                        owner = this.owner,
                        provider =  this.provider,
                        accountId = this.accountId.toString(),
                        sex = this.details.basics.sex.toString(),
                        age = this.details.basics.age,
                        height = this.details.basics.height,
                        weight = this.details.basics.weight,
                        heartRate = this.details.basics.heartRate,
                        vegetables = this.details.diet.vegetables,
                        fruits = this.details.diet.fruits,
                        meat = this.details.diet.meat,
                        exercise = this.details.exercise,
                        sleep = this.details.sleep,
                        score = this.score
                )
                else -> throw IllegalArgumentException("Unrecognized schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(WellnessSchemaV1)
    }

    interface Commands : CommandData {
        class Create : Commands

        class Update : Commands

        class Score: Commands
    }


}