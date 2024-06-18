package com.example.shifttracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduledShiftsAdapter extends RecyclerView.Adapter<ScheduledShiftsAdapter.ShiftViewHolder> {

    private List<Date> scheduledDates = new ArrayList<>();
    private OnShiftListener onShiftListener;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    public interface OnShiftListener {
        void onDeleteShift(int position);
    }

    public ScheduledShiftsAdapter(OnShiftListener onShiftListener) {
        this.onShiftListener = onShiftListener;
    }

    public void setShifts(List<Date> scheduledDates) {
        this.scheduledDates = scheduledDates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.scheduled_shift_item, parent, false);
        return new ShiftViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftViewHolder holder, int position) {
        Date scheduledDate = scheduledDates.get(position);
        holder.textViewDate.setText(sdf.format(scheduledDate));

        holder.deleteButton.setOnClickListener(v -> onShiftListener.onDeleteShift(position));
    }

    @Override
    public int getItemCount() {
        return scheduledDates.size();
    }

    static class ShiftViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDate;
        ImageButton deleteButton;

        public ShiftViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
