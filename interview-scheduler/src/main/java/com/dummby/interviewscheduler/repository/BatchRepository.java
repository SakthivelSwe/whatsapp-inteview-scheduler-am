package com.dummby.interviewscheduler.repository;

import com.dummby.interviewscheduler.model.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    java.util.List<Batch> findAllByOrderByCreatedAtDesc();
}
