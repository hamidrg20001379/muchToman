package com.doxigo.muchtoman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoxesTest {
    private val cash = Holding(TOMAN_ID, 1_000_000.0, label = "خرج", id = "cash")
    private val savings = Holding(TOMAN_ID, 200_000.0, label = "ذخیره", id = "savings")

    @Test
    fun `a same-unit transfer preserves the combined box balance`() {
        val moved = moveBetweenBoxes(listOf(cash, savings), cash.key, savings.key, 250_000.0)!!
        assertEquals(750_000.0, moved.first { it.key == cash.key }.amount, 0.0)
        assertEquals(450_000.0, moved.first { it.key == savings.key }.amount, 0.0)
        assertEquals(1_200_000.0, moved.sumOf { it.amount }, 0.0)
    }

    @Test
    fun `a transfer cannot silently exchange units or overdraw a box`() {
        val dollars = Holding("usd", 100.0, id = "usd")
        assertNull(moveBetweenBoxes(listOf(cash, dollars), cash.key, dollars.key, 1.0))
        assertNull(moveBetweenBoxes(listOf(cash, savings), cash.key, savings.key, 1_000_001.0))
    }
}
