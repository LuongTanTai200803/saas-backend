package com.saasai.admin;

import java.util.List;

public interface AiModelPackageAdminService {

    List<AiModelPackageDTO> listModelPackages();

    AiModelPackageDTO updateCreditRate(
            Long id,
            CreditRateUpdateRequest request
    );

    AiModelPackageDTO update(
            Long id,
            AiModelPackageUpdateRequest request
    );
}