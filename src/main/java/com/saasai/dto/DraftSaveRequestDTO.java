package com.saasai.dto;

public class DraftSaveRequestDTO {

    private String sessionUuid;
    private Integer sessionId;

    /**
     * Nội dung draft.
     *
     * Backend sẽ lưu FormData JSON vào editorText.
     */
    private String editorText;

    /**
     * FormData từ frontend.
     * Backend serialize thành JSON và lưu vào editorText.
     */
    private DocumentFormDTO formData;

    private String status;
    private String wizardStateJson;
    private String fieldCode;

    public DraftSaveRequestDTO() {}

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getEditorText() {
        return editorText;
    }

    public void setEditorText(String editorText) {
        this.editorText = editorText;
    }

    public DocumentFormDTO getFormData() {
        return formData;
    }

    public void setFormData(DocumentFormDTO formData) {
        this.formData = formData;
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

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        this.fieldCode = fieldCode;
    }

}