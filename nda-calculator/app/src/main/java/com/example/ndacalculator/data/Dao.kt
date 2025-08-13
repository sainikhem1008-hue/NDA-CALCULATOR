package com.example.ndacalculator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DutyEntryDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(entry: DutyEntryEntity): Long

	@Delete
	suspend fun delete(entry: DutyEntryEntity)

	@Query("DELETE FROM duty_entries WHERE id = :id")
	suspend fun deleteById(id: Long)

	@Query("SELECT * FROM duty_entries ORDER BY dateEpochDay ASC")
	suspend fun getAll(): List<DutyEntryEntity>

	@Query("DELETE FROM duty_entries")
	suspend fun clear()
}

@Dao
interface CalculationRecordDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insert(record: CalculationRecordEntity): Long

	@Query("SELECT * FROM calculations ORDER BY timestampMillis DESC")
	suspend fun getAll(): List<CalculationRecordEntity>
}