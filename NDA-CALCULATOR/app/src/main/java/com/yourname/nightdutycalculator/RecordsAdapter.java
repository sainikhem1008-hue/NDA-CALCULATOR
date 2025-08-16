package com.yourname.nightdutycalculator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.ViewHolder> {
    private List<DutyRecord> records;
    private OnRecordDeleteListener deleteListener;
    private DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public interface OnRecordDeleteListener { void onRecordDelete(DutyRecord record, int position); }

    public RecordsAdapter(List<DutyRecord> records, OnRecordDeleteListener deleteListener) {
        this.records = records;
        this.deleteListener = deleteListener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DutyRecord record = records.get(position);
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = inputFormat.parse(record.getDate());
            holder.tvDate.setText(outputFormat.format(date));
        } catch (Exception e) { holder.tvDate.setText(record.getDate()); }
        holder.tvTime.setText(record.getDutyFrom() + " - " + record.getDutyTo());
        holder.tvHours.setText(String.format("%.1f hrs", record.getTotalNightHours()));
        
        // Determine type text based on various conditions
        String typeText;
        if (record.isNationalHoliday() && record.isWeeklyRest()) {
            typeText = "🎉 Holiday Allowance Paid";
        } else if (record.isNationalHoliday()) {
            typeText = "🎉 Holiday";
        } else if (record.isWeeklyRest()) {
            typeText = "🌅 Weekly Rest";
        } else {
            typeText = "📅 Regular";
        }
        
        holder.tvType.setText(typeText);
        
        // Set color based on type
        if (record.isNationalHoliday() && record.isWeeklyRest()) {
            holder.tvType.setTextColor(Color.parseColor("#9C27B0")); // purple for both
        } else if (record.isNationalHoliday()) {
            holder.tvType.setTextColor(Color.parseColor("#f39c12")); // orange
        } else if (record.isWeeklyRest()) {
            holder.tvType.setTextColor(Color.parseColor("#4CAF50")); // green
        } else {
            holder.tvType.setTextColor(Color.BLACK);
        }
        
        // Display allowance with status
        if (record.getAllowanceStatus() != null && record.getAllowanceStatus().startsWith("❌")) {
            holder.tvAllowance.setText(record.getAllowanceStatus());
            holder.tvAllowance.setTextColor(Color.parseColor("#e74c3c")); // red for no allowance
        } else {
            holder.tvAllowance.setText("₹" + decimalFormat.format(record.getNightDutyAllowance()));
            holder.tvAllowance.setTextColor(Color.parseColor("#f39c12")); // orange for normal allowance
        }
        holder.btnDelete.setOnClickListener(v -> { if (deleteListener != null) deleteListener.onRecordDelete(record, position); });
    }

    @Override public int getItemCount() { return records.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvHours, tvType, tvAllowance;
        MaterialButton btnDelete;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHours = itemView.findViewById(R.id.tvHours);
            tvType = itemView.findViewById(R.id.tvType);
            tvAllowance = itemView.findViewById(R.id.tvAllowance);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
