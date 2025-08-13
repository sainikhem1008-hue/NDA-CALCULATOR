package com.example.ndacalculator.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "duty_entries")
data class DutyEntryEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val dateEpochDay: Long,
	val hoursBeforeMidnight: Double,
	val hoursAfterMidnight: Double
)

@Entity(tableName = "calculations")
data class CalculationRecordEntity(
	@PrimaryKey(autoGenerate = true) val id: Long = 0,
	val timestampMillis: Long,
	val totalNightHours: Double,
	val basicPayApplied: Double,
	val dearnessAllowancePercent: Double,
	val ndaAmount: Double
)