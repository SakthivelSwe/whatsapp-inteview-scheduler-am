package com.dummby.interviewscheduler.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fileName;

    private Integer totalCandidates;

    /** JSON-serialized list of detected ColumnSchema (so we can show placeholders later). */
    @Lob
    @Column(name = "schema_json", columnDefinition = "CLOB")
    private String schemaJson;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = BatchStatus.UPLOADED;
    }

    public enum BatchStatus { UPLOADED, SENDING, PAUSED, COMPLETED, FAILED }
}

