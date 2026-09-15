package com.saasai.admin;

import com.saasai.feature.ai.ApiResponseDTO;
import com.saasai.admin.AdminDashboardDTO;
import com.saasai.admin.AdminRevenueDTO;
import com.saasai.admin.AdminAiUsageDTO;
import com.saasai.admin.DashboardRange;
import com.saasai.admin.AdminTopAssistantDTO;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@CrossOrigin
public class AdminDashboardController {
    @Autowired
    private AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<AdminDashboardDTO>> getOverview() {
        AdminDashboardDTO dto = adminService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponseDTO.success("OK", dto));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponseDTO<AdminRevenueDTO>> getRevenue(
            @RequestParam String range
    ) {
        AdminRevenueDTO data =
                adminService.getRevenue(DashboardRange.parse(range));

        return ResponseEntity.ok(
                ApiResponseDTO.success("OK", data)
        );
    }

    @GetMapping("/ai-usage")
    public ResponseEntity<ApiResponseDTO<AdminAiUsageDTO>> getAiUsage(
            @RequestParam String range
    ) {
        AdminAiUsageDTO data =
                adminService.getAiUsage(DashboardRange.parse(range));

        return ResponseEntity.ok(
                ApiResponseDTO.success("OK", data)
        );
    }

    @GetMapping("/top-assistants")
    public ResponseEntity<ApiResponseDTO<List<AdminTopAssistantDTO>>>
    getTopAssistants(
            @RequestParam String range
    ) {
        List<AdminTopAssistantDTO> data =
                adminService.getTopAssistants(
                        DashboardRange.parse(range)
                );

        return ResponseEntity.ok(
                ApiResponseDTO.success("OK", data)
        );
    }
}