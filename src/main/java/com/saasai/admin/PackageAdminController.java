package com.saasai.admin;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.admin.AdminPackageDTO;
import com.saasai.admin.AdminPackageUpdateDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/packages")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin
public class PackageAdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<AdminPackageDTO>>> listPackages() {
        return ResponseEntity.ok(ApiResponseDTO.success("OK", adminService.listPackages()));
    }

    @GetMapping("/{packageType}")
    public ResponseEntity<ApiResponseDTO<AdminPackageDTO>> getPackage(@PathVariable String packageType) {
        return ResponseEntity.ok(ApiResponseDTO.success("OK", adminService.getPackage(packageType)));
    }

    @PutMapping("/{packageType}")
    public ResponseEntity<ApiResponseDTO<AdminPackageDTO>> updatePackage(@PathVariable String packageType, @RequestBody AdminPackageUpdateDTO req) {
        return ResponseEntity.ok(ApiResponseDTO.success("Updated", adminService.upsertPackageConfig(packageType, req)));
    }
}