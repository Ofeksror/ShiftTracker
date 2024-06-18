package com.example.shifttracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScheduledShiftsStorage {

    private static final String PREFS_NAME = "ShiftPrefs";
    private static final String SHIFTS_KEY = "ScheduledShifts";
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public ScheduledShiftsStorage(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveShifts(List<Date> shifts) {
        String json = gson.toJson(shifts);
        sharedPreferences.edit().putString(SHIFTS_KEY, json).apply();
    }

    public List<Date> loadShifts() {
        String json = sharedPreferences.getString(SHIFTS_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        } else {
            Type type = new TypeToken<List<Date>>() {}.getType();
            return gson.fromJson(json, type);
        }
    }

    public void deleteShift(int position) {
        List<Date> shifts = loadShifts();
        if (position >= 0 && position < shifts.size()) {
            shifts.remove(position);
            saveShifts(shifts);
        }
    }
}
