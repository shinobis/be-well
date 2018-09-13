package com.bewell.flows

import com.bewell.contracts.Wellness
import com.bewell.schemas.WellnessSchemaV1
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.BinaryComparisonOperator
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.node.services.vault.QueryCriteria
import java.time.LocalDate
import java.time.ZoneOffset

fun ServiceHub.accountExists(accountId: UniqueIdentifier) : List<StateAndRef<Wellness.State>> {
    val queryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
    val customCriteria =
            QueryCriteria.VaultCustomQueryCriteria(WellnessSchemaV1.PersistentWellnessState::accountId.equal(accountId.toString()))

    val criteria = queryCriteria.and(customCriteria)

    val pages = vaultService.queryBy(Wellness.State::class.java, criteria)
    return pages.states
}

fun ServiceHub.retrieveWellness(accountId: UniqueIdentifier) : List<StateAndRef<Wellness.State>> {
    val asOfDateTime = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
    val consumedAfterExpression = QueryCriteria.TimeCondition(
            QueryCriteria.TimeInstantType.CONSUMED, ColumnPredicate.BinaryComparison(BinaryComparisonOperator.GREATER_THAN_OR_EQUAL, asOfDateTime))

    val queryCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.CONSUMED, timeCondition = consumedAfterExpression)
    val customCriteria =
            QueryCriteria.VaultCustomQueryCriteria(WellnessSchemaV1.PersistentWellnessState::accountId.equal(accountId.toString()))

    val criteria = queryCriteria.and(customCriteria)
    val pages = vaultService.queryBy(Wellness.State::class.java, criteria)

    return pages.states
}