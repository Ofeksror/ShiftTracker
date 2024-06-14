package com.example.shifttracker;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Firebase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import data_models.Shift;

public class ShiftsListAdapter extends RecyclerView.Adapter<ShiftsListAdapter.MyViewHolder> {

    private ArrayList<Shift> dataset;
    private OnClickListener onClickListener;
//    SimpleDateFormat startDateFormat = new SimpleDateFormat("dd/MM, HH:mm ⇨ ");
//    SimpleDateFormat endDateFormat = new SimpleDateFormat("HH:mm");

    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM, EEEE");

    public interface OnClickListener {
        void onClick(int position, Shift shift);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }


    public ShiftsListAdapter(ArrayList<Shift> newDataset) {
        this.dataset = newDataset;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_date, tv_hours, tv_wage;
        private Button btn_edit;

        public MyViewHolder(final View view) {
            super(view);

            tv_date = view.findViewById(R.id.dateTV);
            tv_hours = view.findViewById(R.id.hoursTV);
            tv_wage = view.findViewById(R.id.wageTV);
            btn_edit = view.findViewById(R.id.btn_editShift);
        }
    }

    @NonNull
    @Override
    public ShiftsListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.shift_list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ShiftsListAdapter.MyViewHolder holder, int position) {
        Shift currentShift = dataset.get(position);

        holder.tv_date.setText(dateFormatter.format(currentShift.getStartTime()));
        float shiftDuration = FirebaseManager.getShiftDuration(currentShift.getStartTime(), currentShift.getEndTime());
        holder.tv_hours.setText(String.valueOf(shiftDuration) + " Hours");
        holder.tv_wage.setText("$" + String.valueOf(currentShift.getWage()));

        holder.btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onClick(position, currentShift);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public void updateDataset(ArrayList<Shift> newDataset) {
        dataset.clear();
        if (newDataset == null) {
            return;
        }
        dataset.addAll(newDataset);
        notifyDataSetChanged();
    }
}