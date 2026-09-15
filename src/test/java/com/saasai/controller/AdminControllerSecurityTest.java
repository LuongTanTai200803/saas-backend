package com.saasai.controller;

import com.saasai.admin.AdminService;
import com.saasai.admin.AdminPackageUpdateDTO;
import com.saasai.security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AdminService adminService;

    @Test
    void adminRoleShouldAccessPackageUpdate() throws Exception {
        String token = jwtTokenProvider.generateToken("admin-uuid-10293", "admin@example.com", "ROLE_ADMIN");
        when(adminService.upsertPackageConfig(anyString(), any(AdminPackageUpdateDTO.class)))
                .thenReturn(null);

        mockMvc.perform(put("/api/v1/admin/packages/PROFESSIONAL")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":199000,\"creditLimit\":100.0,\"allowedModels\":[\"deepseek-v4-flash\",\"gpt-5.4-mini\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleShouldBeForbiddenForPackageUpdate() throws Exception {
        String token = jwtTokenProvider.generateToken("user-uuid-2", "user@example.com", "ROLE_USER");

        mockMvc.perform(put("/api/v1/admin/packages/PROFESSIONAL")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":199000,\"creditLimit\":100.0,\"allowedModels\":[\"deepseek-v4-flash\",\"gpt-5.4-mini\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedShouldReceiveUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats/finance"))
                .andExpect(status().isUnauthorized());
    }
}
