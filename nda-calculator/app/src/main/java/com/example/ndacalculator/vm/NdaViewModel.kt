package com.example.ndacalculator.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ndacalculator.model.CalculationInput
import com.example.ndacalculator.model.CalculationResult
import com.example.ndacalculator.model.DutyEntry
import com.example.ndacalculator.repo.NdaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class NdaViewModel(application: Application) : AndroidViewModel(application) {
	private val repository = NdaRepository(application)

	private val _entries = MutableStateFlow<List<DutyEntry>>(emptyList())
	val entries: StateFlow<List<DutyEntry>> = _entries.asStateFlow()

	private val _basicPay = MutableStateFlow(0.0)
	val basicPay: StateFlow<Double> = _basicPay.asStateFlow()

	private val _daPercent = MutableStateFlow(0.0)
	val daPercent: StateFlow<Double> = _daPercent.asStateFlow()

	private val _result = MutableStateFlow<CalculationResult?>(null)
	val result: StateFlow<CalculationResult?> = _result.asStateFlow()

	init {
		refreshEntries()
	}

	fun refreshEntries() {
		viewModelScope.launch {
			_entries.value = repository.getDutyEntries()
		}
	}

	fun setBasicPay(value: Double) { _basicPay.value = value }
	fun setDaPercent(value: Double) { _daPercent.value = value }

	fun addEntry(date: LocalDate, beforeMidnightHours: Double, afterMidnightHours: Double) {
		viewModelScope.launch {
			repository.addDutyEntry(
				DutyEntry(
					date = date,
					hoursBeforeMidnight = beforeMidnightHours,
					hoursAfterMidnight = afterMidnightHours
				)
			)
			refreshEntries()
		}
	}

	fun deleteEntry(id: Long) {
		viewModelScope.launch {
			repository.deleteDutyEntry(id)
			refreshEntries()
		}
	}

	fun clearEntries() {
		viewModelScope.launch {
			repository.clearDutyEntries()
			refreshEntries()
		}
	}

	fun calculate() {
		val input = CalculationInput(
			entries = _entries.value,
			basicPay = _basicPay.value,
			dearnessAllowancePercent = _daPercent.value
		)
		_result.value = repository.calculate(input)
	}

	suspend fun saveCalculation(): Long {
		val res = _result.value ?: calculateAndGet()
		return repository.saveCalculation(res)
	}

	private fun calculateAndGet(): CalculationResult {
		val input = CalculationInput(
			entries = _entries.value,
			basicPay = _basicPay.value,
			dearnessAllowancePercent = _daPercent.value
		)
		val res = repository.calculate(input)
		_result.value = res
		return res
	}
}