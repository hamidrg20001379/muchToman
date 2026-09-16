package com.doxigo.muchtoman

import kotlinx.serialization.Serializable

/**
 * The arithmetic behind a monthly installment. This stays separate from the ledger until a
 * payment UI can name the box it came from; recording the same payment once here and once as a
 * manual transaction would make a household total lie.
 *
 * ponytail: Rial-only for the first implementation. Dollar and gold boxes already keep
 * their native quantities in holdings; connecting either to a payment needs an explicit
 * conversion record, not an implicit rate lookup.
 */
@Serializable
data class InstallmentPlan(
    val id: String,
    val titleFa: String,
    /** Each monthly payment, in whole Rial. */
    val paymentRial: Long,
    val count: Int,
    /** Tehran/Jalali day of the first due payment. */
    val firstDueDay: Long,
) {
    init {
        require(titleFa.isNotBlank())
        require(paymentRial > 0)
        require(count > 0)
        Math.multiplyExact(paymentRial, count.toLong()) // reject a total the ledger cannot hold
    }

    val totalRial: Long get() = Math.multiplyExact(paymentRial, count.toLong())
}

/** A partial payment is intentionally just an amount: several may settle one monthly due. */
@Serializable
data class InstallmentPayment(val planId: String, val amountRial: Long, val paidOn: Long) {
    init { require(amountRial > 0) }
}

data class InstallmentProgress(
    val totalRial: Long,
    val paidRial: Long,
    val remainingRial: Long,
    /** Number of scheduled payments due today or earlier, bounded by [InstallmentPlan.count]. */
    val dueCount: Int,
    /** What should have been paid by today but has not been paid yet. */
    val overdueRial: Long,
    val complete: Boolean,
)

/** The due date [months] after [day], preserving its Jalali day where that month has it. */
fun jalaliMonthsAfter(day: Long, months: Int): Long {
    require(months >= 0)
    val here = jalaliOf(day)
    val serial = here.year * 12 + here.month - 1 + months
    val year = serial / 12
    val month = serial % 12 + 1
    return jalaliDay(year, month, minOf(here.day, jalaliMonthLength(year, month)))
}

/**
 * The ledger view of one plan. Payments after today do not quietly make today's card look paid;
 * they become visible on their actual day. Extra payments are capped at the plan total because a
 * completed installment cannot turn into credit for an unrelated one.
 */
fun installmentProgress(
    plan: InstallmentPlan,
    payments: List<InstallmentPayment>,
    today: Long,
): InstallmentProgress {
    var paid = 0L
    for (payment in payments) {
        if (payment.planId != plan.id || payment.paidOn > today) continue
        // Clamp each addition, before it can overflow. A corrupted or duplicate oversized
        // payment must read as «complete», never as a negative balance.
        paid = if (payment.amountRial >= plan.totalRial - paid) plan.totalRial
        else paid + payment.amountRial
    }
    val dueCount = (0 until plan.count).count { jalaliMonthsAfter(plan.firstDueDay, it) <= today }
    val dueRial = Math.multiplyExact(plan.paymentRial, dueCount.toLong())
    return InstallmentProgress(
        totalRial = plan.totalRial,
        paidRial = paid,
        remainingRial = plan.totalRial - paid,
        dueCount = dueCount,
        overdueRial = (dueRial - paid).coerceAtLeast(0L),
        complete = paid == plan.totalRial,
    )
}
