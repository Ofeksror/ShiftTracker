package data_models;

import java.util.Date;

public class Shift {
    private Date startTime;
    private Date endTime;
    private String notes;
    private float hourlyFee;
    private float bonus;
    private float wage;

    public Shift() {
    }

    public Shift(Date startTime, Date endTime, float hourlyFee, float bonus, String notes, float wage) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.hourlyFee = hourlyFee;
        this.bonus = bonus;
        this.notes = notes;
        this.wage = wage;
    }

    // Getters
    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getNotes() {
        return notes;
    }

    public float getHourlyFee() {
        return hourlyFee;
    }

    public float getBonus() {
        return bonus;
    }

    public float getWage() { return wage; }

    // Setters
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setHourlyFee(float hourlyFee) {
        this.hourlyFee = hourlyFee;
    }

    public void setBonus(float bonus) {
        this.bonus = bonus;
    }

    public void setWage(float wage) { this.wage = wage;}
}
