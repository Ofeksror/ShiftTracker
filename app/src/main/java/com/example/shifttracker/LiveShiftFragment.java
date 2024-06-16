package com.example.shifttracker;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import data_models.Job;
import data_models.Shift;

public class LiveShiftFragment extends Fragment {

    private BottomSheetDialog bottomSheetDialog;
    private TextView tvShiftStartTime, tvShiftDuration;
    TextView tvEndTime, tvFinalDuration, tvShiftWage;
    private EditText etBonus, etNotes;
    private Button btnEndShift, btnCancelLiveShift;
    private Job selectedJob;
    private EditText etHourlyFee;
    private float calculatedWage;
    private Timer timer;
    private Date startTime, endTime;
    private final Handler handler = new Handler(Looper.getMainLooper());
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());



    public LiveShiftFragment() {
        super(R.layout.fragment_live_shift);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_shift, container, false);

        Button btnStartShift = view.findViewById(R.id.start_new_shift_button);
        btnStartShift.setOnClickListener(v -> startShift());

        createNotificationChannel();

        return view;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "ShiftNotificationChannel";
            String description = "Channel for shift duration notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("SHIFT_NOTIFICATION_CHANNEL", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startShift() {
        startTime = new Date();
        showBottomSheetDialog();
        startTimer();
    }

    private void showBottomSheetDialog() {
        bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_live_shift, null);
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
        });

        bottomSheetDialog.show();
    }

    private void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    long durationMillis = new Date().getTime() - startTime.getTime();
                    int seconds = (int) (durationMillis / 1000 % 60);
                    int minutes = (int) (durationMillis / (1000 * 60) % 60);
                    int hours = (int) (durationMillis / (1000 * 60 * 60) % 24);

                    // Send notification every whole hour
                    if (seconds == 0 && minutes == 0 && hours > 0) {
                        sendNotification(hours);
                    }

                    // DEMO !
                    // Send notification every 5 seconds
                    if (seconds % 5 == 0 && seconds > 0) {
                        sendNotification(seconds);
                    }

                    if (minutes == 0 && seconds == 0 && hours > 0) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions((Activity) requireContext(), new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                            } else {
                                sendNotification(hours);
                            }
                        } else {
                            sendNotification(hours);
                        }
                    }

                    tvShiftDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                });
            }
        }, 0, 1000);
    }

    private void sendNotification(int hours) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "SHIFT_NOTIFICATION_CHANNEL")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Shift Duration Alert")
                .setContentText("It has been " + hours + " hours since your shift started.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission to send notifications was not granted.", Toast.LENGTH_LONG).show();
        }
        else {
            notificationManager.notify(hours, builder.build());
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

    public void updateTextViewText(TextView tvRef, Date selectedDateTime) {
        if (tvRef == tvShiftStartTime) {
            tvRef.setText("Since " + sdf.format(selectedDateTime) + " (Tap to Edit)");
            startTime = selectedDateTime;

        } else if (tvRef == tvEndTime) {
            tvRef.setText(sdf.format(startTime) + " to " + sdf.format(selectedDateTime) + " (Tap to Edit End Time)");
            endTime = selectedDateTime;
            updateDurationTV();

            float wage = FirebaseManager.calculateWage(startTime, endTime, (etHourlyFee.getText().toString().length() == 0) ? 0 : Float.parseFloat(etHourlyFee.getText().toString()), selectedJob.getExtraHoursAfter(), selectedJob.getExtraHoursRate(), (etBonus.getText().toString().length() == 0) ? 0 : Float.parseFloat(etBonus.getText().toString()));
            tvShiftWage.setText("Wage: $" + String.valueOf(wage));
        }
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


    private void updateSelectedJob(String selectedJobTitle) {
        selectedJob = FirebaseManager.findJobByTitle(selectedJobTitle);
        etHourlyFee.setText(String.valueOf(selectedJob.getHourlyFee()));
    }


}
