package com.example.shifttracker;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import data_models.Job;

public class JobsListAdapter extends RecyclerView.Adapter<JobsListAdapter.MyViewHolder> {

    private ArrayList<Job> jobsDataSet;
    private OnClickListener onClickListener;

    public interface OnClickListener {
        void onClick(int position, Job job);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }


    public JobsListAdapter(ArrayList<Job> dataSet) {
        this.jobsDataSet = dataSet;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_jobTitle, tv_hourlyFee;
        private Button btn_editJob;

        public MyViewHolder(final View view) {
            super(view);

            tv_jobTitle = view.findViewById(R.id.dateTV);
            tv_hourlyFee = view.findViewById(R.id.hourlyFeeTv);
            btn_editJob = view.findViewById(R.id.btn_editJob);
        }
    }

    @NonNull
    @Override
    public JobsListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.job_list_items, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull JobsListAdapter.MyViewHolder holder, int position) {
        Job currentJob = jobsDataSet.get(position);
        holder.tv_jobTitle.setText(currentJob.getTitle());
        holder.tv_hourlyFee.setText("$" + String.valueOf(currentJob.getHourlyFee()) + "/hour");

        holder.btn_editJob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onClickListener != null) {
                    onClickListener.onClick(position, currentJob);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobsDataSet.size();
    }

    public void updateJobsList(ArrayList<Job> newJobsDataset) {
        if (newJobsDataset == null) {
            jobsDataSet.clear();
            return;
        }
        jobsDataSet = newJobsDataset;
        notifyDataSetChanged();
    }
}