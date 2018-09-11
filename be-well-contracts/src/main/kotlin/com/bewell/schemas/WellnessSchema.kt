package com.bewell.schemas

import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object WellnessSchema

/**
 * Schema to define database table to capture data in a wellness state.
 */
@CordaSerializable
object WellnessSchemaV1 : MappedSchema(schemaFamily = WellnessSchema.javaClass, version = 1,
        mappedTypes = listOf(PersistentWellnessState::class.java)) {
    @Entity
    @Table(name = "wellness_states")
    class PersistentWellnessState(

            @Column(name = "owner")
            var owner: AbstractParty? = null,

            @Column(name = "provider")
            var provider: AbstractParty? = null,

            @Column(name = "account_id")
            var accountId: String = "",

            @Column(name = "sex")
            var sex: String = "",

            @Column(name = "age")
            var age: Int = 0,

            @Column(name = "height")
            var height: Int = 0,

            @Column(name = "weight")
            var weight: Int = 0,

            @Column(name = "heart_rate")
            var heartRate: Int = 0,

            @Column(name = "vegetables")
            var vegetables: Int = 0,

            @Column(name = "fruits")
            var fruits: Int =0,

            @Column(name = "meat")
            var meat: Int = 0,

            @Column(name = "exercise")
            var exercise: Float = 0f,

            @Column(name = "sleep")
            var sleep: Float = 0f,

            @Column(name = "wellness_score")
            var score: Int = 0

    ) : PersistentState()
}