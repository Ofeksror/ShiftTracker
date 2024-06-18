package com.example.shifttracker;

import static com.example.shifttracker.FirebaseManager.getAuthInstance;
import static com.example.shifttracker.FirebaseManager.getInstance;
import static com.example.shifttracker.FirebaseManager.getUserInstance;
import static com.example.shifttracker.FirebaseManager.signOut;
import static com.example.shifttracker.FirebaseManager.syncUserWithDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements SyncCallback {
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
            syncUserWithDatabase((SyncCallback) this);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA}, 99);
        }

        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        fragmentContainer = (FragmentContainerView) findViewById(R.id.fragmentContainer);

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigationBar);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment;
            String tag;
            if (item.getItemId() == R.id.settingsNavigationItem) {
                fragment = new SettingsFragment();
                tag = "SettingsFragment";
            }
            else if (item.getItemId() == R.id.jobsNavigationItem) {
                fragment = new JobsFragment();
                tag = "JobsFragment";
            }
            else if (item.getItemId() == R.id.shiftsNavigationItem) {
                fragment = new ShiftsFragment();
                tag = "ShiftsFragment";
            }
            else if (item.getItemId() == R.id.statusNavigationItem) {
                fragment = new StatusFragment();
                tag = "StatusFragment";
            }
            else {
                tag = getCurrentFragment();
                Log.d("Ofek", "Loaded tag: " + tag);
                fragment = getFragmentByTag(tag);
            }
            return loadFragment(fragment, tag);
        });
    }

    private Fragment getFragmentByTag(String tag) {
        Fragment fragment = new StatusFragment();
        switch (tag) {
            case "SettingsFragment":
                fragment = new SettingsFragment();
            case "JobsFragment":
                fragment = new JobsFragment();
            case "ShiftsFragment":
                fragment = new ShiftsFragment();
            case "StatusFragment":
                fragment = new StatusFragment();
        }

        return fragment;
    }

    private void setBottomNavigationSelectedItem(String tag) {
        if (bottomNavigationView == null) {
            return;
        }

        switch (tag) {
            case "SettingsFragment":
                bottomNavigationView.setSelectedItemId(R.id.settingsNavigationItem);
            case "JobsFragment":
                bottomNavigationView.setSelectedItemId(R.id.jobsNavigationItem);
            case "ShiftsFragment":
                bottomNavigationView.setSelectedItemId(R.id.shiftsNavigationItem);
            case "StatusFragment":
                bottomNavigationView.setSelectedItemId(R.id.statusNavigationItem);
            default:
                return;
        }
    }

    private void saveCurrentFragment(String fragmentTag) {
        SharedPreferences sharedPreferences = getSharedPreferences("FragmentPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentFragment", fragmentTag);
        editor.apply();
    }

    private String getCurrentFragment() {
        SharedPreferences sharedPreferences = getSharedPreferences("FragmentPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("currentFragment", "StatusFragment");
    }

    private boolean loadFragment(Fragment fragment, String tag) {
        if (fragment == null || getUserInstance() == null) {
            Log.d("Ofek", "Fragment is null: " + (fragment == null));
            Log.d("Ofek", "User is null: " + (getUserInstance() == null));
            return false;
        }

        getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit();

        saveCurrentFragment(tag);
        return true;
    }

    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 99) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Ofek", "Permission to send notifications granted.");
            }
            else {
                Log.e("Ofek", "User did not grant permission to send notifications.");
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentFragmentTag = getCurrentFragment();
        Log.d("Ofek", currentFragmentTag);
        Fragment fragment;
        switch (currentFragmentTag) {
            case "SettingsFragment":
                fragment = new SettingsFragment();
                bottomNavigationView.setSelectedItemId(R.id.settingsNavigationItem);
                break;
            case "JobsFragment":
                fragment = new JobsFragment();
                bottomNavigationView.setSelectedItemId(R.id.jobsNavigationItem);
                break;
            case "ShiftsFragment":
                fragment = new ShiftsFragment();
                bottomNavigationView.setSelectedItemId(R.id.shiftsNavigationItem);
                break;
            case "StatusFragment":
                fragment = new StatusFragment();
                bottomNavigationView.setSelectedItemId(R.id.statusNavigationItem);
                break;
            default:
                fragment = new StatusFragment();
                bottomNavigationView.setSelectedItemId(R.id.statusNavigationItem);
                break;
        }
        loadFragment(fragment, currentFragmentTag);
    }

    @Override
    public void onSyncComplete() {
        // Load the status fraggment after user data is synchronized
        if (getSupportFragmentManager().getFragments().isEmpty()) {
            String tag = getCurrentFragment();
            setBottomNavigationSelectedItem(tag);
            loadFragment(getFragmentByTag(tag), tag);
        }
    }

}