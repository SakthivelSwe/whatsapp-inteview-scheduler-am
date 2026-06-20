package com.dummby.interviewscheduler.model.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Response returned right after an Excel upload, summarizing parse + validation. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UploadResponse {
    private UUID batchId;
    private String fileName;
    private int totalRows;
    private int validCandidates;
    private int invalidCandidates;
    /** Validation errors per invalid row so HR can fix the sheet before sending. */
    @Builder.Default
    private List<RowError> errors = new ArrayList<>();
    /** Auto-detected schema — every column with its placeholder + sample value. */
    @Builder.Default
    private List<ColumnSchema> schema = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RowError {
        private Integer rowNumber;
        private String candidateName;
        private String phoneNumber;
        private String error;
    }
}
