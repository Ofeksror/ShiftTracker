package com.example.shifttracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import data_models.Job;
import data_models.Shift;

public class StatusFragment extends Fragment {

    private BottomSheetDialog bottomSheetDialog;
    private TextView tvShiftStartTime, tvShiftDuration;
    TextView tvEndTime, tvFinalDuration, tvShiftWage;
    private EditText etBonus, etNotes;
    private Button btnEndShift, btnCancelLiveShift;
    private Job selectedJob;
    private EditText etHourlyFee;

    private Date startTime, endTime;
    private float calculatedWage;
    private Timer timer;

    private FirebaseFirestore db;
    private String userId = "exampleUserId"; // Replace with the actual user ID
    private final Handler handler = new Handler(Looper.getMainLooper());
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());


    public StatusFragment() {
        super(R.layout.fragment_status);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container, false);

        userId = FirebaseManager.getUserId();
        db = FirebaseManager.getFirestoreInstance();

        Button btnStartShift = view.findViewById(R.id.start_new_shift_button);
        btnStartShift.setOnClickListener(v -> startShift());

        return view;
    }

    private void startShift() {
        startTime = new Date();
        showBottomSheetDialog();
        startTimer();
        saveShiftState(true);
    }

    private void showBottomSheetDialog() {
        bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.live_shift_tracker, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        tvShiftStartTime = view.findViewById(R.id.tvShiftStartTime);
        tvShiftDuration = view.findViewById(R.id.tvShiftDuration);
        etBonus = view.findViewById(R.id.etBonus);
        etNotes = view.findViewById(R.id.etNotes);
        btnEndShift = view.findViewById(R.id.btnEndShift);
        btnCancelLiveShift = view.findViewById(R.id.btnCancelLiveShift);

        tvShiftStartTime.setText("Since " + sdf.format(startTime) + " (Tap to Edit)");

        tvShiftStartTime.setOnClickListener(v -> setDateTimePickerDialog(tvShiftStartTime, startTime));

        btnEndShift.setOnClickListener(v -> endShift());
        btnCancelLiveShift.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (timer != null) {
                timer.cancel();
            }
            saveShiftState(false);
        });

        bottomSheetDialog.show();
    }

    public void updateTextViewText(TextView tvRef, Date selectedDateTime) {
        if (tvRef == tvShiftStartTime) {
            tvRef.setText("Since " + sdf.format(selectedDateTime) + " (Tap to Edit)");
            startTime = selectedDateTime;
        }
        else if (tvRef == tvEndTime) {
            tvRef.setText(sdf.format(startTime) + " to " + sdf.format(selectedDateTime) + " (Tap to Edit End Time)");
            endTime = selectedDateTime;
            updateDurationTV();

            float wage = FirebaseManager.calculateWage(startTime, endTime, (etHourlyFee.getText().toString().length() == 0) ? 0 : Float.parseFloat(etHourlyFee.getText().toString()), selectedJob.getExtraHoursAfter(), selectedJob.getExtraHoursRate(), (etBonus.getText().toString().length() == 0) ? 0 : Float.parseFloat(etBonus.getText().toString()));
            tvShiftWage.setText("Wage: $" + String.valueOf(wage));
        }
    }

    private void updateDurationTV() {
        // Calculate the difference in milliseconds
        long durationMillis = endTime.getTime() - startTime.getTime();

        // Calculate hours, minutes, and seconds
        long minutes = durationMillis / (1000 * 60) % 60;
        long hours = durationMillis / (1000 * 60 * 60);

        // Format the duration as HH:mm:ss
        String duration = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
        tvFinalDuration.setText("Shift Duration: " + duration);
    }

    public void setDateTimePickerDialog(TextView tvRef, Date dateTimeRef) {
        Calendar currentDate = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(System.currentTimeMillis());

        final Date[] dateTimeObject = new Date[1];

        new DatePickerDialog(getContext(), (view, year, month, day) -> {
            selectedDate.set(year, month, day);

            new TimePickerDialog(getContext(), (timeView, hour, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hour);
                selectedDate.set(Calendar.MINUTE, minute);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);

                updateTextViewText(tvRef, selectedDate.getTime());
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();

        dateTimeRef = dateTimeObject[0];
    }

    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    long durationMillis = new Date().getTime() - startTime.getTime();
                    long seconds = durationMillis / 1000 % 60;
                    long minutes = durationMillis / (1000 * 60) % 60;
                    long hours = durationMillis / (1000 * 60 * 60) % 24;
                    tvShiftDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                });
            }
        }, 0, 1000);
    }

    private void endShift() {
        // Open a dialog where the user can select the job
        Dialog endShiftDialog = new Dialog(getContext());
        endShiftDialog.setContentView(R.layout.dialog_end_shift);

        // Set the size of the dialog to be 25% larger than default
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(endShiftDialog.getWindow().getAttributes());

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = (int) (displayMetrics.widthPixels * 0.85);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;

        layoutParams.width = width;
        layoutParams.height = height;
        endShiftDialog.getWindow().setAttributes(layoutParams);

        Spinner spinnerJobs = endShiftDialog.findViewById(R.id.spinnerJobs);
        etHourlyFee = endShiftDialog.findViewById(R.id.etHourlyFee);
        tvEndTime = endShiftDialog.findViewById(R.id.tvEndTime);
        tvFinalDuration = endShiftDialog.findViewById(R.id.tvFinalDuration);
        tvShiftWage = endShiftDialog.findViewById(R.id.tvShiftWage);
        Button btnCreateShift = endShiftDialog.findViewById(R.id.btnCreateShift);

        loadSpinner(spinnerJobs);

        spinnerJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSelectedJob((String) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // pass
            }
        });

        etHourlyFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                float wage = FirebaseManager.calculateWage(startTime, endTime, (etHourlyFee.getText().toString().length() == 0) ? 0 : Float.parseFloat(etHourlyFee.getText().toString()), selectedJob.getExtraHoursAfter(), selectedJob.getExtraHoursRate(), (etBonus.getText().toString().length() == 0) ? 0 : Float.parseFloat(etBonus.getText().toString()));
                tvShiftWage.setText("Wage: $" + String.valueOf(wage));
            }
        });

        endTime = new Date();
        tvEndTime.setText(sdf.format(startTime) + " to " + sdf.format(endTime) + " (Tap to Edit End Time)");

        tvEndTime.setOnClickListener(v -> {
            setDateTimePickerDialog(tvEndTime, endTime);
        });

        updateDurationTV();

        btnCreateShift.setOnClickListener(v -> {

            float hourlyFee = 0;
            if (!etHourlyFee.getText().toString().equals("")) {
                hourlyFee = Float.parseFloat(etHourlyFee.getText().toString());
            }

            float bonus = 0;
            if (! etBonus.getText().toString().equals("")) {
                bonus = Float.parseFloat(etBonus.getText().toString());
            }

            String notes = etNotes.getText().toString();

            Job job = FirebaseManager.findJobByTitle(spinnerJobs.getSelectedItem().toString());

            float wage = FirebaseManager.calculateWage(startTime, endTime, hourlyFee, job.getExtraHoursAfter(), job.getExtraHoursRate(), bonus);

            FirebaseManager.addShiftToJob(new Shift(startTime, endTime, hourlyFee, bonus, notes, wage), job.getTitle());

            Toast.makeText(getContext(), "Successfully Created Your Shift!", Toast.LENGTH_LONG).show();

            endShiftDialog.dismiss();
            bottomSheetDialog.dismiss();
        });

        endShiftDialog.show();
    }

    private void loadSpinner(Spinner spinnerJobs) {
        ArrayList<String> jobTitles = new ArrayList<String>();
        for (Job job : FirebaseManager.getUserInstance().getJobs()) {
            jobTitles.add(job.getTitle());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, jobTitles);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJobs.setAdapter(spinnerAdapter);
    }

    private void updateSelectedJob(String selectedJobTitle) {
        selectedJob = FirebaseManager.findJobByTitle(selectedJobTitle);
        etHourlyFee.setText(String.valueOf(selectedJob.getHourlyFee()));
    }

    private void saveShiftState(boolean isActive) {
        // Save the state of the shift to SharedPreferences or a database
        // Include fields like startTime, hourlyFee, notes, jobTitle, isActive
    }
}