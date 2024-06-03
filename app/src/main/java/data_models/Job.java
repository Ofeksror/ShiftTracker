package data_models;

import java.util.List;

import java.util.List;

public class Job {
    private String title;
    private float hourlyFee;
    private float extraHoursAfter;
    private float extraHoursRate;
    private List<Shift> shifts;

    //    private float minimumShiftWage;
    //    private float hourlyPayAfter;

    public Job() {
    }

    public Job(String title, float hourlyFee, float extraHoursAfter, float extraHoursRate, List<Shift> shifts) {
        this.title = title;
        this.hourlyFee = hourlyFee;
        this.extraHoursAfter = extraHoursAfter;
        this.extraHoursRate = extraHoursRate;
        // this.minimumShiftWage = minimumShiftWage;
        // this.hourlyPayAfter = hourlyPayAfter;
        this.shifts = shifts;
    }

    // Getters
    public String getTitle() {
        return title;
    }
    public float getHourlyFee() { return hourlyFee; }
    public float getExtraHoursAfter() {
        return extraHoursAfter;
    }
    public float getExtraHoursRate() {
        return extraHoursRate;
    }
    public List<Shift> getShifts() {
        return shifts;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }
    public void setHourlyFee(float hourlyFee) { this.hourlyFee = hourlyFee; }
    public void setExtraHoursAfter(float extraHoursAfter) { this.extraHoursAfter = extraHoursAfter; }
    public void setExtraHoursRate(float extraHoursRate) {
        this.extraHoursRate = extraHoursRate;
    }
    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
    }
}

