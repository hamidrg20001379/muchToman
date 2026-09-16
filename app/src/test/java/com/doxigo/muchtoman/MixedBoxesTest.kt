package com.doxigo.muchtoman

import org.junit.Assert.assertEquals
import org.junit.Test

class MixedBoxesTest {
    @Test
    fun `mixed box membership keeps each asset and contributes one live Toman total`() {
        val box = MixedBox("travel", "سفر")
        val holdings = listOf(
            Holding(TOMAN_ID, 1_000_000.0, id = "cash", boxId = box.id),
            Holding("usd", 20.0, id = "dollars", boxId = box.id),
        )

        val total = computeTotals(holdings, mapOf(TOMAN_ID to 1.0, "usd" to 900_000.0))

        assertEquals(19_000_000.0, total.toman, 0.0)
        assertEquals(listOf("cash", "dollars"), holdings.filter { it.boxId == box.id }.map { it.key })
    }
}
