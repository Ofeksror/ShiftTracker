package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.signOut;

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
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.text.SimpleDateFormat;

public class SettingsFragment extends Fragment {
    public String TAG = "MAIN ACTIVITY";
    private Button signOutButton, changePfpButton;
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

        return view;
    }

    public void signOutUser() {
        signOut();
        startActivity(new Intent(getActivity(), SignIn.class));
    }
}