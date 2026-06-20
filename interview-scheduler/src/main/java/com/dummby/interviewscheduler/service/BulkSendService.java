package com.dummby.interviewscheduler.service;

import com.dummby.interviewscheduler.exception.ResourceNotFoundException;
import com.dummby.interviewscheduler.model.dto.BatchStatusResponse;
import com.dummby.interviewscheduler.model.entity.Batch;
import com.dummby.interviewscheduler.model.entity.Candidate;
import com.dummby.interviewscheduler.model.entity.MessageLog;
import com.dummby.interviewscheduler.model.entity.MessageTemplate;
import com.dummby.interviewscheduler.repository.BatchRepository;
import com.dummby.interviewscheduler.repository.CandidateRepository;
import com.dummby.interviewscheduler.repository.MessageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates bulk WhatsApp sending. Validates input, transitions batch
 * status, then hands the heavy loop off to {@link BulkSendDispatcher} — a
 * SEPARATE Spring bean. This is required for {@code @Async} to actually fire:
 * a {@code this.sendAllAsync(...)} call inside the same bean would bypass the
 * AOP proxy and run the entire bulk send synchronously on the HTTP thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkSendService {

    private final CandidateRepository candidateRepository;
    private final BatchRepository batchRepository;
    private final MessageLogRepository messageLogRepository;
    private final TemplateService templateService;
    private final BulkSendDispatcher dispatcher;

    /** Pause a running batch. The async loop checks this flag between each message. */
    public BatchStatusResponse pauseBatch(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        if (batch.getStatus() != Batch.BatchStatus.SENDING) {
            throw new IllegalStateException("Cannot pause batch in status " + batch.getStatus());
        }
        dispatcher.requestPause(batchId);
        log.info("Pause requested for batch {}", batchId);
        return getStatus(batchId);
    }

    /** Resume a paused batch — re-runs the bulk send for remaining PENDING candidates. */
    public BatchStatusResponse resumeBatch(UUID batchId, String templateName) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        if (batch.getStatus() != Batch.BatchStatus.PAUSED) {
            throw new IllegalStateException("Cannot resume batch in status " + batch.getStatus());
        }
        dispatcher.clearPause(batchId);
        log.info("Resuming batch {}", batchId);
        return triggerBulkSend(batchId, templateName);
    }

    /** Kicks off async bulk send for all PENDING rows of a batch. */
    public BatchStatusResponse triggerBulkSend(UUID batchId, String templateName) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        List<Candidate> sendable = candidateRepository
                .findByBatchIdAndStatus(batchId, Candidate.SendStatus.PENDING);
        if (sendable.isEmpty())
            throw new IllegalArgumentException("No sendable (PENDING) candidates in batch " + batchId);

        MessageTemplate template = (templateName == null || templateName.isBlank())
                ? templateService.findDefault()
                : templateService.findByName(templateName);

        batch.setStatus(Batch.BatchStatus.SENDING);
        batchRepository.save(batch);

        // Cross-bean call → AOP proxy fires → method runs on whatsappExecutor.
        dispatcher.sendAllAsync(batch, sendable, template);
        return getStatus(batchId);
    }

    /** Resends only previously FAILED candidates (RD §5: "Retry Failed Only"). */
    public BatchStatusResponse retryFailed(UUID batchId, String templateName) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        List<Candidate> failed = candidateRepository
                .findByBatchIdAndStatus(batchId, Candidate.SendStatus.FAILED);
        if (failed.isEmpty())
            throw new IllegalArgumentException("No FAILED candidates to retry in batch " + batchId);

        failed.forEach(c -> { c.setStatus(Candidate.SendStatus.PENDING); c.setLastError(null); });
        candidateRepository.saveAll(failed);

        MessageTemplate template = (templateName == null || templateName.isBlank())
                ? templateService.findDefault()
                : templateService.findByName(templateName);

        batch.setStatus(Batch.BatchStatus.SENDING);
        batchRepository.save(batch);

        dispatcher.sendAllAsync(batch, failed, template);
        return getStatus(batchId);
    }

    @Transactional(readOnly = true)
    public BatchStatusResponse getStatus(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        long total   = batch.getTotalCandidates() == null ? 0 : batch.getTotalCandidates();
        long sent    = candidateRepository.countByBatchIdAndStatus(batchId, Candidate.SendStatus.SENT);
        long failed  = candidateRepository.countByBatchIdAndStatus(batchId, Candidate.SendStatus.FAILED);
        long invalid = candidateRepository.countByBatchIdAndStatus(batchId, Candidate.SendStatus.INVALID);
        long pending = candidateRepository.countByBatchIdAndStatus(batchId, Candidate.SendStatus.PENDING);

        List<BatchStatusResponse.FailedRecord> failedRecords = candidateRepository
                .findByBatchIdAndStatus(batchId, Candidate.SendStatus.FAILED).stream()
                .map(c -> BatchStatusResponse.FailedRecord.builder()
                        .candidateId(c.getId())
                        .rowNumber(c.getRowNumber())
                        .candidateName(c.getCandidateName())
                        .phoneNumber(c.getPhoneNumber())
                        .error(c.getLastError())
                        .build())
                .collect(Collectors.toList());

        return BatchStatusResponse.builder()
                .batchId(batchId)
                .status(batch.getStatus().name())
                .totalRecords(total)
                .successfullySent(sent)
                .failed(failed)
                .invalid(invalid)
                .pending(pending)
                .failedRecords(failedRecords)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageLog> getLogs(UUID batchId) {
        return messageLogRepository.findByBatchId(batchId);
    }
}
