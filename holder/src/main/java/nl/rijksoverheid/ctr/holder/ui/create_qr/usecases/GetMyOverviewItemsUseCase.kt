package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.rijksoverheid.ctr.appconfig.usecases.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.persistence.database.HolderDatabase
import nl.rijksoverheid.ctr.holder.persistence.database.entities.CredentialEntity
import nl.rijksoverheid.ctr.holder.persistence.database.entities.GreenCardType
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.persistence.database.models.GreenCard
import nl.rijksoverheid.ctr.holder.ui.create_qr.usecases.MyOverviewItem.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.usecases.MyOverviewItem.GreenCardItem.CredentialState
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.CredentialUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.GreenCardUtil
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.OriginState
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.OriginUtil

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

/**
 * Get all cards that should be displayed in MyOverviewFragment
 */
interface GetMyOverviewItemsUseCase {
    suspend fun get(
        walletId: Int,
        selectedType: GreenCardType
    ): MyOverviewItems
}

class GetMyOverviewItemsUseCaseImpl(private val holderDatabase: HolderDatabase,
                                    private val credentialUtil: CredentialUtil,
                                    private val cachedAppConfigUseCase: CachedAppConfigUseCase,
                                    private val greenCardUtil: GreenCardUtil,
                                    private val originUtil: OriginUtil) :
    GetMyOverviewItemsUseCase {

    override suspend fun get(
        walletId: Int,
        selectedType: GreenCardType
    ): MyOverviewItems {
        return withContext(Dispatchers.IO) {
            val unselectedType = when (selectedType) {
                is GreenCardType.Domestic -> GreenCardType.Eu
                is GreenCardType.Eu -> GreenCardType.Domestic
            }

            val allGreenCards = holderDatabase.greenCardDao().getAll()
            val greenCardsForSelectedType = allGreenCards.filter { it.greenCardEntity.type == selectedType }
            val greenCardsForUnselectedType = allGreenCards.filter { it.greenCardEntity.type == unselectedType }

            val items = mutableListOf<MyOverviewItem>()

            items.addAll(
                getGreenCardItems(
                    selectedType = selectedType,
                    greenCardsForSelectedType = greenCardsForSelectedType,
                    greenCardsForUnselectedType = greenCardsForUnselectedType
                )
            )

            getCreatePlaceholderCardItem(items)?.let {
                items.add(it)
            }

            getTravelModeItem(
                greenCards = allGreenCards,
                selectedType = selectedType
            )?.let {
                items.add(it)
            }

            MyOverviewItems(
                items = items,
                selectedType = selectedType
            )
        }
    }

    private suspend fun getGreenCardItems(
        selectedType: GreenCardType,
        greenCardsForSelectedType: List<GreenCard>,
        greenCardsForUnselectedType: List<GreenCard>
    ): List<MyOverviewItem> {

        // Loop through all green cards that exists in the database and map them to UI models
        val items = greenCardsForSelectedType.map { greenCard ->
            // If the origin with the highest possible expiration time is expired
            if (greenCardUtil.isExpired(greenCard)) {
                // Remove green card from database
                holderDatabase.greenCardDao().delete(greenCard.greenCardEntity)

                // Show green card expired banner
                GreenCardExpiredItem(greenCardType = greenCard.greenCardEntity.type)
            } else {
                // Check if we have a credential
                val activeCredential = credentialUtil.getActiveCredential(
                    entities = greenCard.credentialEntities
                )

                // Check the states of our origins
                val originStates = originUtil.getOriginState(
                    origins = greenCard.origins
                ).sortedBy { it.origin.type.order }

                // Check if we have any valid origins
                val hasValidOriginStates = originStates.any { it is OriginState.Valid }
                val nonExpiredOriginStates = originStates.filterNot { it is OriginState.Expired }

                // More our credential to a more readable state
                val credentialState = when {
                    activeCredential == null -> CredentialState.NoCredential
                    !hasValidOriginStates -> CredentialState.NoCredential
                    else -> CredentialState.HasCredential(activeCredential)
                }

                // Show green card
                GreenCardItem(
                    greenCard = greenCard,
                    originStates = nonExpiredOriginStates,
                    credentialState = credentialState
                )
            }
        }.toMutableList()

        // If we have valid origins that exists in the other selected type but not in the current one, we show a banner
        val allOriginsForSelectedType = greenCardsForSelectedType.map { it.origins }.flatten()
        val allOriginsForUnselectedType = greenCardsForUnselectedType.map { it.origins }.flatten()
        val allValidOriginsForSelectedType = originUtil.getOriginState(allOriginsForSelectedType)
            .filter { it is OriginState.Valid || it is OriginState.Future }.map { it.origin }
        val allValidOriginsForUnselectedType = originUtil.getOriginState(allOriginsForUnselectedType)
            .filter { it is OriginState.Valid || it is OriginState.Future }.map { it.origin }

        allValidOriginsForUnselectedType.forEach { originForUnselectedType ->
            if (!allValidOriginsForSelectedType.map { it.type }.contains(originForUnselectedType.type)) {
                items.add(
                    OriginInfoItem(
                        greenCardType = selectedType,
                        originType = originForUnselectedType.type
                    )
                )
            }
        }

        // Always order by origin type
        items.sortBy {
            when (it) {
                is GreenCardItem -> {
                    it.originStates.first().origin.type.order
                }
                is OriginInfoItem -> {
                    it.originType.order
                }
                else -> {
                    0
                }
            }
        }

        return items
    }

    private fun getCreatePlaceholderCardItem(
        greenCards: List<MyOverviewItem>
    ): MyOverviewItem? {
        return if (greenCards.any { it is GreenCardItem }) null else PlaceholderCardItem
    }

    private fun getTravelModeItem(
        greenCards: List<GreenCard>,
        selectedType: GreenCardType
    ): MyOverviewItem? {
        return when (selectedType) {
            is GreenCardType.Eu -> {
                TravelModeItem(
                    text = R.string.travel_toggle_europe,
                    buttonText = R.string.travel_toggle_change_domestic
                )
            }
            is GreenCardType.Domestic -> {
                val hasGreenCards = greenCards.map { greenCardUtil.isExpired(it) }.any { !it }
                if (hasGreenCards) {
                    TravelModeItem(
                        text = R.string.travel_toggle_domestic,
                        buttonText = R.string.travel_toggle_change_eu
                    )
                } else {
                    null
                }
            }
        }
    }
}

data class MyOverviewItems(
    val items: List<MyOverviewItem>,
    val selectedType: GreenCardType,
) {
    fun setGreenCardItemsLoading(): MyOverviewItems {
        val itemsWithGreenItemsLoading = items.map {
            if (it is GreenCardItem) {
                it.copy(loading = true)
            } else {
                it
            }
        }
        return this.copy(items = itemsWithGreenItemsLoading)
    }
}

sealed class MyOverviewItem {

    object PlaceholderCardItem : MyOverviewItem()

    data class GreenCardItem(
        val greenCard: GreenCard,
        val originStates: List<OriginState>,
        val credentialState: CredentialState,
        val loading: Boolean = false,
    ) : MyOverviewItem() {

        sealed class CredentialState {
            data class HasCredential(val credential: CredentialEntity) : CredentialState()
            object NoCredential : CredentialState()
        }
    }

    data class GreenCardExpiredItem(
        val greenCardType: GreenCardType
    ) : MyOverviewItem()

    data class TravelModeItem(@StringRes val text: Int, @StringRes val buttonText: Int) :
        MyOverviewItem()

    data class OriginInfoItem(val greenCardType: GreenCardType, val originType: OriginType) :
        MyOverviewItem()
}
