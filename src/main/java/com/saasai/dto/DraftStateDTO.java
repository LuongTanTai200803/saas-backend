package com.saasai.dto;

import java.time.LocalDateTime;

public class DraftStateDTO {

    private String sessionUuid;

    private String userId;

    /**
     * Dữ liệu AI JSON trả được lưu tại đây.
     */
    private String editorText;

    private String status;
    private String wizardStateJson;
    private LocalDateTime updatedAt;
    private String fieldCode;
    private String sessionName;

    public DraftStateDTO() {}

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String draftId) {
        this.sessionUuid = draftId;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEditorText() {
        return editorText;
    }

    public void setEditorText(String editorText) {
        this.editorText = editorText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWizardStateJson() {
        return wizardStateJson;
    }

    public void setWizardStateJson(String wizardStateJson) {
        this.wizardStateJson = wizardStateJson;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
}