package com.example.shifttracker;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
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
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import data_models.Job;
import data_models.Shift;

public class ShiftsFragment extends Fragment {

    private Spinner jobSpinner;
    private String selectedJobTitle;
    RecyclerView shiftsRecyclerView;
    Button createNewShift, buttonGenerateCsv;
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
        buttonGenerateCsv = (Button) view.findViewById(R.id.buttonGenerateCsv);

        buttonGenerateCsv.setOnClickListener(v -> {
            Job selectedJob = FirebaseManager.findJobByTitle(selectedJobTitle);
            File generatedCsvFile = generateCsvFile((ArrayList<Shift>) selectedJob.getShifts(), selectedJob.getExtraHoursAfter(), selectedJob.getExtraHoursRate());
            if (generatedCsvFile != null) {
                promptUserToDownloadFile(generatedCsvFile);
            }
        });

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

    private File generateCsvFile(ArrayList<Shift> shifts, float extraHoursAfter, float extraHoursRate) {
        File csvFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "shifts.csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            // Write header
            String[] header = {"Start Time", "End Time", "Duration", "Hourly Fee", "Bonus", "Calculated Wage"};
            writer.writeNext(header);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy, HH:mm", Locale.getDefault());

            for (int i = 0; i < shifts.size(); i++) {
                Shift shift = shifts.get(i);
                String startTime = sdf.format(shift.getStartTime());
                String endTime = sdf.format(shift.getEndTime());
                String hourlyFee = String.valueOf(shift.getHourlyFee());
                String bonus = String.valueOf(shift.getBonus());

                float roundedDuration = FirebaseManager.getShiftDuration(shift.getStartTime(), shift.getEndTime());

                // Create Excel formulas
//                String durationFormula = String.format("=TEXT(B%d-A%d,\"[h]:mm\")", i + 2, i + 2);
                String wageFormula = String.format(Locale.US, "=IF(%.2f > %.2f, %.2f*%.2f + (%.2f - %.2f)*%.2f*%.2f + E%d, %.2f*%.2f + E%d)",
                        roundedDuration, extraHoursAfter, extraHoursAfter, Float.parseFloat(hourlyFee),
                        roundedDuration, extraHoursAfter, extraHoursRate, Float.parseFloat(hourlyFee), i + 2,
                        roundedDuration, Float.parseFloat(hourlyFee), i + 2);

                String[] row = {startTime, endTime, String.valueOf(roundedDuration), hourlyFee, bonus, wageFormula};
                writer.writeNext(row);
            }

            return csvFile;

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to generate CSV file", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void promptUserToDownloadFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "text/csv");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Open CSV file");
        startActivity(chooser);


        Toast.makeText(getContext(), "CSV file download started", Toast.LENGTH_SHORT).show();
    }
}