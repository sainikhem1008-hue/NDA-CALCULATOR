package com.example.ndacalculator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [DutyEntryEntity::class, CalculationRecordEntity::class],
	version = 1,
	exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun dutyEntryDao(): DutyEntryDao
	abstract fun calculationRecordDao(): CalculationRecordDao

	companion object {
		@Volatile private var instance: AppDatabase? = null

		fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
			instance ?: Room.databaseBuilder(
				context.applicationContext,
				AppDatabase::class.java,
				"nda_calculator.db"
			).build().also { instance = it }
		}
	}
}