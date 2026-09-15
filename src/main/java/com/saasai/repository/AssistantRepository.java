package com.saasai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.saasai.entity.Assistant;

@Repository
public interface AssistantRepository extends JpaRepository<Assistant, Integer> {
}
    