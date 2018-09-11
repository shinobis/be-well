package com.bewell.contracts

import com.bewell.contracts.Wellness.Companion.WELLNESS_ID
import com.bewell.contracts.data.Basics
import com.bewell.contracts.data.Sex
import com.bewell.contracts.data.WellnessDetails
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.transaction
import org.junit.Test

/**
 * Test for verifications associated with various commands.
 */
class WellnessTests {

    val ledgerServices = MockServices()

    val BROKER_ONE = TestIdentity(CordaX500Name(commonName = "Be Well", organisation = "Be Well Inc", locality = "New York", country = "US"))
    val BROKER_TWO = TestIdentity(CordaX500Name(commonName = "Fitness One", organisation = "Fitness One Inc", locality = "New York", country = "US"))
    val PROVIDER_ONE = TestIdentity(CordaX500Name(commonName = "First Wellness", organisation = "First Wellness Inc", locality = "New York", country = "US"))
    val PROVIDER_TWO = TestIdentity(CordaX500Name(commonName = "Titan Health", organisation = "Titan Health Inc", locality = "New York", country = "US"))

    @Test
    fun `create wellness has correct states and commands`() {
        val accountId = UniqueIdentifier()
        ledgerServices.transaction {
            attachment(WELLNESS_ID)
            command(listOf(BROKER_ONE.publicKey, PROVIDER_ONE.publicKey), Wellness.Commands.Create())
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 65))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                `fails with` ("there are no input states")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                `fails with` ("there is a single output state")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_TWO.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                `fails with` ("broker and provider are signers on the command")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 128, 168, 69, 68))))
                `fails with` ("age is within human range")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 350, 69, 68))))
                `fails with` ("height is within expected range")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 1200, 68))))
                `fails with` ("weight is within expected range")
            }
            tweak {
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 300))))
                `fails with` ("heart rate is in normal range")
            }
            output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                    WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
            verifies()
        }
    }

    @Test
    fun `update wellness has expected states and commands`() {
        val accountId = UniqueIdentifier()
        ledgerServices.transaction {
            attachment(WELLNESS_ID)
            command(listOf(BROKER_ONE.publicKey), Wellness.Commands.Update())
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 68))))
                `fails with` ("there is a single input state")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 29, 176, 78, 62))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 29, 176, 80, 62))))
                `fails with` ("there is a single output state")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
                output(WELLNESS_ID, Wellness.State(BROKER_TWO.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 29, 176, 78, 62))))
                `fails with` ("user broker is a command signer")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
                `fails with` ("details have been updated")
            }
            input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                    WellnessDetails(Basics(Sex.MALE, 28, 176, 78, 62))))
            output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                    WellnessDetails(Basics(Sex.MALE, 29, 176, 78, 62))))
            verifies()
        }
    }

    @Test
    fun `score wellness has correct states and commands`() {
        val accountId = UniqueIdentifier()
        ledgerServices.transaction {
            attachment(WELLNESS_ID)
            command(listOf(BROKER_ONE.publicKey, PROVIDER_ONE.publicKey), Wellness.Commands.Score())
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 8))
                `fails with` ("there is a single input state")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 8))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 8))
                `fails with` ("there is a single output state")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                output(WELLNESS_ID, Wellness.State(BROKER_TWO.party, PROVIDER_TWO.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 8))
                `fails with` ("broker and provider are signers on the command")
            }
            tweak {
                input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
                output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                        WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 12))
                `fails with` ("valid score has been computed")
            }
            input(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                    WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68))))
            output(WELLNESS_ID, Wellness.State(BROKER_ONE.party, PROVIDER_ONE.party, accountId,
                    WellnessDetails(Basics(Sex.FEMALE, 28, 168, 69, 68)), 8))
            verifies()
        }
    }
}