package data_models;

import java.util.ArrayList;
import java.util.List;

import java.util.List;

public class User {
    private String userId;
    private List<Job> jobs = new ArrayList<Job>();
    private int targetMonthlyIncome;
    private int targetWeeklyHours;

    public User() {
        this.userId = null;
        jobs = new ArrayList<Job>();
        targetMonthlyIncome = 0;
        targetWeeklyHours = 0;
    }

    public User(String userId) {
        this.userId = userId;
        jobs = new ArrayList<Job>();
        targetMonthlyIncome = 0;
        targetWeeklyHours = 0;
    }

    public User(String userId, List<Job> jobs) {
        this.userId = userId;
        this.jobs = jobs;
        this.targetMonthlyIncome = 0;
        this.targetWeeklyHours = 0;
    }


    public User(String userId, List<Job> jobs, int targetMonthlyIncome, int targetWeeklyHours) {
        this.userId = userId;
        this.jobs = jobs;
        this.targetMonthlyIncome = targetMonthlyIncome;
        this.targetWeeklyHours = targetWeeklyHours;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public int getTargetMonthlyIncome() {
        return targetMonthlyIncome;
    }

    public void setTargetMonthlyIncome(int targetMonthlyIncome) {
        this.targetMonthlyIncome = targetMonthlyIncome;
    }

    public int getTargetWeeklyHours() {
        return targetWeeklyHours;
    }

    public void setTargetWeeklyHours(int targetWeeklyHours) {
        this.targetWeeklyHours = targetWeeklyHours;
    }
}
