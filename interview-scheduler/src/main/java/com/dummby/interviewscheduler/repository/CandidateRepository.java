package com.dummby.interviewscheduler.repository;

import com.dummby.interviewscheduler.model.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    List<Candidate> findByBatchId(UUID batchId);
    List<Candidate> findByBatchIdAndStatus(UUID batchId, Candidate.SendStatus status);
    long countByBatchIdAndStatus(UUID batchId, Candidate.SendStatus status);
}
