package com.saasai.admin;

import java.time.LocalDateTime;

public class UserUpdateRequest {
    private String packageType;
    private LocalDateTime expireDate;
    private Double credits;
    private String status;

    // getters/setters
    public String getPackageType(){return packageType;}
    public void setPackageType(String p){this.packageType=p;}
    public LocalDateTime getExpireDate(){return expireDate;}
    public void setExpireDate(LocalDateTime d){this.expireDate=d;}
    public Double getCredits(){return credits;}
    public void setCredits(Double c){this.credits=c;}
    public String getStatus(){return status;}
    public void setStatus(String s){this.status=s;}
}