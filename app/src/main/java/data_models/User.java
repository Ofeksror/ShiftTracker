package data_models;

import java.util.ArrayList;
import java.util.List;

import java.util.List;

public class User {
    private String userId;
    private List<Job> jobs = new ArrayList<Job>();

    public User() {
        this.userId = null;
        jobs = new ArrayList<Job>();
    }

    public User(String userId) {
        this.userId = userId;
        jobs = new ArrayList<Job>();
    }

    public User(String userId, List<Job> jobs) {
        this.userId = userId;
        this.jobs = jobs;
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
}
