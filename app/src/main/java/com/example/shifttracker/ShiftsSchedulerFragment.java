package com.example.shifttracker;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShiftsSchedulerFragment extends Fragment implements ScheduledShiftsAdapter.OnShiftListener {

    private RecyclerView recyclerView;
    private ScheduledShiftsAdapter scheduledShiftsAdapter;
    private List<Date> scheduledShifts;
    private ImageButton addShiftButton;
    private ScheduledShiftsStorage scheduledShiftsStorage;
    private AlarmManager alarmManager;

    public ShiftsSchedulerFragment() {
        super(R.layout.scheduled_shifts_card);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scheduled_shifts_card, container, false);

        alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        scheduledShiftsStorage = new ScheduledShiftsStorage(requireContext());
        scheduledShifts = scheduledShiftsStorage.loadShifts();

        recyclerView = view.findViewById(R.id.recyclerView);
        addShiftButton = view.findViewById(R.id.addShiftButton);

        scheduledShiftsAdapter = new ScheduledShiftsAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(scheduledShiftsAdapter);

        addShiftButton.setOnClickListener(v -> showShiftDialog());

        // Load scheduled shifts
        scheduledShiftsAdapter.setShifts(scheduledShifts);

        return view;
    }

    private void showShiftDialog() {
        Calendar currentDate = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();

        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            selectedDate.set(year, month, day);

            new TimePickerDialog(requireContext(), (timeView, hour, minute) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, hour);
                selectedDate.set(Calendar.MINUTE, minute);
                selectedDate.set(Calendar.SECOND, 0);
                selectedDate.set(Calendar.MILLISECOND, 0);

                Date shiftDate = selectedDate.getTime();
                scheduledShifts.add(shiftDate);
                scheduleNotifications(shiftDate);
                scheduledShiftsStorage.saveShifts(scheduledShifts);
                scheduledShiftsAdapter.setShifts(scheduledShifts);

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), true).show();
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    private void scheduleNotifications(Date shiftDate) {
        scheduleNotification(shiftDate, -2 * 60 * 60 * 1000, 2); // Notify 2 hours before
        scheduleNotification(shiftDate, -45 * 60 * 1000, 45); // Notify 45 minutes before
        scheduleNotification(shiftDate, 0, 0); // Notify at the time of the shift
    }

    private void cancelNotifications(Date scheduledDate) {
        cancelNotification(scheduledDate, -2 * 60 * 60 * 1000, 2);
        cancelNotification(scheduledDate, -45 * 60 * 1000, 45);
        cancelNotification(scheduledDate, 0, 0);
    }

    private void scheduleNotification(Date shiftDate, long millisBeforeShift, int requestCodeSuffix) {
        long notificationTimeInMillis = shiftDate.getTime() + millisBeforeShift;
        long currentTimeInMillis = System.currentTimeMillis();

        // Only schedule the notification if the notification time is in the future
        if (notificationTimeInMillis > currentTimeInMillis) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(notificationTimeInMillis);

            Intent intent = new Intent(requireContext(), NotificationReceiver.class);
            intent.putExtra("shiftDate", shiftDate);
            intent.putExtra("millisBeforeShift", millisBeforeShift);

            int requestCode = (int) (shiftDate.getTime() / 1000 + requestCodeSuffix); // Unique request code for each notification

            PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelNotification(Date shiftDate, long millisBeforeShift, int requestCodeSuffix) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(shiftDate.getTime() + millisBeforeShift);

        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        int requestCode = (int) (shiftDate.getTime() / 1000 + requestCodeSuffix); // Unique request code for each notification

        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onDeleteShift(int position) {
        Date scheduledDate = scheduledShifts.get(position);
        cancelNotifications(scheduledDate);
        scheduledShifts.remove(position);
        scheduledShiftsStorage.deleteShift(position);
        scheduledShiftsAdapter.setShifts(scheduledShifts);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Remove past shifts
        Date now = new Date();
        List<Date> validShifts = new ArrayList<>();
        for (Date shift : scheduledShifts) {
            if (shift.after(now)) {
                validShifts.add(shift);
            }
        }
        scheduledShifts = validShifts;
        scheduledShiftsStorage.saveShifts(scheduledShifts);
        scheduledShiftsAdapter.setShifts(scheduledShifts);
    }
}
