package com.dummby.interviewscheduler.service;

import com.dummby.interviewscheduler.exception.ResourceNotFoundException;
import com.dummby.interviewscheduler.model.dto.ColumnSchema;
import com.dummby.interviewscheduler.model.dto.UploadResponse;
import com.dummby.interviewscheduler.model.entity.Batch;
import com.dummby.interviewscheduler.model.entity.Candidate;
import com.dummby.interviewscheduler.repository.BatchRepository;
import com.dummby.interviewscheduler.repository.CandidateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final ExcelParserService excelParser;
    private final CandidateRepository candidateRepository;
    private final BatchRepository batchRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public UploadResponse uploadExcel(MultipartFile file, Map<String, String> columnMapping) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty");

        ExcelParserService.ParseResult parsed = excelParser.parse(file, columnMapping);
        if (parsed.getAll().isEmpty())
            throw new IllegalArgumentException("No data rows found in the file");

        // Build the UI-facing schema (with sample values from the first valid row)
        Candidate firstRow = parsed.getAll().isEmpty() ? null : parsed.getAll().get(0);
        List<ColumnSchema> schema = buildSchema(parsed.getSchema(), firstRow);

        Batch batch = Batch.builder()
                .fileName(file.getOriginalFilename())
                .totalCandidates(parsed.getAll().size())
                .status(Batch.BatchStatus.UPLOADED)
                .schemaJson(toJson(schema))
                .build();
        batch = batchRepository.save(batch);

        final UUID batchId = batch.getId();
        parsed.getAll().forEach(c -> c.setBatchId(batchId));
        candidateRepository.saveAll(parsed.getAll());

        log.info("Saved batch {}: total={}, valid={}, invalid={}, columns={}",
                batchId, parsed.getAll().size(), parsed.getValid().size(),
                parsed.getInvalid().size(), schema.size());

        List<UploadResponse.RowError> errors = parsed.getInvalid().stream()
                .map(c -> UploadResponse.RowError.builder()
                        .rowNumber(c.getRowNumber())
                        .candidateName(c.getCandidateName())
                        .phoneNumber(c.getPhoneNumber())
                        .error(c.getValidationError())
                        .build())
                .collect(Collectors.toList());

        return UploadResponse.builder()
                .batchId(batchId)
                .fileName(batch.getFileName())
                .totalRows(parsed.getAll().size())
                .validCandidates(parsed.getValid().size())
                .invalidCandidates(parsed.getInvalid().size())
                .errors(errors)
                .schema(schema)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Candidate> findByBatch(UUID batchId) {
        if (!batchRepository.existsById(batchId))
            throw new ResourceNotFoundException("Batch not found: " + batchId);
        return candidateRepository.findByBatchId(batchId);
    }

    /** Returns the auto-detected column schema for a batch (placeholders for templates). */
    @Transactional(readOnly = true)
    public List<ColumnSchema> getBatchSchema(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        if (batch.getSchemaJson() == null || batch.getSchemaJson().isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(batch.getSchemaJson(), new TypeReference<List<ColumnSchema>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize schema for batch {}: {}", batchId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /* ---------- helpers ---------- */

    private List<ColumnSchema> buildSchema(List<ExcelParserService.DetectedColumn> detected, Candidate firstRow) {
        List<ColumnSchema> out = new ArrayList<>();
        Map<String, String> firstExtras = firstRow == null ? Collections.emptyMap() : firstRow.getExtraFields();

        for (ExcelParserService.DetectedColumn d : detected) {
            String sample;
            if (d.isPhone()) {
                sample = firstRow == null ? "" : (firstRow.getPhoneNumber() == null ? "" : firstRow.getPhoneNumber());
            } else {
                sample = firstExtras.getOrDefault(d.getSlug(), "");
            }
            out.add(ColumnSchema.builder()
                    .header(d.getHeader())
                    .slug(d.getSlug())
                    .namedPlaceholder(d.getNamedPlaceholder())
                    .positionalPlaceholder(d.getPositionalPlaceholder())
                    .sampleValue(sample)
                    .phoneColumn(d.isPhone())
                    .known(d.getMappedField() != null)
                    .build());
        }
        return out;
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return null; }
    }
}
