package com.yourname.nightdutycalculator;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yourname.nightdutycalculator.LeaveRecord;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecordsAdapter.OnRecordDeleteListener {

    private TextInputEditText etDutyDate, etDutyFrom, etDutyTo, etCeilingLimit, etBasicPay, etDearnessAllowance;
    private CheckBox cbNationalHoliday, cbWeeklyRest;
    private MaterialButton btnLeaveManagement;
    private MaterialButton btnCalculate, btnSave, btnExport, btnClear, btnExit;
    private LinearLayout llResults;
    private TextView tvCeilingWarning;
    private RecyclerView rvRecords;
    private List<DutyRecord> records = new ArrayList<>();
    private RecordsAdapter adapter;
    private DutyRecord currentCalculation;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private Vibrator vibrator;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews(); setupListeners(); loadRecords(); setDefaultValues();
    }

    private void initViews() {
        etDutyDate = findViewById(R.id.etDutyDate);
        etDutyFrom = findViewById(R.id.etDutyFrom);
        etDutyTo = findViewById(R.id.etDutyTo);
        etCeilingLimit = findViewById(R.id.etCeilingLimit);
        etBasicPay = findViewById(R.id.etBasicPay);
        etDearnessAllowance = findViewById(R.id.etDearnessAllowance);
        cbNationalHoliday = findViewById(R.id.cbNationalHoliday);
        cbWeeklyRest = findViewById(R.id.cbWeeklyRest);
        btnLeaveManagement = findViewById(R.id.btnLeaveManagement);

        btnCalculate = findViewById(R.id.btnCalculate);
        btnSave = findViewById(R.id.btnSave);
        btnExport = findViewById(R.id.btnExport);
        btnClear = findViewById(R.id.btnClear);
        btnExit = findViewById(R.id.btnExit);
        llResults = findViewById(R.id.llResults);
        tvCeilingWarning = findViewById(R.id.tvCeilingWarning);
        rvRecords = findViewById(R.id.rvRecords);
        sharedPreferences = getSharedPreferences("NightDutyRecords", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        adapter = new RecordsAdapter(records, this);
        if (rvRecords != null) {
            rvRecords.setLayoutManager(new LinearLayoutManager(this));
            rvRecords.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        etDutyDate.setOnClickListener(v -> showDatePicker());
        etDutyFrom.setOnClickListener(v -> showTimePicker(etDutyFrom));
        etDutyTo.setOnClickListener(v -> showTimePicker(etDutyTo));
        btnLeaveManagement.setOnClickListener(v -> { vibrate(); openLeaveManagement(); });

        btnCalculate.setOnClickListener(v -> { vibrate(); calculateNightDuty(); });
        btnSave.setOnClickListener(v -> { vibrate(); saveRecord(); });
        btnExport.setOnClickListener(v -> { vibrate(); exportToPDF(); });
        btnClear.setOnClickListener(v -> { vibrate(); clearAllRecords(); });
        btnExit.setOnClickListener(v -> { vibrate(); exitApp(); });

        etBasicPay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkCeilingLimit(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        etCeilingLimit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkCeilingLimit(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void vibrate() { if (vibrator != null && vibrator.hasVibrator()) vibrator.vibrate(50); }

    private void checkCeilingLimit() {
        try { double basicPay = Double.parseDouble(etBasicPay.getText().toString()); double ceilingLimit = Double.parseDouble(etCeilingLimit.getText().toString());
            if (basicPay > ceilingLimit) { if (tvCeilingWarning != null) { tvCeilingWarning.setVisibility(View.VISIBLE); tvCeilingWarning.setText("⚠ Using ceiling limit ₹" + decimalFormat.format(ceilingLimit)); } }
            else if (tvCeilingWarning != null) tvCeilingWarning.setVisibility(View.GONE);
        } catch (Exception e) { if (tvCeilingWarning != null) tvCeilingWarning.setVisibility(View.GONE); }
    }

    private void setDefaultValues() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etDutyDate.setText(sdf.format(new Date()));
        etDutyFrom.setText("22:00");
        etDutyTo.setText("00:00");
        calculateNightDuty();
    }

    private void showDatePicker() {
        showDatePicker(etDutyDate);
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            editText.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void openLeaveManagement() {
        Intent intent = new Intent(this, LeaveManagementActivity.class);
        startActivity(intent);
    }



    private void showTimePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            editText.setText(time);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    private void calculateNightDuty() {
        try {
            String dutyDate = etDutyDate.getText().toString();
            String dutyFrom = etDutyFrom.getText().toString();
            String dutyTo = etDutyTo.getText().toString();
            double basicPay = Double.parseDouble(etBasicPay.getText().toString());
            double ceilingLimit = Double.parseDouble(etCeilingLimit.getText().toString());
            double dearnessAllowance = Double.parseDouble(etDearnessAllowance.getText().toString());
            boolean isNationalHoliday = cbNationalHoliday.isChecked();
            boolean isWeeklyRest = cbWeeklyRest.isChecked();

            if (dutyDate.isEmpty() || dutyFrom.isEmpty() || dutyTo.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show warning if calculating for weekly rest
            if (isWeeklyRest) {
                Toast.makeText(this, "⚠️ Weekly Rest Day - No allowance will be calculated", Toast.LENGTH_LONG).show();
            }

            double effectiveBasicPay = Math.min(basicPay, ceilingLimit);

            Calendar fromCal = Calendar.getInstance();
            Calendar toCal = Calendar.getInstance();

            String[] fromParts = dutyFrom.split(":");
            String[] toParts = dutyTo.split(":");

            fromCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(fromParts[0]));
            fromCal.set(Calendar.MINUTE, Integer.parseInt(fromParts[1]));
            fromCal.set(Calendar.SECOND, 0);

            toCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(toParts[0]));
            toCal.set(Calendar.MINUTE, Integer.parseInt(toParts[1]));
            toCal.set(Calendar.SECOND, 0);

            if (toCal.before(fromCal) || toCal.equals(fromCal)) toCal.add(Calendar.DAY_OF_MONTH, 1);

            long durationMs = toCal.getTimeInMillis() - fromCal.getTimeInMillis();
            double totalDutyHours = durationMs / (1000.0 * 60 * 60);

            double nightHours1 = calculateNightHours(fromCal, toCal, 22, 0, 0, 0);
            double nightHours2 = calculateNightHours(fromCal, toCal, 0, 0, 6, 0);

            double totalNightHours = nightHours1 + nightHours2;
            double nightHoursDivided = totalNightHours / 6.0;

            // Check if duty date falls within any active leave period from leave database
            boolean isOnLeave = false;
            String leaveStatus = "";
            
            // Load leave records from separate database
            SharedPreferences leavePrefs = getSharedPreferences("LeaveRecords", Context.MODE_PRIVATE);
            String leaveRecordsJson = leavePrefs.getString("leave_records", "[]");
            Type leaveListType = new TypeToken<List<LeaveRecord>>(){}.getType();
            List<LeaveRecord> leaveRecords = gson.fromJson(leaveRecordsJson, leaveListType);
            
            if (leaveRecords != null && !leaveRecords.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar dutyDateCal = Calendar.getInstance();
                    
                    dutyDateCal.setTime(sdf.parse(dutyDate));
                    
                    // Check each leave record
                    for (LeaveRecord leaveRecord : leaveRecords) {
                        if (leaveRecord.getStatus().equals("Applied") || leaveRecord.getStatus().equals("Approved")) {
                            Calendar leaveFromCal = Calendar.getInstance();
                            Calendar leaveToCal = Calendar.getInstance();
                            
                            leaveFromCal.setTime(sdf.parse(leaveRecord.getLeaveFrom()));
                            leaveToCal.setTime(sdf.parse(leaveRecord.getLeaveTo()));
                            
                            // Check if duty date is within this leave period
                            if (!dutyDateCal.before(leaveFromCal) && !dutyDateCal.after(leaveToCal)) {
                                isOnLeave = true;
                                leaveStatus = "📅 On Leave (" + leaveRecord.getLeaveType() + ": " + 
                                            leaveRecord.getLeaveFrom() + " to " + leaveRecord.getLeaveTo() + ")";
                                break; // Found matching leave period
                            }
                        }
                    }
                    
                    if (!isOnLeave && !leaveRecords.isEmpty()) {
                        leaveStatus = "📅 Leave Records Available";
                    }
                } catch (Exception e) {
                    // If date parsing fails, assume not on leave
                    isOnLeave = false;
                    leaveStatus = "📅 Leave Check Error";
                }
            }

            // Calculate night duty allowance based on conditions
            double nightDutyAllowance = 0.0;
            String allowanceStatus = "Calculated";
            
            if (isWeeklyRest) {
                allowanceStatus = "❌ No Allowance (Weekly Rest)";
            } else if (isOnLeave) {
                allowanceStatus = "❌ No Allowance (On Leave)";
            } else {
                nightDutyAllowance = nightHoursDivided * (effectiveBasicPay * (1 + dearnessAllowance/100)) / 200;
            }

            currentCalculation = new DutyRecord();
            currentCalculation.setDate(dutyDate);
            currentCalculation.setDutyFrom(dutyFrom);
            currentCalculation.setDutyTo(dutyTo);
            currentCalculation.setTotalDutyHours(totalDutyHours);
            currentCalculation.setNightHours1(nightHours1);
            currentCalculation.setNightHours2(nightHours2);
            currentCalculation.setTotalNightHours(totalNightHours);
            currentCalculation.setBasicPay(basicPay);
            currentCalculation.setEffectiveBasicPay(effectiveBasicPay);
            currentCalculation.setDearnessAllowance(dearnessAllowance);
            currentCalculation.setNightDutyAllowance(nightDutyAllowance);
            currentCalculation.setNationalHoliday(isNationalHoliday);
            currentCalculation.setWeeklyRest(isWeeklyRest);
            currentCalculation.setAllowanceStatus(allowanceStatus);
            currentCalculation.setLeaveStatus(leaveStatus);

            displayResults();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateNightHours(Calendar fromCal, Calendar toCal, int startHour, int startMin, int endHour, int endMin) {
        Calendar nightStart = (Calendar) fromCal.clone();
        Calendar nightEnd = (Calendar) fromCal.clone();

        nightStart.set(Calendar.HOUR_OF_DAY, startHour);
        nightStart.set(Calendar.MINUTE, startMin);
        nightStart.set(Calendar.SECOND, 0);

        nightEnd.set(Calendar.HOUR_OF_DAY, endHour);
        nightEnd.set(Calendar.MINUTE, endMin);
        nightEnd.set(Calendar.SECOND, 0);

        if (endHour < startHour || (endHour == startHour && endMin <= startMin)) nightEnd.add(Calendar.DAY_OF_MONTH, 1);

        Calendar overlapStart = fromCal.after(nightStart) ? fromCal : nightStart;
        Calendar overlapEnd = toCal.before(nightEnd) ? toCal : nightEnd;

        if (overlapStart.before(overlapEnd)) {
            long overlapMs = overlapEnd.getTimeInMillis() - overlapStart.getTimeInMillis();
            return Math.max(0, overlapMs / (1000.0 * 60 * 60));
        }
        return 0;
    }

    private void displayResults() {
        if (llResults != null) llResults.removeAllViews();
        if (currentCalculation == null) return;
        // add simple text views programmatically
        addResultItem("Total Duty Hours:", String.format("%.2f hrs", currentCalculation.getTotalDutyHours()));
        addResultItem("Night Hours (22:00-00:00):", String.format("%.2f hrs", currentCalculation.getNightHours1()));
        addResultItem("Night Hours (00:00-06:00):", String.format("%.2f hrs", currentCalculation.getNightHours2()));
        addResultItem("Total Night Hours:", String.format("%.2f hrs", currentCalculation.getTotalNightHours()));
        
        // Display allowance with status
        if (currentCalculation.getAllowanceStatus() != null && currentCalculation.getAllowanceStatus().startsWith("❌")) {
            addResultItem("Night Allowance:", currentCalculation.getAllowanceStatus());
        } else {
            addResultItem("Night Allowance:", "₹" + decimalFormat.format(currentCalculation.getNightDutyAllowance()));
        }
        
        // Display weekly rest information
        if (currentCalculation.isWeeklyRest()) {
            addResultItem("Weekly Rest:", "✅ Applied");
        }
    }

    private void addResultItem(String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + " " + value);
        tv.setTextSize(16);
        tv.setTextColor(getResources().getColor(R.color.dark_black));
        if (llResults != null) llResults.addView(tv);
    }

    private void saveRecord() {
        if (currentCalculation == null) { Toast.makeText(this, "Please calculate first", Toast.LENGTH_SHORT).show(); return; }
        boolean exists = false;
        for (int i = 0; i < records.size(); i++) { if (records.get(i).getDate().equals(currentCalculation.getDate())) { records.set(i, currentCalculation); exists = true; break; } }
        if (!exists) records.add(currentCalculation);
        saveRecordsToPrefs();
        if (adapter != null) adapter.notifyDataSetChanged();
        Toast.makeText(this, "Record saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void exportToPDF() {
        if (records.isEmpty()) { Toast.makeText(this, "No records to export", Toast.LENGTH_SHORT).show(); return; }
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String baseFileName = "Night_Duty_Report_" + new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault()).format(new Date());
            String fileName = baseFileName + ".pdf";
            File pdfFile = new File(downloadsDir, fileName);
            
            // Handle existing files by adding a counter
            int counter = 1;
            while (pdfFile.exists()) {
                fileName = baseFileName + "_(" + counter + ").pdf";
                pdfFile = new File(downloadsDir, fileName);
                counter++;
            }
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.add(new Paragraph("🌙 Night Duty Allowance Report").setTextAlignment(TextAlignment.CENTER).setFontSize(20));
            document.add(new Paragraph("Generated on: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())).setTextAlignment(TextAlignment.CENTER).setFontSize(12));
            double totalAllowance = 0; double totalHours = 0; int holidayCount = 0; int regularCount = 0; int weeklyRestCount = 0;
            for (DutyRecord record : records) { 
                totalAllowance += record.getNightDutyAllowance(); 
                totalHours += record.getTotalNightHours(); 
                if (record.isNationalHoliday()) holidayCount++; 
                else if (record.isWeeklyRest()) weeklyRestCount++;
                else regularCount++; 
            }
            document.add(new Paragraph("\nSummary:").setFontSize(16));
            document.add(new Paragraph("Total Records: " + records.size()));
            document.add(new Paragraph("Regular Days: " + regularCount));
            document.add(new Paragraph("National Holidays: " + holidayCount));
            document.add(new Paragraph("Weekly Rest Days: " + weeklyRestCount));
            document.add(new Paragraph("Total Night Hours: " + String.format("%.2f hrs", totalHours)));
            document.add(new Paragraph("Total Allowance: ₹" + decimalFormat.format(totalAllowance)));
            Table table = new Table(6);
            table.addHeaderCell("Date"); table.addHeaderCell("Time"); table.addHeaderCell("Hours"); table.addHeaderCell("Basic Pay"); table.addHeaderCell("Type"); table.addHeaderCell("Allowance");
            Collections.sort(records, (a, b) -> b.getDate().compareTo(a.getDate()));
            for (DutyRecord record : records) {
                try { SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); Date date = inputFormat.parse(record.getDate()); table.addCell(outputFormat.format(date)); } catch (Exception e) { table.addCell(record.getDate()); }
                table.addCell(record.getDutyFrom() + "-" + record.getDutyTo());
                table.addCell(String.format("%.1f", record.getTotalNightHours()));
                table.addCell("₹" + decimalFormat.format(record.getEffectiveBasicPay()));
                
                // Determine type
                String type = "Regular";
                if (record.isNationalHoliday() && record.isWeeklyRest()) type = "Holiday Allowance Paid";
                else if (record.isNationalHoliday()) type = "Holiday";
                else if (record.isWeeklyRest()) type = "Weekly Rest";
                table.addCell(type);
                
                // Display allowance in PDF
                if (record.getAllowanceStatus() != null && record.getAllowanceStatus().startsWith("❌")) {
                    table.addCell(record.getAllowanceStatus().replace("❌ ", ""));
                } else {
                    table.addCell("₹" + decimalFormat.format(record.getNightDutyAllowance()));
                }
            }
            document.add(table); document.close();
            Uri pdfUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW); intent.setDataAndType(pdfUri, "application/pdf"); intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Open PDF"));
            Toast.makeText(this, "PDF exported: " + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) { Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show(); e.printStackTrace(); }
    }

    private void clearAllRecords() {
        new AlertDialog.Builder(this).setTitle("Clear All Records").setMessage("Are you sure you want to clear all records? This cannot be undone.").setPositiveButton("Yes", (dialog, which) -> {
            records.clear(); saveRecordsToPrefs(); if (adapter!=null) adapter.notifyDataSetChanged(); if (llResults!=null) llResults.removeAllViews(); currentCalculation = null; Toast.makeText(this, "All records cleared", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("No", null).show();
    }

    @Override public void onRecordDelete(DutyRecord record, int position) {
        new AlertDialog.Builder(this).setTitle("Delete Record").setMessage("Are you sure you want to delete this record?").setPositiveButton("Yes", (dialog, which) -> {
            records.remove(position); saveRecordsToPrefs(); if (adapter!=null) adapter.notifyItemRemoved(position); Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("No", null).show();
    }

    private void loadRecords() {
        String recordsJson = sharedPreferences.getString("records", "[]"); Type listType = new TypeToken<List<DutyRecord>>(){}.getType(); List<DutyRecord> loadedRecords = gson.fromJson(recordsJson, listType);
        if (loadedRecords != null) { records.clear(); records.addAll(loadedRecords); Collections.sort(records, (a, b) -> b.getDate().compareTo(a.getDate())); if (adapter!=null) adapter.notifyDataSetChanged(); }
    }

    private void saveRecordsToPrefs() { String recordsJson = gson.toJson(records); sharedPreferences.edit().putString("records", recordsJson).apply(); }
    
    private void exitApp() {
        new AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit the Night Duty Calculator?")
            .setPositiveButton("Yes", (dialog, which) -> {
                finishAffinity(); // Closes all activities
                System.exit(0);   // Completely exits the app
            })
            .setNegativeButton("No", null)
            .show();
    }
}
