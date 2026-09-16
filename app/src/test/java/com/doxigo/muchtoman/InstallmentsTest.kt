package com.doxigo.muchtoman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentsTest {
    private val first = jalaliDay(1405, 6, 31)
    private val plan = InstallmentPlan("phone", "گوشی", 10_000_000, 3, first)

    @Test
    fun `partial payments settle one due installment without becoming a new installment`() {
        val progress = installmentProgress(
            plan,
            listOf(
                InstallmentPayment(plan.id, 4_000_000, first),
                InstallmentPayment(plan.id, 6_000_000, first),
            ),
            first,
        )
        assertEquals(10_000_000L, progress.paidRial)
        assertEquals(0L, progress.overdueRial)
        assertEquals(2L, progress.remainingRial / 10_000_000L)
    }

    @Test
    fun `a missing second payment is overdue only after its Jalali due date`() {
        val secondDue = jalaliMonthsAfter(first, 1)
        val progress = installmentProgress(
            plan,
            listOf(InstallmentPayment(plan.id, 10_000_000, first)),
            secondDue,
        )
        assertEquals(2, progress.dueCount)
        assertEquals(10_000_000L, progress.overdueRial)
    }

    @Test
    fun `month ends clamp a due day instead of skipping Esfand`() {
        val esfand = jalaliMonthsAfter(first, 6)
        assertEquals(JalaliDate(1405, 12, 29), jalaliOf(esfand))
        assertTrue(jalaliOf(jalaliMonthsAfter(first, 7)).day == 31)
    }

    @Test
    fun `an oversized payment cannot overflow into a negative remaining balance`() {
        val progress = installmentProgress(
            plan,
            listOf(InstallmentPayment(plan.id, Long.MAX_VALUE, first)),
            first,
        )
        assertTrue(progress.complete)
        assertEquals(0L, progress.remainingRial)
    }
}
