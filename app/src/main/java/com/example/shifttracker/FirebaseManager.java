package com.example.shifttracker;

import static com.example.shifttracker.JobsFragment.jobsListAdapter;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import data_models.Job;
import data_models.Shift;
import data_models.User;

public class FirebaseManager {
    private static FirebaseManager instance;
    private String TAG = "Ofek : FirebaseManager";

    private static FirebaseFirestore db;
    private static FirebaseStorage storage;
    private static FirebaseAuth auth;
    private static User user;

    private FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
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
    public static synchronized FirebaseStorage getStorageInstance() { return storage; }
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

    public static float calculateWage(Date startTime, Date endTime, float hourlyFee, float extraHoursAfter, float extraHoursRate, float bonus) {
        float shiftDuration = getShiftDuration(startTime, endTime);

        if (shiftDuration <= extraHoursAfter) {
            return (hourlyFee * shiftDuration) + bonus ;
        }
        else {
            float extraHours = shiftDuration - extraHoursAfter;
            return hourlyFee * shiftDuration + hourlyFee * extraHoursRate * extraHours + bonus;
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


    public static Shift createNewShift(Date startTime, Date endTime, float hourlyFee, float bonus, String notes, String jobTitle) {
        // get the job object
        Job job = findJobByTitle(jobTitle);

        // if hourlyFee is not set, set it to the job's hourlyFee
        if (hourlyFee == 0) {
            hourlyFee = job.getHourlyFee();
        }

        // create a new shift object
        return new Shift(startTime, endTime, hourlyFee, bonus, notes, calculateWage(startTime, endTime, hourlyFee, job.getExtraHoursAfter(), job.getExtraHoursRate(), bonus));
    }

    public static Job findJobByTitle(String jobTitle) {
        for (Job job : user.getJobs()) {
            if (jobTitle.equals(job.getTitle())) {
                return job;
            }
        }
        return null;
    }

    public static String deleteJob(String jobTitle) {
        ArrayList<Job> jobs = (ArrayList<Job>) user.getJobs();

        int i = 0;
        for (Job job : jobs) {
            if (jobTitle.equals(job.getTitle())) {
                jobs.remove(i);
                break;
            }
            i++;
        }

        user.setJobs(jobs);

        // Upload updated data to FireStore
        final String[] errorMessage = new String[] {""};
        getUserRef().update("jobs", jobs)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Ofek", "Successfully Removed Job " + jobTitle);
                })
                .addOnFailureListener(e -> {
                    Log.d("Ofek", "Failed to Remove Job");
                    errorMessage[0] = "Failed to Remove Job, " + e;
                });

        return errorMessage[0];
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

    public static Map<String, Float> calculateIncomeForLastSixMonths() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.getDefault());

        // Go back 6 months ago
        calendar.add(Calendar.MONTH, -5);

        // Initialize map values
        Map<String, Float> incomeData = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            String month = sdf.format(calendar.getTime());
            incomeData.put(month, (float) 0);

            // Go forward one month
            calendar.add(Calendar.MONTH, 1);
        }

        Log.d("Ofek", incomeData.keySet().toString());

        ArrayList<Shift> shifts = getAllShifts();

        for (Shift shift : shifts) {
            String shiftMonth = sdf.format(shift.getStartTime());
            if (incomeData.containsKey(shiftMonth)) {
                incomeData.put(shiftMonth, incomeData.get(shiftMonth) + shift.getWage());
            }
        }

        return incomeData;
    }

    public static void updateGoals(int targetMonthlyIncome, int targetWeeklyHours) {
        user.setTargetMonthlyIncome(targetMonthlyIncome);
        user.setTargetWeeklyHours(targetWeeklyHours);

        updateDatabaseUserDocument();
    }

    public static ArrayList<Shift> getAllShifts() {
        ArrayList<Shift> shifts = new ArrayList<Shift>();
        for (Job job : user.getJobs()) {
            shifts.addAll(job.getShifts());
        }

        return shifts;
    }

    public static double getThisMonthIncome() {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        float totalIncome = 0;

        ArrayList<Shift> shifts = getAllShifts();
        for (Shift shift : shifts) {
            calendar.setTime(shift.getStartTime());

            if (calendar.get(Calendar.MONTH) == currentMonth
            && calendar.get(Calendar.YEAR) == currentYear) {
                totalIncome += shift.getWage();
            }
        }

        return totalIncome;
    }

    public static double getThisWeekWorkingHours() {
        Calendar calendar = Calendar.getInstance();
        int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
        int currentYear = calendar.get(Calendar.YEAR);

        float totalHours = 0;

        ArrayList<Shift> shifts = getAllShifts();
        for (Shift shift : shifts) {
            calendar.setTime(shift.getStartTime());

            if (calendar.get(Calendar.WEEK_OF_YEAR) == currentWeek
                    && calendar.get(Calendar.YEAR) == currentYear) {
                totalHours += getShiftDuration(shift.getStartTime(), shift.getEndTime());
            }
        }

        return totalHours;
    }



}
