package com.example.shifttracker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NotificationReceiver extends BroadcastReceiver {

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        Date shiftDate = (Date) intent.getSerializableExtra("shiftDate");
        long millisBeforeShift = intent.getLongExtra("millisBeforeShift", 0);

        String contentTitle, contentText;

        if (millisBeforeShift == 0) {
            contentTitle = "Shift starting now";
            contentText = "Your shift is starting now! Open the app to start tracking it.";
        } else if (millisBeforeShift == -2 * 60 * 60 * 1000) {
            contentTitle = "Upcoming shift at " + sdf.format(shiftDate);
            contentText = "Your shift is starting in 2 hours.";
        } else if (millisBeforeShift == -45 * 60 * 1000) {
            contentTitle = "Upcoming shift at " + sdf.format(shiftDate);
            contentText = "Your shift is starting in 45 minutes.";
        } else {
            contentTitle = "Upcoming Shift";
            contentText = "Your shift is starting soon.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "SHIFT_NOTIFICATION_CHANNEL")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) (shiftDate.getTime() + millisBeforeShift), builder.build());
    }
}
