package com.example.shifttracker;

import static android.app.Activity.RESULT_OK;
import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getUserId;
import static com.example.shifttracker.FirebaseManager.getUserInstance;
import static com.example.shifttracker.FirebaseManager.signOut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingsFragment extends Fragment {
    public String TAG = "OFEK";
    private TextView emailTV;
    private Button signOutButton, changePfpButton, buttonUpdateGoals;
    private ImageView pfpImageView;
    private Uri imageUri;

    private ActivityResultLauncher<String> selectImageLauncher;
    private ActivityResultLauncher<Intent> takePhotoLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: SettingsFragment created");

        // Initialize the activity result launcher for selecting an image from the gallery
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null) {
                        startCrop(result);
                    }
                }
        );

        // Initialize the activity result launcher for taking a photo
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        startCrop(imageUri);
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "cropImageLauncher: onActivityResult called");
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri croppedImageUri = UCrop.getOutput(result.getData());
                        if (croppedImageUri != null) {
                            Log.d(TAG, "cropImageLauncher: Cropped image URI: " + croppedImageUri.toString());
                            imageUri = croppedImageUri; // Update imageUri to the new cropped image URI
                            pfpImageView.setImageURI(null);
                            pfpImageView.setImageURI(imageUri);
                            updateUserProfilePicture(croppedImageUri); // Save to Firebase Storage and update Firestore
                        } else {
                            Log.e(TAG, "cropImageLauncher: Cropped image URI is null");
                        }
                    } else {
                        Log.e(TAG, "cropImageLauncher: Result not OK or data is null");
                    }
                }
        );

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: SettingsFragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: SettingsFragment paused");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView: SettingsFragment view created");

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        emailTV = (TextView) view.findViewById(R.id.emailTV);
        emailTV.setText("Logged in as " + FirebaseManager.getAuthInstance().getCurrentUser().getEmail());

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

         updateProfileImage();

         buttonUpdateGoals = (Button) view.findViewById(R.id.buttonUpdateGoals);
         buttonUpdateGoals.setOnClickListener(v -> {showUpdateGoalsDialog();});

        return view;
    }

    private void updateProfileImage() {
        String userId = getUserId(); // Get the current user's unique ID
        FirebaseManager.getFirestoreInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profilePicUrl = documentSnapshot.getString("profilePicUrl");
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            Uri profilePicUri = Uri.parse(profilePicUrl);
                            // Fetch the image from the URL and set it to the ImageView
                            new Thread(() -> {
                                try {
                                    Bitmap bitmap = BitmapFactory.decodeStream(new java.net.URL(profilePicUrl).openStream());
                                    requireActivity().runOnUiThread(() -> {
                                        imageUri = profilePicUri;
                                        pfpImageView.setImageBitmap(bitmap);
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load profile picture.", Toast.LENGTH_SHORT).show());
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

    private void startCrop(@NonNull Uri uri) {
        Log.d(TAG, "startCrop: Starting UCrop with URI: " + uri.toString());
        String destinationFileName = "cropped";
        destinationFileName += ".jpg";

        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(450, 450);
        Intent intent = uCrop.getIntent(requireContext());

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            Log.d(TAG, "startCrop: UCrop Intent is valid, launching cropImageLauncher");
            cropImageLauncher.launch(intent);
        } else {
            Log.e(TAG, "startCrop: UCrop Intent could not be resolved");
        }
    }


    private void updateUserProfilePicture(Uri uri) {
        String userId = getUserId(); // Get the current user's unique ID
        FirebaseStorage storage = FirebaseManager.getStorageInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference profilePicRef = storageRef.child("profilePictures/" + userId + ".jpg");

        profilePicRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> profilePicRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    FirebaseManager.getFirestoreInstance().collection("users").document(userId)
                            .update("profilePicUrl", downloadUri.toString())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Profile picture updated successfully.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update profile picture in Firestore.", Toast.LENGTH_SHORT).show());
                }))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to upload profile picture.", Toast.LENGTH_SHORT).show());
    }
}