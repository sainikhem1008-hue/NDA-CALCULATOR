package com.example.ndacalculator.model

import java.time.LocalDate
import kotlin.math.min

const val BASIC_PAY_CAP = 43600.0

data class DutyEntry(
	val id: Long? = null,
	val date: LocalDate,
	val hoursBeforeMidnight: Double,
	val hoursAfterMidnight: Double
) {
	val totalNightHours: Double get() = hoursBeforeMidnight + hoursAfterMidnight
}

data class CalculationInput(
	val entries: List<DutyEntry>,
	val basicPay: Double,
	val dearnessAllowancePercent: Double
)

data class CalculationResult(
	val totalNightHours: Double,
	val basicPayApplied: Double,
	val dearnessAllowancePercent: Double,
	val ndaAmount: Double
)

fun calculateNightDutyAllowance(input: CalculationInput): CalculationResult {
	val totalHours = input.entries.sumOf { it.totalNightHours }
	val basicApplied = min(input.basicPay, BASIC_PAY_CAP)
	val multiplier = (1.0 + input.dearnessAllowancePercent / 100.0)
	val nda = (totalHours / 6.0) * ((basicApplied * multiplier) / 200.0)
	return CalculationResult(
		totalNightHours = totalHours,
		basicPayApplied = basicApplied,
		dearnessAllowancePercent = input.dearnessAllowancePercent,
		ndaAmount = nda
	)
}