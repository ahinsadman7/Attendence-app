package com.ruetcse.attendanceapp;

public class StudentRoll {
    private final String rollNumber;
    private boolean isPresent;

    public StudentRoll(String rollNumber) {
        this.rollNumber = rollNumber;
        this.isPresent = true;
    }

    public String getRollNumber() { return rollNumber; }
    public boolean isPresent() { return isPresent; }
    public void setPresent(boolean present) { isPresent = present; }
}
