package com.saasai.dto;

public class DraftFinalizeRequestDTO {
    private String draftId;
    private String instruction;

    public DraftFinalizeRequestDTO() {}

    public DraftFinalizeRequestDTO(String draftId, String instruction) {
        this.draftId = draftId;
        this.instruction = instruction;
    }

    public String getDraftId() { return draftId; }
    public void setDraftId(String draftId) { this.draftId = draftId; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
}