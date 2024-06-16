package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getUserInstance;
import static com.example.shifttracker.FirebaseManager.signOut;

import android.app.AlertDialog;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.text.SimpleDateFormat;

public class SettingsFragment extends Fragment {
    public String TAG = "MAIN ACTIVITY";
    private Button signOutButton, changePfpButton, buttonUpdateGoals;
    private ImageView pfpImageView;

    private static final int REQUEST_IMAGE_GALLERY = 1;
    private static final int REQUEST_IMAGE_CAMERA = 2;

    private Uri photoUri;
    private String currentPhotoPath;


    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        signOutButton = (Button) view.findViewById(R.id.signOutButton);

         signOutButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                signOutUser();
             }
         });

         changePfpButton = (Button) view.findViewById(R.id.btnChangePFP);
         pfpImageView = (ImageView) view.findViewById(R.id.imageViewPFP);

         buttonUpdateGoals = (Button) view.findViewById(R.id.buttonUpdateGoals);

         buttonUpdateGoals.setOnClickListener(v -> {showUpdateGoalsDialog();});

        return view;
    }

    private void showUpdateGoalsDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_update_goals, null);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        // Get the EditText fields
        EditText etTargetMonthlyIncome = dialogView.findViewById(R.id.etTargetMonthlyIncome);
        EditText etTargetWeeklyHours = dialogView.findViewById(R.id.etTargetWeeklyHours);

        etTargetMonthlyIncome.setText(String.valueOf(getUserInstance().getTargetMonthlyIncome()));
        etTargetWeeklyHours.setText(String.valueOf(getUserInstance().getTargetWeeklyHours()));

        // Set up the buttons
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button updateButton = dialogView.findViewById(R.id.updateButton);

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> {
            // Discard the dialog
            dialog.dismiss();
        });

        updateButton.setOnClickListener(v -> {
            // Trigger a function and pass the values of the EditText fields
            int targetMonthlyIncome = Integer.parseInt(etTargetMonthlyIncome.getText().toString());
            int targetWeeklyHours = Integer.parseInt(etTargetWeeklyHours.getText().toString());

            FirebaseManager.updateGoals(targetMonthlyIncome, targetWeeklyHours);

            // Dismiss the dialog
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }

    public void signOutUser() {
        signOut();
        startActivity(new Intent(getActivity(), SignIn.class));
    }
}