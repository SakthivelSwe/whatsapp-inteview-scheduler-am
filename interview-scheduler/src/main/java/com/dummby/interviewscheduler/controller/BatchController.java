package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.exception.ResourceNotFoundException;
import com.dummby.interviewscheduler.model.entity.Batch;
import com.dummby.interviewscheduler.repository.BatchRepository;
import com.dummby.interviewscheduler.repository.CandidateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only listing of batches stored in the database. Replaces the previous
 * localStorage-only approach so the UI can always recover its history,
 * regardless of which browser/device it is opened from.
 */
@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
@Tag(name = "Batches", description = "List and inspect uploaded batches")
public class BatchController {

    private final BatchRepository batchRepository;
    private final CandidateRepository candidateRepository;

    @Operation(summary = "List all uploaded batches (newest first)")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<BatchSummary>> list() {
        List<BatchSummary> out = batchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(b -> BatchSummary.builder()
                        .id(b.getId())
                        .fileName(b.getFileName())
                        .totalCandidates(b.getTotalCandidates() == null ? 0 : b.getTotalCandidates())
                        .status(b.getStatus() == null ? "UPLOADED" : b.getStatus().name())
                        .createdAt(b.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Get a single batch by id")
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<BatchSummary> get(@PathVariable UUID id) {
        Batch b = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
        return ResponseEntity.ok(BatchSummary.builder()
                .id(b.getId())
                .fileName(b.getFileName())
                .totalCandidates(b.getTotalCandidates() == null ? 0 : b.getTotalCandidates())
                .status(b.getStatus() == null ? "UPLOADED" : b.getStatus().name())
                .createdAt(b.getCreatedAt())
                .build());
    }

    @Operation(summary = "Delete a batch and all its candidates / message logs")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!batchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Batch not found: " + id);
        }
        // Cascade-delete child rows manually (no JPA cascade configured)
        candidateRepository.findByBatchId(id).forEach(c -> candidateRepository.deleteById(c.getId()));
        batchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Lightweight DTO so we never leak the @Lob {@code schemaJson} into list responses. */
    @lombok.Getter @lombok.Setter @lombok.Builder
    @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class BatchSummary {
        private UUID id;
        private String fileName;
        private long totalCandidates;
        private String status;
        private LocalDateTime createdAt;
    }
}

