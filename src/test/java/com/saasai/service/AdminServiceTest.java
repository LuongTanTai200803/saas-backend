package com.saasai.service;

import com.saasai.admin.AdminService;
import com.saasai.entity.AdminPackageConfig;
import com.saasai.repository.AdminPackageConfigRepository;
import com.saasai.repository.SystemStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminPackageConfigRepository adminPackageConfigRepository;

    @Mock
    private SystemStatsRepository systemStatsRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getPackageConfig_shouldReturnExistingPackageConfig() {
        AdminPackageConfig config = new AdminPackageConfig();
        config.setPackageType("FREE");
        config.setPackageCategory(
                AdminPackageConfig.PackageCategory.SUBSCRIPTION
        );
        config.setPrice(0L);
        config.setCreditLimit(0.0);
        config.setDuration(30);
        config.setStorageQuotaMb(100L);

        when(adminPackageConfigRepository.findByPackageType("FREE"))
                .thenReturn(Optional.of(config));

        AdminPackageConfig result =
                adminService.getPackageConfig("FREE");

        assertThat(result).isSameAs(config);
        assertThat(result.getPackageType()).isEqualTo("FREE");
        assertThat(result.getStorageQuotaMb()).isEqualTo(100L);

        verify(adminPackageConfigRepository)
                .findByPackageType("FREE");
    }

    @Test
    void getPackageConfig_shouldReturnNullWhenPackageDoesNotExist() {
        when(adminPackageConfigRepository.findByPackageType("UNKNOWN"))
                .thenReturn(Optional.empty());

        AdminPackageConfig result =
                adminService.getPackageConfig("UNKNOWN");

        assertThat(result).isNull();

        verify(adminPackageConfigRepository)
                .findByPackageType("UNKNOWN");
    }
}