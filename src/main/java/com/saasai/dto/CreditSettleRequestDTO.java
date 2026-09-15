package com.saasai.dto;

public class CreditSettleRequestDTO {

    private String model;
    private Long modelPackageId;
    private Double creditRate;
    private Double outputWeight;
    private Double estimatedHold; // totalCreditHold
    private Double inputCredit;
    private Double outputCredit;
    private String description;
    private String sessionUuid; // optional, useful for logging

    // Getters and Setters

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getModelPackageId() {
        return modelPackageId;
    }

    public void setModelPackageId(Long modelPackageId) {
        this.modelPackageId = modelPackageId;
    }

    public Double getCreditRate() {
        return creditRate;
    }

    public void setCreditRate(Double creditRate) {
        this.creditRate = creditRate;
    }

    public Double getOutputWeight() {
        return outputWeight;
    }

    public void setOutputWeight(Double outputWeight) {
        this.outputWeight = outputWeight;
    }

    public Double getEstimatedHold() {
        return estimatedHold;
    }

    public void setEstimatedHold(Double estimatedHold) {
        this.estimatedHold = estimatedHold;
    }

    public Double getInputCredit() {
        return inputCredit;
    }

    public void setInputCredit(Double inputCredit) {
        this.inputCredit = inputCredit;
    }

    public Double getOutputCredit() {
        return outputCredit;
    }

    public void setOutputCredit(Double outputCredit) {
        this.outputCredit = outputCredit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
}
