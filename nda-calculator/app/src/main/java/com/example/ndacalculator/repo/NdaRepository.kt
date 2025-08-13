package com.example.ndacalculator.repo

import android.content.Context
import com.example.ndacalculator.data.AppDatabase
import com.example.ndacalculator.data.CalculationRecordEntity
import com.example.ndacalculator.data.DutyEntryEntity
import com.example.ndacalculator.model.CalculationInput
import com.example.ndacalculator.model.CalculationResult
import com.example.ndacalculator.model.DutyEntry
import com.example.ndacalculator.model.calculateNightDutyAllowance
import java.time.LocalDate

class NdaRepository(private val context: Context) {
	private val db by lazy { AppDatabase.get(context) }

	suspend fun getDutyEntries(): List<DutyEntry> = db.dutyEntryDao().getAll().map {
		DutyEntry(
			id = it.id,
			date = LocalDate.ofEpochDay(it.dateEpochDay),
			hoursBeforeMidnight = it.hoursBeforeMidnight,
			hoursAfterMidnight = it.hoursAfterMidnight
		)
	}

	suspend fun addDutyEntry(entry: DutyEntry) {
		db.dutyEntryDao().insert(
			DutyEntryEntity(
				dateEpochDay = entry.date.toEpochDay(),
				hoursBeforeMidnight = entry.hoursBeforeMidnight,
				hoursAfterMidnight = entry.hoursAfterMidnight
			)
		)
	}

	suspend fun deleteDutyEntry(id: Long) {
		db.dutyEntryDao().deleteById(id)
	}

	suspend fun clearDutyEntries() {
		db.dutyEntryDao().clear()
	}

	fun calculate(input: CalculationInput): CalculationResult = calculateNightDutyAllowance(input)

	suspend fun saveCalculation(result: CalculationResult): Long {
		return db.calculationRecordDao().insert(
			CalculationRecordEntity(
				timestampMillis = System.currentTimeMillis(),
				totalNightHours = result.totalNightHours,
				basicPayApplied = result.basicPayApplied,
				dearnessAllowancePercent = result.dearnessAllowancePercent,
				ndaAmount = result.ndaAmount
			)
		)
	}
}