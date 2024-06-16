package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.addJobToUser;
import static com.example.shifttracker.FirebaseManager.checkJobTitleAlreadyExists;
import static com.example.shifttracker.FirebaseManager.updateDatabaseUserDocument;
import static com.example.shifttracker.FirebaseManager.updateJobFields;
import static com.example.shifttracker.FirebaseManager.updateRecyclerviewDataset;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import data_models.Job;
import data_models.Shift;

public class EditJobActivity extends AppCompatActivity {
    
    boolean editingExistingJob;
    Toolbar toolbar;
    EditText inputJobTitle, inputHourlyFee, inputExtraHoursAfter, inputExtraHoursRate;
    Button buttonCreate, buttonDeleteJob;
    String originalJobTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_job);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editingExistingJob = getIntent().hasExtra("title");

        inputJobTitle = (EditText) findViewById(R.id.inputJobTitle);
        inputHourlyFee = (EditText) findViewById(R.id.inputHourlyFee);
        inputExtraHoursAfter = (EditText) findViewById(R.id.inputExtraHoursAfter);
        inputExtraHoursRate = (EditText) findViewById(R.id.inputExtraHoursRate);
        buttonCreate = (Button) findViewById(R.id.buttonCreate);
        buttonDeleteJob = (Button) findViewById(R.id.buttonDeleteJob);

        buttonDeleteJob.setVisibility(View.GONE);

        Bundle intentData = getIntent().getExtras();

        if (editingExistingJob) {
            inputJobTitle.setText(intentData.getString("title"));
            originalJobTitle = intentData.getString("title");
            inputHourlyFee.setText(intentData.getString("hourlyFee"));
            inputExtraHoursAfter.setText(intentData.getString("extraHoursAfter"));
            inputExtraHoursRate.setText(intentData.getString("extraHoursRate"));

            buttonCreate.setText("Update Job");
            buttonDeleteJob.setVisibility(View.VISIBLE);
        }

        buttonDeleteJob.setOnClickListener(v -> {showConfirmationDialog();});

        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String title = inputJobTitle.getText().toString().trim();
                String hourlyFeeString = inputHourlyFee.getText().toString().trim();
                String extraHoursAfterString = inputExtraHoursAfter.getText().toString().trim();
                String extraHoursRateString = inputExtraHoursRate.getText().toString().trim();

                if (title.length() == 0 || hourlyFeeString.length() == 0 || extraHoursAfterString.length() == 0 || extraHoursRateString.length() == 0) {
                    Toast.makeText(EditJobActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();

                    if (title.length() == 0) {
                        inputJobTitle.setError("Must Specify Job Title");
                    }
                    if (hourlyFeeString.length() == 0) {
                        inputHourlyFee.setError("Must Specify Hourly Fee");
                    }
                    if (extraHoursAfterString.length() == 0) {
                        inputExtraHoursAfter.setError("Must Specify The Amount of Hours Considered as Standard Shift");
                    }
                    if (extraHoursRateString.length() == 0) {
                        inputExtraHoursRate.setError("Must Specify Multiplication Factor for Extra Hours Pay");
                    }

                    return;
                }

                float hourlyFee = (float) (Math.round(Float.parseFloat(hourlyFeeString) * 100.0) / 100.0);
                float extraHoursAfter = (float) (Math.round(Float.parseFloat(extraHoursAfterString) * 100.0) / 100.0);
                float extraHoursRate = (float) (Math.round(Float.parseFloat(extraHoursRateString) * 100.0) / 100.0);

                if (editingExistingJob) {
                    // Title has been changed
                    if (!title.equals(intentData.getString("title"))) {
                        // Check if new title already exists in database
                        if (checkJobTitleAlreadyExists(title)) {
                            inputJobTitle.setError("A job with this title already exists");
                            Toast.makeText(EditJobActivity.this, "A job with this title already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    // Find job by old title and modify it
                    Job updatedJob = new Job(title, hourlyFee, extraHoursAfter, extraHoursRate, null);
                    String operationResults = updateJobFields(updatedJob, intentData.getString("title"));

                    Toast.makeText(EditJobActivity.this, operationResults.length() == 0 ? "Successfully Updated Job" : operationResults, Toast.LENGTH_SHORT);

                    if (operationResults.length() == 0) {
                        updateRecyclerviewDataset();
                        finish();
                    }
                }
                else {
                    // Check if new title already exists in database
                    if (checkJobTitleAlreadyExists(title)) {
                        inputJobTitle.setError("A job with this title already exists");
                        Toast.makeText(EditJobActivity.this, "A job with this title already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Job createdJob = new Job(title, hourlyFee, extraHoursAfter, extraHoursRate, new ArrayList<Shift>());

                    addJobToUser(createdJob);
                    String operationResults = updateDatabaseUserDocument();

                    Toast.makeText(EditJobActivity.this, operationResults.length() == 0 ? "Operation Successful" : operationResults, Toast.LENGTH_SHORT);

                    if (operationResults.length() == 0) {
                        updateRecyclerviewDataset();
                        finish();
                    }
                }
            }
        });
    }

    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Action");
        builder.setMessage("Are you sure you want to delete this job?\nAll shifts under this job will be deleted and cannot be restored.");

        builder.setPositiveButton("Yes, Delete Job", (dialog, which) -> {
            proceedDeleteJob();
        });

        builder.setNegativeButton("No, Keep this Job", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void proceedDeleteJob() {
        String results = FirebaseManager.deleteJob(originalJobTitle);
        if (results.length() == 0) {
            results = "Successfully Deleted Job " + originalJobTitle;
        }

        Toast.makeText(EditJobActivity.this, results, Toast.LENGTH_LONG).show();
        finish();
    }
}