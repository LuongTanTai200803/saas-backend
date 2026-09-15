package com.saasai.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.saasai.dto.DocumentDTO;
import com.saasai.dto.PaginatedResponseDTO;
import com.saasai.dto.UpdateUserProfileRequest;
import com.saasai.dto.UserProfileDTO;
import com.saasai.entity.BillingInvoice;
import com.saasai.service.UserService;

import com.saasai.dto.UserCreditSummaryDTO;
import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.feature.payment.BillingInvoiceDTO;


@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication != null ? authentication.getDetails() : null;
        String email = details instanceof String ? (String) details : null;

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileDTO profile = userService.getUserProfileByEmail(email);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> updateProfile(
            @RequestBody UpdateUserProfileRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication != null ? authentication.getDetails() : null;
        String email = details instanceof String ? (String) details : null;

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileDTO updated = userService.updateUserProfileByEmail(email, request);
        return ResponseEntity.ok(ApiResponseDTO.success("Cập nhật thông tin thành công", updated));
    }

    @GetMapping("/documents")
    public ResponseEntity<PaginatedResponseDTO<DocumentDTO>> getRecentDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getDetails();
        PaginatedResponseDTO<DocumentDTO> documents = userService.getUserDocumentsByUserEmail(email, page, size);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/credit-summary")
    public ResponseEntity<ApiResponseDTO<UserCreditSummaryDTO>> getCreditSummary() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication != null ? authentication.getDetails() : null;
        String email = details instanceof String ? (String) details : null;

        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserCreditSummaryDTO summary = userService.getUserCreditSummaryByEmail(email);
        return ResponseEntity.ok(ApiResponseDTO.success("OK", summary));
    }

    // Lấy thông tin hóa đơn cụ thể của người dùng dựa trên invoiceId
    @GetMapping("/invoice")
    public ResponseEntity<ApiResponseDTO<BillingInvoiceDTO>> getMyInvoice(@RequestParam("invoiceId") String invoiceId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BillingInvoiceDTO dto = userService.getInvoiceForUser(userId, invoiceId);
        return ResponseEntity.ok(ApiResponseDTO.success("OK", dto));
    }
    
}
