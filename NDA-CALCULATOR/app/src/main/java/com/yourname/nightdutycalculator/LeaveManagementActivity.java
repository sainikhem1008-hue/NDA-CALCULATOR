package com.yourname.nightdutycalculator;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
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
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LeaveManagementActivity extends AppCompatActivity implements LeaveRecordsAdapter.OnLeaveDeleteListener {

    private TextInputEditText etLeaveFromDate, etLeaveToDate, etLeaveTypeSelection, etLeaveNotes;
    private MaterialButton btnApplyLeave, btnExportLeavePDF;
    private RecyclerView rvLeaveRecords;
    private List<LeaveRecord> leaveRecords = new ArrayList<>();
    private LeaveRecordsAdapter adapter;
    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_management);
        initViews();
        setupListeners();
        loadLeaveRecords();
    }

    private void initViews() {
        etLeaveFromDate = findViewById(R.id.etLeaveFromDate);
        etLeaveToDate = findViewById(R.id.etLeaveToDate);
        etLeaveTypeSelection = findViewById(R.id.etLeaveTypeSelection);
        etLeaveNotes = findViewById(R.id.etLeaveNotes);
        btnApplyLeave = findViewById(R.id.btnApplyLeave);
        btnExportLeavePDF = findViewById(R.id.btnExportLeavePDF);
        rvLeaveRecords = findViewById(R.id.rvLeaveRecords);
        
        sharedPreferences = getSharedPreferences("LeaveRecords", Context.MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        adapter = new LeaveRecordsAdapter(leaveRecords, this);
        rvLeaveRecords.setLayoutManager(new LinearLayoutManager(this));
        rvLeaveRecords.setAdapter(adapter);
    }

    private void setupListeners() {
        etLeaveFromDate.setOnClickListener(v -> showDatePicker(etLeaveFromDate));
        etLeaveToDate.setOnClickListener(v -> showDatePicker(etLeaveToDate));
        etLeaveTypeSelection.setOnClickListener(v -> showLeaveTypeDialog());
        btnApplyLeave.setOnClickListener(v -> {
            vibrate();
            applyLeave();
        });
        
        btnExportLeavePDF.setOnClickListener(v -> {
            vibrate();
            exportLeaveToPDF();
        });
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

    private void showLeaveTypeDialog() {
        String[] leaveTypes = {
            getString(R.string.casual_leave),
            getString(R.string.sick_leave),
            getString(R.string.earned_leave),
            getString(R.string.compensatory_off),
            getString(R.string.other_leave)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Type of Leave")
               .setItems(leaveTypes, (dialog, which) -> {
                   etLeaveTypeSelection.setText(leaveTypes[which]);
               })
               .setNegativeButton("Cancel", null)
               .show();
    }

    private void applyLeave() {
        String leaveFrom = etLeaveFromDate.getText().toString();
        String leaveTo = etLeaveToDate.getText().toString();
        String leaveType = etLeaveTypeSelection.getText().toString();
        String notes = etLeaveNotes.getText().toString();

        if (leaveFrom.isEmpty() || leaveTo.isEmpty() || leaveType.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new leave record
        LeaveRecord leaveRecord = new LeaveRecord();
        leaveRecord.setLeaveFrom(leaveFrom);
        leaveRecord.setLeaveTo(leaveTo);
        leaveRecord.setLeaveType(leaveType);
        leaveRecord.setNotes(notes);
        leaveRecord.setAppliedDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        // Add to list and save
        leaveRecords.add(leaveRecord);
        Collections.sort(leaveRecords, (a, b) -> b.getAppliedDate().compareTo(a.getAppliedDate()));
        saveLeaveRecords();
        
        if (adapter != null) adapter.notifyDataSetChanged();

        // Clear form
        etLeaveFromDate.setText("");
        etLeaveToDate.setText("");
        etLeaveTypeSelection.setText("");
        etLeaveNotes.setText("");

        Toast.makeText(this, "Leave application submitted successfully!", Toast.LENGTH_SHORT).show();
    }

    private void loadLeaveRecords() {
        String recordsJson = sharedPreferences.getString("leave_records", "[]");
        Type listType = new TypeToken<List<LeaveRecord>>(){}.getType();
        List<LeaveRecord> loadedRecords = gson.fromJson(recordsJson, listType);
        
        if (loadedRecords != null) {
            leaveRecords.clear();
            leaveRecords.addAll(loadedRecords);
            Collections.sort(leaveRecords, (a, b) -> b.getAppliedDate().compareTo(a.getAppliedDate()));
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }

    private void saveLeaveRecords() {
        String recordsJson = gson.toJson(leaveRecords);
        sharedPreferences.edit().putString("leave_records", recordsJson).apply();
    }

    @Override
    public void onLeaveDelete(LeaveRecord record, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Leave Application")
            .setMessage("Are you sure you want to delete this leave application?")
            .setPositiveButton("Yes", (dialog, which) -> {
                leaveRecords.remove(position);
                saveLeaveRecords();
                if (adapter != null) adapter.notifyItemRemoved(position);
                Toast.makeText(this, "Leave application deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void exportLeaveToPDF() {
        if (leaveRecords.isEmpty()) {
            Toast.makeText(this, "No leave records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdfFile = new File(getExternalFilesDir(null), "Leave_Records_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf");
            
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.add(new Paragraph("📅 Leave Management Report").setTextAlignment(TextAlignment.CENTER).setFontSize(20));
            document.add(new Paragraph("Generated on: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())).setTextAlignment(TextAlignment.CENTER).setFontSize(12));
            
            // Summary
            int appliedCount = 0, approvedCount = 0, rejectedCount = 0, completedCount = 0;
            for (LeaveRecord record : leaveRecords) {
                switch (record.getStatus()) {
                    case "Applied": appliedCount++; break;
                    case "Approved": approvedCount++; break;
                    case "Rejected": rejectedCount++; break;
                    case "Completed": completedCount++; break;
                }
            }
            
            document.add(new Paragraph("\nSummary:").setFontSize(16));
            document.add(new Paragraph("Total Applications: " + leaveRecords.size()));
            document.add(new Paragraph("Applied: " + appliedCount));
            document.add(new Paragraph("Approved: " + approvedCount));
            document.add(new Paragraph("Rejected: " + rejectedCount));
            document.add(new Paragraph("Completed: " + completedCount));
            
            // Table
            Table table = new Table(5);
            table.addHeaderCell("Leave Period");
            table.addHeaderCell("Type");
            table.addHeaderCell("Status");
            table.addHeaderCell("Applied Date");
            table.addHeaderCell("Notes");
            
            Collections.sort(leaveRecords, (a, b) -> b.getAppliedDate().compareTo(a.getAppliedDate()));
            
            for (LeaveRecord record : leaveRecords) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    
                    Date fromDate = inputFormat.parse(record.getLeaveFrom());
                    Date toDate = inputFormat.parse(record.getLeaveTo());
                    Date appliedDate = inputFormat.parse(record.getAppliedDate());
                    
                    String periodText = outputFormat.format(fromDate) + " to " + outputFormat.format(toDate);
                    table.addCell(periodText);
                    table.addCell(record.getLeaveType());
                    table.addCell(record.getStatus());
                    table.addCell(outputFormat.format(appliedDate));
                    table.addCell(record.getNotes() != null ? record.getNotes() : "");
                } catch (Exception e) {
                    table.addCell(record.getLeaveFrom() + " to " + record.getLeaveTo());
                    table.addCell(record.getLeaveType());
                    table.addCell(record.getStatus());
                    table.addCell(record.getAppliedDate());
                    table.addCell(record.getNotes() != null ? record.getNotes() : "");
                }
            }
            
            document.add(table);
            document.close();
            
            // Share PDF
            Uri pdfUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", pdfFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Leave Management Report");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share Leave Report"));
            Toast.makeText(this, "Leave report exported successfully!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50);
        }
    }
}