package com.ruetcse.attendanceapp;

public class ClassItem {
    private String department;
    private String seriesNo;
    private String courseCode;
    private int firstRoll;
    private int lastRoll;
    private String permanentAbsentRolls;
    private String extraRolls;

    public ClassItem(String department, String seriesNo, String courseCode, int firstRoll, int lastRoll,
                     String permanentAbsentRolls, String extraRolls) {
        this.department = department;
        this.seriesNo = seriesNo;
        this.courseCode = courseCode;
        this.firstRoll = firstRoll;
        this.lastRoll = lastRoll;
        this.permanentAbsentRolls = permanentAbsentRolls;
        this.extraRolls = extraRolls;
    }

    public String getDepartment() { return department; }
    public String getSeriesNo() { return seriesNo; }
    public String getCourseCode() { return courseCode; }
    public int getFirstRoll() { return firstRoll; }
    public int getLastRoll() { return lastRoll; }
    public String getPermanentAbsentRolls() { return permanentAbsentRolls; }
    public String getExtraRolls() { return extraRolls; }
}
