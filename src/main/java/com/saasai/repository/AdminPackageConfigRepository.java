package com.saasai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saasai.entity.AdminPackageConfig;

import java.util.Optional;

@Repository
public interface AdminPackageConfigRepository extends JpaRepository<AdminPackageConfig, Long> {


    Optional<AdminPackageConfig> findByPackageType(String packageType);
}
