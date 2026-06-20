package com.dummby.interviewscheduler.model.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Post-execution summary report (RD §5). */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BatchStatusResponse {
    private UUID batchId;
    private String status;          // UPLOADED | SENDING | COMPLETED
    private long totalRecords;      // RD: Total Records Processed
    private long successfullySent;  // RD: Successfully Sent
    private long failed;            // RD: Failed Records (count)
    private long invalid;           // rejected before send (bad data)
    private long pending;
    /** RD §5: list of phone numbers that failed (for the "Retry Failed Only" action). */
    @Builder.Default
    private List<FailedRecord> failedRecords = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FailedRecord {
        private UUID candidateId;
        private Integer rowNumber;
        private String candidateName;
        private String phoneNumber;
        private String error;
    }
}
