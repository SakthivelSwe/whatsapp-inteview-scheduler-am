package com.dummby.interviewscheduler.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_logs", indexes = {
        @Index(name = "idx_log_batch", columnList = "batch_id"),
        @Index(name = "idx_log_candidate", columnList = "candidate_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "candidate_id")
    private UUID candidateId;

    private String whatsappNumber;

    @Column(length = 4000)
    private String renderedMessage;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String providerMessageId;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime sentAt;

    @PrePersist
    void prePersist() { sentAt = LocalDateTime.now(); }

    public enum Status { SENT, FAILED }
}

