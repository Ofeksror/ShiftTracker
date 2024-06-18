package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getUserInstance;
import static com.example.shifttracker.FirebaseManager.signOut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingsFragment extends Fragment {
    public String TAG = "MAIN ACTIVITY";
    private Button signOutButton, changePfpButton, buttonUpdateGoals;
    private ImageView pfpImageView;
    private Uri imageUri;

    private ActivityResultLauncher<String> selectImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the activity result launcher for selecting an image from the gallery
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null) {
                        pfpImageView.setImageURI(result);
                    }
                }
        );

        // Initialize the activity result launcher for taking a photo
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        pfpImageView.setImageURI(imageUri);
                    }
                }
        );
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
         changePfpButton.setOnClickListener(v -> showImagePickerOptions());

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

    private void showImagePickerOptions() {
        String[] options = {"Select from Gallery", "Take a Photo"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        selectImageFromGallery();
//                        if (checkAndRequestPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {}
                    } else if (which == 1) {
                        if (checkAndRequestPermissions(android.Manifest.permission.CAMERA)) {
                            takePhoto();
                        }
                    }
                })
                .show();
    }

    private boolean checkAndRequestPermissions(@NonNull String permission) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            requestPermissionLauncher.launch(permission);
            return false;
        } else {
            return true; // Permission already granted
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted) {
                    Toast.makeText(requireContext(), "Permission denied. Cannot proceed with action.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void selectImageFromGallery() {
        selectImageLauncher.launch("image/*");
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(requireContext(), "com.example.shifttracker.provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                takePhotoLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }
}