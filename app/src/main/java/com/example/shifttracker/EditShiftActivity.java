package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.findJobByTitle;
import static com.example.shifttracker.FirebaseManager.getUserInstance;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import data_models.Job;
import data_models.Shift;

public class EditShiftActivity extends AppCompatActivity {
    boolean editingExistingShift;
    Toolbar toolbar;
    Spinner jobSpinner;
    TextView inputStartTime, inputEndTime;
    Date startDateTime, endDateTime;
    EditText inputHourlyFeeOptional, inputBonus, inputNotes;
    Button buttonSubmit, buttonDelete;
    int shiftIndex;
    String selectedJobTitle;
    Bundle intentData;
    SimpleDateFormat dateFormat  = new SimpleDateFormat("dd/MM, EEEE, HH:mm");

    private ArrayList<String> jobTitles = new ArrayList<String>();

    public void loadJobTitles() {
        ArrayList<Job> jobs = (ArrayList<Job>) FirebaseManager.getUserInstance().getJobs();
        for (Job job : jobs) {
            jobTitles.add(job.getTitle());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_shift);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        jobSpinner = (Spinner) findViewById(R.id.spinner);
        inputStartTime = (TextView) findViewById(R.id.inputStartTime);
        inputEndTime = (TextView) findViewById(R.id.inputEndTime);
        inputHourlyFeeOptional = (EditText) findViewById(R.id.inputHourlyFeeOptional);
        inputBonus = (EditText) findViewById(R.id.inputBonus);
        inputNotes = (EditText) findViewById(R.id.inputNotes);
        buttonSubmit = (Button) findViewById(R.id.buttonCreate);
        buttonDelete = (Button) findViewById(R.id.buttonDelete);

        inputStartTime.setOnClickListener(view -> setDateTimePickerDialog(inputStartTime, startDateTime));
        inputEndTime.setOnClickListener(view -> setDateTimePickerDialog(inputEndTime, endDateTime));

        loadJobTitles();
        setupSpinner();

        intentData = getIntent().getExtras();

        if (getIntent().hasExtra("selectedJobTitle")) {
            jobSpinner.setSelection(indexOfSelectedJob(intentData.getString("selectedJobTitle")));
        }

        if (getIntent().hasExtra("startTime")) {
            editingExistingShift = true;
            buttonDelete.setVisibility(View.VISIBLE);

            startDateTime = (Date) new Date(intentData.getLong("startTime"));
            endDateTime = (Date) new Date(intentData.getLong("endTime"));

            inputStartTime.setText(dateFormat.format(startDateTime));
            inputEndTime.setText(dateFormat.format(endDateTime));

            inputHourlyFeeOptional.setText(String.valueOf(intentData.get("hourlyFee")));
            inputBonus.setText(String.valueOf(intentData.get("bonus")));
            inputNotes.setText(intentData.get("notes").toString());

            shiftIndex = Integer.parseInt(intentData.get("index").toString());
            buttonSubmit.setText("Update Job");
        }
        else {
            editingExistingShift = false;
            buttonDelete.setVisibility(View.GONE);
        }


        buttonSubmit.setOnClickListener(view -> submitShift());
        buttonDelete.setOnClickListener(view -> deleteShift());
    }

    public void deleteShift() {
        String errorMessage = FirebaseManager.removeShiftAtIndexFromJob(shiftIndex, intentData.get("selectedJobTitle").toString());
        Toast.makeText(this, (errorMessage.length() == 0) ? "Successfully Removed Shift" : errorMessage, Toast.LENGTH_LONG).show();
        finish();
    }

    public void submitShift() {
        // Check for empty date-time fields
        if (startDateTime == null || endDateTime == null || inputStartTime.getText().toString().equals("") || inputEndTime.getText().toString().equals("")) {
            // Raise error
            if (startDateTime == null) {
                inputStartTime.setError("This field is mandatory");
            }
            if (endDateTime == null) {
                inputEndTime.setError("This field is mandatory");
            }

            Toast.makeText(EditShiftActivity.this, "Please fill the Date-Time Fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if endDate is after startDate
        if (endDateTime.before(startDateTime) || endDateTime.equals(startDateTime)) {
            inputEndTime.setError("Shift's ending time must be after its starting time");
            Toast.makeText(EditShiftActivity.this, "Shift's ending time must be after its starting time", Toast.LENGTH_LONG).show();
            return;
        }

        Job job = findJobByTitle(selectedJobTitle);

        float hourlyFee;
        if (inputHourlyFeeOptional.getText().toString().length() == 0) {
            hourlyFee = job.getHourlyFee();
            if (hourlyFee == 0) {
                Toast.makeText(EditShiftActivity.this, "Job's default hourly fee is missing. Please specify an hourly fee for this shift", Toast.LENGTH_LONG).show();
                inputHourlyFeeOptional.setError("Missing default hourly fee. Please specify one for this shift");
                return;
            }
        }
        else {
            hourlyFee = Float.parseFloat(inputHourlyFeeOptional.getText().toString());
        }
        float bonus = ((inputBonus.getText().toString().length() == 0) ? 0 : Float.parseFloat(inputBonus.getText().toString()));
        String notes = inputNotes.getText().toString();

        float wage = FirebaseManager.calculateWage(startDateTime, endDateTime, hourlyFee, job.getExtraHoursAfter(), job.getExtraHoursRate());

        if (editingExistingShift) {
            // Modifying an existing job
            Intent resultsIntent = new Intent();

            // Same job?
            if (jobSpinner.getSelectedItem().toString().equals(intentData.get("selectedJobTitle"))) {
                // Replace at shift index
                resultsIntent.putExtra("jobTitle", jobSpinner.getSelectedItem().toString());
                resultsIntent.putExtra("shiftIndex", shiftIndex);

                // Shift object map
                resultsIntent.putExtra("startTime", startDateTime.getTime());
                resultsIntent.putExtra("endTime", endDateTime.getTime());
                resultsIntent.putExtra("hourlyFee", hourlyFee);
                resultsIntent.putExtra("bonus", bonus);
                resultsIntent.putExtra("notes", notes);
                resultsIntent.putExtra("wage", wage);

                setResult(2, resultsIntent);
                finish();

                return;
            }
            else {
                // Remove from old job
                FirebaseManager.removeShiftAtIndexFromJob(shiftIndex, intentData.get("selectedJobTitle").toString());

                // Add to new job
            }
        }

        Shift createdShift = new Shift(startDateTime, endDateTime, hourlyFee, bonus, notes, wage);
        String errorMessage = FirebaseManager.addShiftToJob(createdShift, selectedJobTitle);

        if (errorMessage.length() != 0) {
            Toast.makeText(EditShiftActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            return;
        } else {
            Toast.makeText(EditShiftActivity.this, editingExistingShift ? "Successfully Modified Shift" : "Successfully Created New Shift", Toast.LENGTH_LONG).show();
            Intent resultsIntent = new Intent();
            resultsIntent.putExtra("jobTitle", jobSpinner.getSelectedItem().toString());
            setResult(1, resultsIntent);
            finish();
        }
    }


    public void updateTextViewText(TextView tvRef, Date selectedDateTime) {
        tvRef.setText(dateFormat.format(selectedDateTime));
        if (tvRef == inputStartTime) {
            startDateTime = selectedDateTime;
        }
        else if (tvRef == inputEndTime) {
            endDateTime = selectedDateTime;
        }
    }

    public void setDateTimePickerDialog(TextView tvRef, Date dateTimeRef) {
        Calendar currentDate = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(System.currentTimeMillis());

        final Date[] dateTimeObject = new Date[1];

        new DatePickerDialog(EditShiftActivity.this, (view, year, month, day) -> {
            selectedDate.set(year, month, day);

            new TimePickerDialog(EditShiftActivity.this, (timeView, hour, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hour);
                selectedDate.set(Calendar.MINUTE, minute);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);

                updateTextViewText(tvRef, selectedDate.getTime());
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();

        dateTimeRef = dateTimeObject[0];
    }

    public int indexOfSelectedJob(String jobTitle) {
        int i = 0;
        for (String title : jobTitles) {
            if (jobTitle.equals(title)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void setupSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, jobTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        jobSpinner.setAdapter(spinnerAdapter);

        jobSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedJobTitle = jobTitles.get(position);
                setSelectedJobTitle(selectedJobTitle);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    public void setSelectedJobTitle(String job) {
        selectedJobTitle = job;
    }
}