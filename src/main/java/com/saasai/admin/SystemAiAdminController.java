package com.saasai.admin;

import com.saasai.feature.ai.ApiResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/ai")
@CrossOrigin
public class SystemAiAdminController {

    @Autowired
    private AiModelPackageAdminService aiModelPackageAdminService;

    
    @GetMapping("/model-packages")
    public ResponseEntity<ApiResponseDTO<List<AiModelPackageDTO>>>
    listModelPackages() {
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "OK",
                        aiModelPackageAdminService.listModelPackages()
                )
        );
    }

    @PutMapping("/model-packages/{id}/credit-rate")
    public ResponseEntity<ApiResponseDTO<AiModelPackageDTO>>
    updateCreditRate(
            @PathVariable Long id,
            @RequestBody CreditRateUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Updated",
                        aiModelPackageAdminService.updateCreditRate(
                                id,
                                request
                        )
                )
        );
    }
    
    // Update an existing AI model package
    @PutMapping("/model-packages/{id}")
        public ResponseEntity<ApiResponseDTO<AiModelPackageDTO>>
        updateModelPackage(
                @PathVariable Long id,
                @RequestBody AiModelPackageUpdateRequest request
        ) {
        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Updated",
                        aiModelPackageAdminService.update(id, request)
                )
        );
        }

}