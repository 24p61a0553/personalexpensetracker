package com.expensetracker.model;

import java.sql.Timestamp;

/**
 * Model class representing the application user's profile and person name.
 */
public class UserProfile {

    private int id = 1;
    private String personName = "Srinivas";
    private Timestamp updatedAt;

    public UserProfile() {
        this.personName = "Srinivas";
    }

    public UserProfile(int id, String personName) {
        this.id = id;
        this.personName = personName;
    }

    public UserProfile(int id, String personName, Timestamp updatedAt) {
        this.id = id;
        this.personName = personName;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
