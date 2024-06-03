package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.signOut;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {
    public String TAG = "MAIN ACTIVITY";
    private Button signOutButton, userDetailsButton;

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
        userDetailsButton = (Button) view.findViewById(R.id.userDetailsButton);
         signOutButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                signOutUser();
             }
         });
         userDetailsButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 String userId = getAuthInstance().getCurrentUser().getUid();
                 String email = getAuthInstance().getCurrentUser().getEmail();

                 Log.d(TAG, "USER DETAILS: " + email + " " + userId);
             }
         });

        return view;
    }

    public void signOutUser() {
        signOut();
        startActivity(new Intent(getActivity(), SignIn.class));
    }

}