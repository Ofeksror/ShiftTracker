package com.example.shifttracker;

import static com.example.shifttracker.JobsFragment.jobsListAdapter;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import data_models.Job;
import data_models.Shift;
import data_models.User;

public class FirebaseManager {
    private static FirebaseManager instance;
    private String TAG = "Ofek : FirebaseManager";

    private static FirebaseFirestore db;
    private static FirebaseAuth auth;
    private static User user;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
//        authUser = FirebaseAuth.getInstance().getCurrentUser();
        user = null;
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public static synchronized FirebaseFirestore getFirestoreInstance() {
        return db;
    }
    public static synchronized FirebaseAuth getAuthInstance() {
        return auth;
    }
    public static synchronized User getUserInstance() { return user; }
    public static synchronized void setUserInstance(User newUser) { user = newUser; }

    public static void signOut() {
        auth.signOut();
        user = null;
    }

    public static float getShiftDuration(Date startTime, Date endTime) {
        float hours_duration = (float) (endTime.getTime() - startTime.getTime()) / (3600000);
        return (float) (Math.round(hours_duration * 2) / 2.0);
    }

    public static float calculateWage(Date startTime, Date endTime, float hourlyFee, float extraHoursAfter, float extraHoursRate) {
        float shiftDuration = getShiftDuration(startTime, endTime);

        if (shiftDuration <= extraHoursAfter) {
            return hourlyFee * shiftDuration;
        }
        else {
            float extraHours = shiftDuration - extraHoursAfter;
            return hourlyFee * shiftDuration + hourlyFee * extraHoursRate * extraHours;
        }
    }

    public static void syncUserWithDatabase() {
        // Parse user document User object
        getUserRef().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                setUserInstance(documentSnapshot.toObject(User.class));
            }
        });
    }

    public static String getUserId() {
        return auth.getUid();
    }

    public static DocumentReference getUserRef() {
        DocumentReference userRef = db.collection("users").document(getUserId());
        return userRef;
    }

    public static void updateRecyclerviewDataset() {
        jobsListAdapter.updateJobsList((ArrayList<Job>) user.getJobs());
    }

    public static boolean checkJobTitleAlreadyExists(String jobTitle) {
        if (getUserInstance() == null) {
            Log.d("Ofek", "User is null");
        }

        List<Job> jobs = user.getJobs();

        if (jobs == null || jobs.size() == 0) {
            return false;
        }

        for (Job job : jobs) {
            if (job.getTitle().equals(jobTitle)) {
                return true;
            }
        }
        return false;
    }

    public static void addJobToUser(Job newJob) {
        List<Job> jobs = user.getJobs();

        if (jobs == null) {
            jobs = new ArrayList<Job>();
        }

        jobs.add(newJob);
        user.setJobs(jobs);
    }

    public static String updateJobFields(Job updatedJob, String oldTitle) {

        ArrayList<Job> jobs = (ArrayList<Job>) user.getJobs();

        for (Job job : jobs) {
            if (oldTitle.equals(job.getTitle())) {
                job.setTitle(updatedJob.getTitle());
                job.setHourlyFee(updatedJob.getHourlyFee());
                job.setExtraHoursAfter(updatedJob.getExtraHoursAfter());
                job.setExtraHoursRate(updatedJob.getExtraHoursRate());
            }
        }

        // Update locally
        user.setJobs(jobs);

        // Update Database
        final String[] errorMessage = new String[] {""};
        getUserRef().update("jobs", jobs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Ofek", "Successfuly Updated Job");
                })
                .addOnFailureListener(e -> {
                    Log.d("Ofek", "Failed to Update Job");
                    errorMessage[0] = "Failed to Update Job, " + e;
                });

        return errorMessage[0];
    }


    public static Job findJobByTitle(String jobTitle) {
        for (Job job : user.getJobs()) {
            if (jobTitle.equals(job.getTitle())) {
                return job;
            }
        }
        return null;
    }


    public static String removeShiftAtIndexFromJob(int index, String jobTitle) {
        ArrayList<Job> jobs = (ArrayList<Job>) user.getJobs();

        for (Job job : jobs) {
            if (jobTitle.equals(job.getTitle())) {
                ArrayList<Shift> shifts = (ArrayList<Shift>) job.getShifts();
                shifts.remove(index);
                job.setShifts(shifts);
            }
        }

        // Update local user object
        user.setJobs(jobs);

        // Upload updated data to FireStore
        final String[] errorMessage = new String[] {""};
        getUserRef().update("jobs", jobs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Ofek", "Successfully Removed Shift");
                })
                .addOnFailureListener(e -> {
                    Log.d("Ofek", "Failed to Remove Shift");
                    errorMessage[0] = "Failed to Remove Shift, " + e;
                });

        return errorMessage[0];
    }

    public static String replaceShiftAtIndex(int index, Shift newShift, String jobTitle) {
        ArrayList<Job> jobs = (ArrayList<Job>) user.getJobs();

        for (Job job : jobs) {
            if (jobTitle.equals(job.getTitle())) {
                ArrayList<Shift> shifts = (ArrayList<Shift>) job.getShifts();
                shifts.set(index, newShift);
                job.setShifts(shifts);
            }
        }

        // Update local user object
        user.setJobs(jobs);

        // Upload updated data to FireStore
        final String[] errorMessage = new String[] {""};
        getUserRef().update("jobs", jobs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Ofek", "Successfully Updated Shift");
                })
                .addOnFailureListener(e -> {
                    Log.d("Ofek", "Failed to Update Shift for Jobs");
                    errorMessage[0] = "Failed to Update Shift for Jobs, " + e;
                });

        return errorMessage[0];
    }

    public static String addShiftToJob(Shift shift, String jobTitle) {

        ArrayList<Job> jobs = (ArrayList<Job>) user.getJobs();

        for (Job job : jobs) {
            if (jobTitle.equals(job.getTitle())) {
                ArrayList<Shift> shifts = (ArrayList<Shift>) job.getShifts();
                shifts.add(shift);
                job.setShifts(shifts);
            }
        }

        // Update local user object
        user.setJobs(jobs);

        // Upload updated data to FireStore
        final String[] errorMessage = new String[] {""};
        getUserRef().update("jobs", jobs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Ofek", "Successfully Updated Jobs and Shifts");
                })
                .addOnFailureListener(e -> {
                    Log.d("Ofek", "Failed to Update Shift for Jobs");
                    errorMessage[0] = "Failed to Update Shift for Jobs, " + e;
                });

        return errorMessage[0];
    }

    public static String updateDatabaseUserDocument() {
        final String[] failureMessage = new String[]{""};

        getUserRef().set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Ofek: FirebaseManager", "Successfully Updated User Document to Database");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.d("Ofek: FirebaseManager", "Error updating user jobs: " + e.getMessage());
                failureMessage[0] = "Error updating user jobs: " + e.getMessage();
            }
        });

        return failureMessage[0];
    }
}
