package com.keptang.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Dashboard's saved layout survives as a string in DataStore, so every round trip - including
 * the ones written by an older or newer build - has to come back as something the Dashboard can
 * render without dropping or duplicating a card.
 */
class DashboardCardOrderTest {

    @Test
    fun roundTripsOrderAndVisibility() {
        val stored = listOf(DashboardCard.RECENT, DashboardCard.SPENDING)

        assertEquals(stored, decodeDashboardCards(encodeDashboardCards(stored)))
    }

    @Test
    fun nothingStoredMeansEveryCardVisibleInDeclarationOrder() {
        assertEquals(DashboardCard.entries, decodeDashboardCards(null))
    }

    @Test
    fun storedEmptyStringMeansEveryCardHidden_notEveryCardVisible() {
        val hiddenEverything = encodeDashboardCards(emptyList())

        assertEquals(emptyList<DashboardCard>(), decodeDashboardCards(hiddenEverything))
    }

    @Test
    fun skipsNamesThisBuildDoesNotKnow() {
        assertEquals(
            listOf(DashboardCard.SPENDING, DashboardCard.RECENT),
            decodeDashboardCards("SPENDING,TRENDS,RECENT")
        )
    }

    @Test
    fun dropsDuplicatesSoACardCannotRenderTwice() {
        assertEquals(listOf(DashboardCard.BUDGET), decodeDashboardCards("BUDGET,BUDGET"))
    }
}
