package com.ajinkya.cleantogether.Models;

public class userModel {
    private String userName;
    private String userEmail;
    private String userNumber;
    private String userId;

    public userModel() {
        // Required empty constructor for Firebase
    }

    public userModel(String userName, String userEmail, String userNumber, String userId) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userNumber = userNumber;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}