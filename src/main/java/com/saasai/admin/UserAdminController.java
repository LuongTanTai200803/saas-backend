package com.saasai.admin;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.admin.UserAdminDTO;
import com.saasai.admin.UserUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@CrossOrigin
public class UserAdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UserAdminDTO>>> listUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var dtos = adminService.listUsers(page, size);
        return ResponseEntity.ok(ApiResponseDTO.success("OK", dtos));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<UserAdminDTO>> getUser(@PathVariable String userId) {
        var dto = adminService.getUser(userId);
        return ResponseEntity.ok(ApiResponseDTO.success("OK", dto));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<UserAdminDTO>> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest req) {
        var dto = adminService.updateUser(userId, req);
        return ResponseEntity.ok(ApiResponseDTO.success("Updated", dto));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponseDTO<Object>> deleteUser(@PathVariable String userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponseDTO.success("Deleted", null));
    }

    @GetMapping("/{userId}/payments")
    public ResponseEntity<ApiResponseDTO<List<UserPaymentHistoryDTO>>>
    listUserPayments(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "OK",
                        adminService.listUserPayments(userId)
                )
        );
    }

    @GetMapping("/{userId}/ai-usage")
    public ResponseEntity<ApiResponseDTO<List<UserAiUsageDTO>>>
    listUserAiUsage(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "OK",
                        adminService.listUserAiUsage(userId)
                )
        );
    }
}