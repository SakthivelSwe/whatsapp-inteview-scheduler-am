package com.dummby.interviewscheduler.repository;

import com.dummby.interviewscheduler.model.entity.MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageLogRepository extends JpaRepository<MessageLog, UUID> {
    List<MessageLog> findByBatchId(UUID batchId);
    long countByBatchIdAndStatus(UUID batchId, MessageLog.Status status);

    @Query("select count(m) from MessageLog m where m.status = com.dummby.interviewscheduler.model.entity.MessageLog.Status.SENT and m.sentAt >= :since")
    long countSentSince(@Param("since") LocalDateTime since);
}

