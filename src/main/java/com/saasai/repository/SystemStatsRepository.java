package com.saasai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saasai.entity.SystemStats;

@Repository
public interface SystemStatsRepository extends JpaRepository<SystemStats, Long> {
}
