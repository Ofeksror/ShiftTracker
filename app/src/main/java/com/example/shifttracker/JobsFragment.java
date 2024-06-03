package com.example.shifttracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;

import data_models.Job;


public class JobsFragment extends Fragment {

    public static JobsListAdapter jobsListAdapter;
    Button newJobButton;
    RecyclerView jobsRecyclerView;

    public JobsFragment() {
        super(R.layout.fragment_jobs);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_jobs, container, false);

        jobsListAdapter = new JobsListAdapter((ArrayList<Job>) FirebaseManager.getUserInstance().getJobs());

        newJobButton = (Button) view.findViewById(R.id.buttonNewJob);

        newJobButton.setOnClickListener(v -> {
            Intent newJobIntent = new Intent(getContext(), EditJobActivity.class);
            startActivity(newJobIntent);
        });

        jobsRecyclerView = (RecyclerView) view.findViewById(R.id.jobsRecyclerView);
        initiateAdapter();

        return view;
    }

    public void initiateAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext().getApplicationContext());

        jobsRecyclerView.setLayoutManager(layoutManager);
        jobsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        jobsListAdapter.setOnClickListener(new JobsListAdapter.OnClickListener() {
            @Override
            public void onClick(int position, Job job) {
                Intent editJobIntent = new Intent(getContext(), EditJobActivity.class);
                editJobIntent.putExtra("title", job.getTitle());
                editJobIntent.putExtra("hourlyFee", (String) Float.toString(job.getHourlyFee()));
                editJobIntent.putExtra("extraHoursAfter", (String) Float.toString(job.getExtraHoursAfter()));
                editJobIntent.putExtra("extraHoursRate", (String) Float.toString(job.getExtraHoursRate()));

                startActivity(editJobIntent);
            }
        });

        jobsRecyclerView.setAdapter(jobsListAdapter);
    }
}