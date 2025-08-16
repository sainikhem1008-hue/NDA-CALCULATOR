package com.example.ndacalculator.ui

import android.app.DatePickerDialog
import android.content.Context
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ndacalculator.model.BASIC_PAY_CAP
import com.example.ndacalculator.vm.NdaViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Calendar

@Composable
fun NdaHomeScreen(viewModel: NdaViewModel = viewModel()) {
	val context = LocalContext.current
	val entries by viewModel.entries.collectAsState()
	val basicPay by viewModel.basicPay.collectAsState()
	val daPercent by viewModel.daPercent.collectAsState()
	val result by viewModel.result.collectAsState()

	var selectedDate by remember { mutableStateOf(LocalDate.now()) }
	var hoursBefore by remember { mutableStateOf(0.0) }
	var hoursAfter by remember { mutableStateOf(0.0) }

	var basicPayText by remember { mutableStateOf("") }
	var daText by remember { mutableStateOf("") }

	LaunchedEffect(Unit) {
		viewModel.refreshEntries()
	}

	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Text(text = "Night Duty Allowance Calculator", style = MaterialTheme.typography.headlineSmall)

		DatePickerSection(
			context = context,
			selectedDate = selectedDate,
			onDateSelected = { selectedDate = it }
		)

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = hoursBefore.toString(),
				onValueChange = { v -> hoursBefore = v.toDoubleOrNull() ?: 0.0 },
				label = { Text("Hours 22:00-00:00") },
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
			)
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = hoursAfter.toString(),
				onValueChange = { v -> hoursAfter = v.toDoubleOrNull() ?: 0.0 },
				label = { Text("Hours 00:00-06:00") },
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
			)
		}

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Button(onClick = {
				viewModel.addEntry(selectedDate, hoursBefore, hoursAfter)
				hoursBefore = 0.0
				hoursAfter = 0.0
			}) { Text("Add date") }
			Button(onClick = { viewModel.clearEntries() }) { Text("Clear dates") }
		}

		Card(modifier = Modifier.fillMaxWidth()) {
			Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text("Entries: ${entries.size}")
				val totalHours = entries.sumOf { it.totalNightHours }
				Text("Total night hours: ${"%.2f".format(totalHours)}")
			}
		}

		Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
			entries.forEach { e ->
				Card(modifier = Modifier.fillMaxWidth()) {
					Row(
						modifier = Modifier.padding(12.dp),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
							Text("${e.date}")
							Text("22-00: ${"%.2f".format(e.hoursBeforeMidnight)} h, 00-06: ${"%.2f".format(e.hoursAfterMidnight)} h")
						}
						val scope = rememberCoroutineScope()
						Button(onClick = {
							val id = e.id
							if (id != null) {
								scope.launch { viewModel.deleteEntry(id) }
							}
						}) { Text("Remove") }
					}
				}
			}
		}

		OutlinedTextField(
			value = basicPayText,
			onValueChange = {
				basicPayText = it
				viewModel.setBasicPay(it.toDoubleOrNull() ?: 0.0)
			},
			label = { Text("Basic pay (cap ${BASIC_PAY_CAP.toInt()})") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			modifier = Modifier.fillMaxWidth()
		)
		OutlinedTextField(
			value = daText,
			onValueChange = {
				daText = it
				viewModel.setDaPercent(it.toDoubleOrNull() ?: 0.0)
			},
			label = { Text("Dearness allowance (%)") },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
			modifier = Modifier.fillMaxWidth()
		)

		Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Button(onClick = { viewModel.calculate() }) { Text("Calculate") }
			val scope = rememberCoroutineScope()
			Button(onClick = {
				scope.launch {
					viewModel.calculate()
					viewModel.saveCalculation()
					val res = viewModel.result.value
					if (res != null) exportPdf(context, res.totalNightHours, res.basicPayApplied, res.dearnessAllowancePercent, res.ndaAmount)
				}
			}) { Text("Save & Export PDF") }
		}

		result?.let { res ->
			Card(modifier = Modifier.fillMaxWidth()) {
				Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
					Text("Total night hours: ${"%.2f".format(res.totalNightHours)}")
					Text("Basic pay applied: ${"%.2f".format(res.basicPayApplied)}")
					Text("DA (%): ${"%.2f".format(res.dearnessAllowancePercent)}")
					Text("NDA amount: ${"%.2f".format(res.ndaAmount)}")
				}
			}
		}
	}
}

@Composable
private fun DatePickerSection(context: Context, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
	val calendar = Calendar.getInstance().apply {
		set(Calendar.YEAR, selectedDate.year)
		set(Calendar.MONTH, selectedDate.monthValue - 1)
		set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)
	}
	val datePicker = remember(selectedDate) {
		DatePickerDialog(
			context,
			{ _, year, month, dayOfMonth ->
				onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
			},
			calendar.get(Calendar.YEAR),
			calendar.get(Calendar.MONTH),
			calendar.get(Calendar.DAY_OF_MONTH)
		)
	}
	Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
		Text("Selected date: $selectedDate")
		Button(onClick = { datePicker.show() }) { Text("Pick date") }
	}
}

private fun exportPdf(context: Context, totalHours: Double, basicPayApplied: Double, daPercent: Double, ndaAmount: Double) {
	try {
		val pdfFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
			"nda_${System.currentTimeMillis()}.pdf")
		PdfHelper.writeSimplePdf(
			file = pdfFile,
			title = "Night Duty Allowance",
			lines = listOf(
				"Total night hours: ${"%.2f".format(totalHours)}",
				"Basic pay applied: ${"%.2f".format(basicPayApplied)}",
				"DA (%): ${"%.2f".format(daPercent)}",
				"NDA amount: ${"%.2f".format(ndaAmount)}"
			)
		)
	} catch (t: Throwable) {
		// no-op for now
	}
}