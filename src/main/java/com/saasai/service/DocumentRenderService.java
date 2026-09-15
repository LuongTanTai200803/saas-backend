package com.saasai.service;

import java.util.List;

import com.saasai.dto.DocumentBlock;
import com.saasai.dto.DocumentFormDTO;

public interface DocumentRenderService {
    /**
     * Render administrative plain text from DocumentFormDTO.
     * Must NOT include uploaded file normalized text.
     */
    String render(DocumentFormDTO formData);

    List<DocumentBlock> parse(String documentText);
    
}