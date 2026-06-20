package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.model.dto.ColumnSchema;
import com.dummby.interviewscheduler.model.dto.UploadResponse;
import com.dummby.interviewscheduler.model.entity.Candidate;
import com.dummby.interviewscheduler.service.CandidateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Upload Excel and view candidates")
public class CandidateController {

    private final CandidateService candidateService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "Upload an Excel file of candidates",
            description = "Optional `columnMapping` JSON lets the UI map custom headers, e.g. " +
                    "`{\"Mobile\":\"phoneNumber\",\"Full Name\":\"candidateName\"}`.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "columnMapping", required = false) String columnMappingJson) throws IOException {

        Map<String, String> mapping = null;
        if (columnMappingJson != null && !columnMappingJson.isBlank()) {
            mapping = objectMapper.readValue(columnMappingJson, new TypeReference<>() {});
        }
        return ResponseEntity.ok(candidateService.uploadExcel(file, mapping));
    }

    @Operation(summary = "List candidates of a batch")
    @GetMapping
    public ResponseEntity<List<Candidate>> list(@RequestParam UUID batchId) {
        return ResponseEntity.ok(candidateService.findByBatch(batchId));
    }

    @Operation(summary = "Get the auto-detected column schema (placeholders) for a batch")
    @GetMapping("/schema/{batchId}")
    public ResponseEntity<List<ColumnSchema>> schema(@PathVariable UUID batchId) {
        return ResponseEntity.ok(candidateService.getBatchSchema(batchId));
    }
}
