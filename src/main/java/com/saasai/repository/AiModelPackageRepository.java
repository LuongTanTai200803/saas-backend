package com.saasai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.saasai.entity.AiModelPackage;
import java.util.List;

public interface AiModelPackageRepository extends JpaRepository<AiModelPackage, Long> {
    List<AiModelPackage> findByIdLessThanEqualOrderByIdAsc(Long level);
}