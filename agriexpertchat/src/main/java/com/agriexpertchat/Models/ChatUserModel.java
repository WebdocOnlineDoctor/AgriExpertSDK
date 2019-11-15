package com.agriexpertchat.Models;

/**
 * Created by WaleedPCC on 9/26/2019.
 */

public class ChatUserModel {

    private String Name;
    private String Email;
    private String Status;
    private String FirebaseEmail;
    private String appName;


    public String getName() {
        return Name;
    }

    public void setName(String doctorName) {
        Name = doctorName;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String doctorEmail) {
        Email = doctorEmail;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String doctorStatus) {
        Status = doctorStatus;
    }

    public String getFirebaseEmail() {
        return FirebaseEmail;
    }

    public void setFirebaseEmail(String doctorFirebaseEmail) {
        FirebaseEmail = doctorFirebaseEmail;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
