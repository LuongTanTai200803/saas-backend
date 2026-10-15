package com.saasai.admin;

public class AdminPackageUpdateDTO {
    private Long price;
    private Double creditLimit;
    private Integer duration;
    private String description;
    private Long storageQuotaMb;
    private String badge;
    private Boolean isActive;
    private Integer maxFilesPerCall;
    private Long maxFileSizeMb;
    private Long maxTotalFileSizeMbPerCall;
    private Long maxTextPerCall;
    private String allowedFileTypes;

    // getters/setters
    public Long getPrice(){return price;}
    public void setPrice(Long p){this.price=p;}
    public Double getCreditLimit(){return creditLimit;}
    public void setCreditLimit(Double c){this.creditLimit=c;}
    public Integer getDuration(){return duration;}
    public void setDuration(Integer d){this.duration=d;}
    public String getDescription(){return description;}
    public void setDescription(String s){this.description=s;}
    public Long getStorageQuotaMb(){return storageQuotaMb;}
    public void setStorageQuotaMb(Long s){this.storageQuotaMb=s;}
    public String getBadge(){return badge;}
    public void setBadge(String b){this.badge=b;}
    public Boolean getIsActive(){return isActive;}
    public void setIsActive(Boolean a){this.isActive=a;}

    public Integer getMaxFilesPerCall(){return maxFilesPerCall;}
    public void setMaxFilesPerCall(Integer m){this.maxFilesPerCall=m;}
    public Long getMaxFileSizeMb(){return maxFileSizeMb;}
    public void setMaxFileSizeMb(Long m){this.maxFileSizeMb=m;}
    public Long getMaxTotalFileSizeMbPerCall(){return maxTotalFileSizeMbPerCall;}
    public void setMaxTotalFileSizeMbPerCall(Long m){this.maxTotalFileSizeMbPerCall=m;}
    public Long getMaxTextPerCall(){return maxTextPerCall;}
    public void setMaxTextPerCall(Long m){this.maxTextPerCall=m;}
    public String getAllowedFileTypes(){return allowedFileTypes;}
    public void setAllowedFileTypes(String a){this.allowedFileTypes=a;}
}