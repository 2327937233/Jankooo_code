package com.example.myapplication.network;

import com.google.gson.annotations.SerializedName;

public class PlanTravelRequest {
    private String location;
    
    @SerializedName("start_date")
    private String startDate;
    
    @SerializedName("end_date")
    private String endDate;
    
    private int days;
    private String preferences;

    public PlanTravelRequest(String location, String startDate, String endDate, int days, String preferences) {
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
        this.preferences = preferences;
    }

    // Getters and Setters
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
}
