package com.dummby.interviewscheduler.model.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Candidate row parsed from the uploaded Excel file.
 *
 * <p>Required system field: <b>phoneNumber</b>. All other Excel columns are stored
 * generically in {@code extraFieldsJson} (a JSON map) so the system supports
 * <b>dynamic, future-added headers</b> like Panel Name, Job Code, Recruiter, etc.
 *
 * <p>For backwards compatibility with the original RD spec, well-known headers
 * (Candidate Name, Job Position, Interview Date, Interview Time, Meeting Link)
 * are also persisted as first-class columns. New unknown headers live only in
 * the JSON map.
 */
@Entity
@Table(name = "candidates", indexes = {
        @Index(name = "idx_candidate_batch", columnList = "batch_id"),
        @Index(name = "idx_candidate_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    /** 1-based row number in the source Excel (for error reporting). */
    private Integer rowNumber;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;          // System field (E.164 e.g. +919342627033)

    @Column(name = "candidate_name")
    private String candidateName;        // {{column_1}}

    @Column(name = "job_position")
    private String jobPosition;          // {{column_2}}

    @Column(name = "interview_date")
    private String interviewDate;        // {{column_3}}

    @Column(name = "interview_time")
    private String interviewTime;        // {{column_4}}

    @Column(name = "meeting_link", length = 1000)
    private String meetingLink;          // {{column_5}}

    /**
     * Dynamic / future-proof field bag. Stored as JSON.
     * Keys are slugified header names (e.g. "panel_name", "job_code", "round_1_time")
     * + positional keys ("column_1", "column_2", ...) covering EVERY column the user
     * had in their Excel, in original order.
     */
    @Lob
    @Column(name = "extra_fields_json", columnDefinition = "CLOB")
    private String extraFieldsJson;

    @Enumerated(EnumType.STRING)
    private SendStatus status;

    /** Validation error captured at upload time (e.g. "Missing phone number"). */
    @Column(length = 500)
    private String validationError;

    /** Last error returned by WhatsApp provider when send failed. */
    @Column(length = 500)
    private String lastError;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = SendStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    /** Lifecycle of a single candidate's outbound message. */
    public enum SendStatus { PENDING, INVALID, SENT, FAILED, DELIVERED }

    /* ---------- helpers for the dynamic field map ---------- */

    private static final ObjectMapper EXTRA_MAPPER = new ObjectMapper();

    @Transient
    public Map<String, String> getExtraFields() {
        if (extraFieldsJson == null || extraFieldsJson.isBlank()) return new LinkedHashMap<>();
        try {
            return EXTRA_MAPPER.readValue(extraFieldsJson, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public void setExtraFields(Map<String, String> map) {
        if (map == null || map.isEmpty()) { this.extraFieldsJson = null; return; }
        try {
            this.extraFieldsJson = EXTRA_MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            this.extraFieldsJson = null;
        }
    }
}
