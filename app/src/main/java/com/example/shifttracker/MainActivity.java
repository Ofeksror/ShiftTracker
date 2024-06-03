package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getInstance;
import static com.example.shifttracker.FirebaseManager.signOut;
import static com.example.shifttracker.FirebaseManager.syncUserWithDatabase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    public String TAG = "MAIN ACTIVITY";
    Toolbar toolbar;
    FragmentContainerView fragmentContainer;
    BottomNavigationView bottomNavigationView;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseManager firebaseManager = getInstance();

        // Check if authenticated
        if (getAuthInstance().getCurrentUser() == null) {
            signOut();
            startActivity(new Intent(this, SignIn.class));
            finish();
        } else {
            // fetch user from database
            syncUserWithDatabase();
        }

        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        fragmentContainer = (FragmentContainerView) findViewById(R.id.fragmentContainer);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigationBar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment;
            if (item.getItemId() == R.id.settingsNavigationItem) {
                fragment = new SettingsFragment();
            }
            else if (item.getItemId() == R.id.jobsNavigationItem) {
                fragment = new JobsFragment();
            }
            else if (item.getItemId() == R.id.shiftsNavigationItem) {
                fragment = new ShiftsFragment();
            }
            else if (item.getItemId() == R.id.statusNavigationItem) {
                fragment = new StatusFragment();
            }
            else {
                return false;
            }
            return loadFragment(fragment);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment == null) {
            return false;
        }

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment, TAG)
                .commit();

        return true;
    }
}