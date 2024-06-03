package com.example.shifttracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.type.DateTime;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;

import data_models.Job;
import data_models.Shift;

public class ShiftsFragment extends Fragment {

    private Spinner jobSpinner;
    private String selectedJobTitle;
    RecyclerView shiftsRecyclerView;
    Button createNewShift;
    private ShiftsListAdapter adapter;

    private ArrayList<String> jobTitles = new ArrayList<String>();

    public ShiftsFragment() {
        super(R.layout.fragment_shifts);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shifts, container, false);

        jobSpinner = (Spinner) view.findViewById(R.id.jobsSpinner);
        shiftsRecyclerView = (RecyclerView) view.findViewById(R.id.shiftsRecyclerView);
        createNewShift = (Button) view.findViewById(R.id.createNewShift);

        createNewShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent newShiftIntent = new Intent(getContext(), EditShiftActivity.class);
                newShiftIntent.putExtra("selectedJobTitle", selectedJobTitle);
//                startActivity(newShiftIntent);
                activityLauncher.launch(newShiftIntent);
            }
        });

        adapter = new ShiftsListAdapter(new ArrayList<Shift>());
        shiftsRecyclerView.setVisibility(View.GONE);

        initiateAdapter();
        loadJobTitles();
        setupSpinner();

        return view;
    }

    ActivityResultLauncher<Intent> activityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == 1) {
                        // Created a new shift
                        Intent intent = result.getData();

                        if (intent == null)
                            return;

                        // Match spinner
                        String spinnerJobTitle = intent.getStringExtra("jobTitle");
                        int i = 0;
                        for (String title : jobTitles) {
                            if (spinnerJobTitle.equals(title)) {
                                break;
                            }
                            i++;
                        }
                        jobSpinner.setSelection(i);

                        return;
                    }
                    else if (result.getResultCode() == 2) {
                        // Replacing Shift, Same Job
                        Intent intent = result.getData();
                        if (intent == null) return;

                        // Match spinner
                        String spinnerJobTitle = intent.getStringExtra("jobTitle");
                        int i = 0;
                        for (String title : jobTitles) {
                            if (spinnerJobTitle.equals(title)) {
                                break;
                            }
                            i++;
                        }
                        jobSpinner.setSelection(i);

                        // Replace shift object
                        int shiftIndex = intent.getIntExtra("shiftIndex", -1);
                        Shift modifiedShift = new Shift(new Date(intent.getLongExtra("startTime", -1)),
                                new Date(intent.getLongExtra("endTime", -1)),
                                intent.getFloatExtra("hourlyFee", -1),
                                intent.getFloatExtra("bonus", -1),
                                intent.getStringExtra("notes"),
                                intent.getFloatExtra("wage", -1));

                        String errorMessage = FirebaseManager.replaceShiftAtIndex(shiftIndex, modifiedShift, spinnerJobTitle);
                        Toast.makeText(getContext(), (errorMessage.length() == 0) ? "Successfully Updated Shift" : errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }
                    else {
                        return;
                    }
                }
            }
    );

    @Override
    public void onResume() {
        super.onResume();
        // Get updated shifts array from job
        adapter.updateDataset((ArrayList<Shift>) FirebaseManager.findJobByTitle(jobSpinner.getSelectedItem().toString()).getShifts());
    }

    public void loadJobTitles() {
        ArrayList<Job> jobs = (ArrayList<Job>) FirebaseManager.getUserInstance().getJobs();
        for (Job job : jobs) {
            jobTitles.add(job.getTitle());
        }
    }

    public void initiateAdapter() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext().getApplicationContext());

        shiftsRecyclerView.setLayoutManager(layoutManager);
        shiftsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter.setOnClickListener(new ShiftsListAdapter.OnClickListener() {
            @Override
            public void onClick(int position, Shift shift) {
                Intent editShiftIntent = new Intent(getContext(), EditShiftActivity.class);
                editShiftIntent.putExtra("selectedJobTitle", jobSpinner.getSelectedItem().toString());
                editShiftIntent.putExtra("startTime", shift.getStartTime().getTime());
                editShiftIntent.putExtra("endTime", shift.getEndTime().getTime());
                editShiftIntent.putExtra("hourlyFee", shift.getHourlyFee());
                editShiftIntent.putExtra("bonus", shift.getBonus());
                editShiftIntent.putExtra("notes", shift.getNotes());
                editShiftIntent.putExtra("index", position);

                activityLauncher.launch(editShiftIntent);
            }
        });

        shiftsRecyclerView.setAdapter(adapter);
    }

    public void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, jobTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        jobSpinner.setAdapter(spinnerAdapter);

        jobSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedJobTitle = jobTitles.get(position);
                loadShiftsForJob(selectedJobTitle);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
                selectedJobTitle = "";
            }
        });
    }

    public void loadShiftsForJob(String selectedJobTitle) {
        if (selectedJobTitle == null || selectedJobTitle.length() == 0) {
            return;
        }

        ArrayList<Job> jobs = (ArrayList<Job>) FirebaseManager.getUserInstance().getJobs();

        for (Job job : jobs) {
            if (selectedJobTitle.equals(job.getTitle())) {

                ArrayList<Shift> shifts = (ArrayList<Shift>) job.getShifts();

                if (shifts == null || shifts.isEmpty()) {
                    shiftsRecyclerView.setVisibility(View.GONE);
                }
                else {
                    adapter.updateDataset(shifts);
                    shiftsRecyclerView.setVisibility(View.VISIBLE);
                }

            }
        }

    }
}