package com.dummby.interviewscheduler.service;

import com.dummby.interviewscheduler.config.WhatsAppProperties;
import com.dummby.interviewscheduler.model.dto.WhatsAppSendResult;
import com.dummby.interviewscheduler.model.entity.Batch;
import com.dummby.interviewscheduler.model.entity.Candidate;
import com.dummby.interviewscheduler.model.entity.MessageLog;
import com.dummby.interviewscheduler.repository.BatchRepository;
import com.dummby.interviewscheduler.repository.CandidateRepository;
import com.dummby.interviewscheduler.repository.MessageLogRepository;
import com.dummby.interviewscheduler.service.whatsapp.WhatsAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Owns the long-running bulk-send loop and the per-batch pause flags.
 *
 * <p>Lives in its OWN Spring bean so the {@code @Async("whatsappExecutor")}
 * annotation actually fires when {@link BulkSendService} invokes it. (If the
 * loop lived inside {@code BulkSendService}, the {@code this.sendAllAsync(...)}
 * self-invocation would bypass the AOP proxy and the entire batch would run
 * synchronously on the HTTP request thread — see Spring docs on @Async.)
 */
@Slf4j
@Component
public class BulkSendDispatcher {

    private final CandidateRepository candidateRepository;
    private final BatchRepository batchRepository;
    private final MessageLogRepository messageLogRepository;
    private final TemplateService templateService;
    private final WhatsAppService whatsAppService;
    private final WhatsAppProperties properties;
    private final TransactionTemplate tx;

    /** In-memory pause flags. The async loop checks this between each message. */
    private final ConcurrentHashMap<UUID, Boolean> pauseFlags = new ConcurrentHashMap<>();

    public BulkSendDispatcher(CandidateRepository candidateRepository,
                              BatchRepository batchRepository,
                              MessageLogRepository messageLogRepository,
                              TemplateService templateService,
                              WhatsAppService whatsAppService,
                              WhatsAppProperties properties,
                              TransactionTemplate tx) {
        this.candidateRepository = candidateRepository;
        this.batchRepository = batchRepository;
        this.messageLogRepository = messageLogRepository;
        this.templateService = templateService;
        this.whatsAppService = whatsAppService;
        this.properties = properties;
        this.tx = tx;
    }

    public void requestPause(UUID batchId)  { pauseFlags.put(batchId, Boolean.TRUE); }
    public void clearPause(UUID batchId)    { pauseFlags.remove(batchId); }

    @Async("whatsappExecutor")
    public void sendAllAsync(Batch batch, List<Candidate> candidates,
                             com.dummby.interviewscheduler.model.entity.MessageTemplate template) {
        log.info("Bulk send starting (async): batch={}, count={}", batch.getId(), candidates.size());
        long baseDelayMs = resolveDelayMs();
        double jitter = Math.max(0d, Math.min(1d, properties.getRateLimit().getJitter()));
        int dailyLimit = properties.getRateLimit().getDailyLimit();
        Random rnd = new Random();

        try {
            for (Candidate c : candidates) {
                try {
                    if (Boolean.TRUE.equals(pauseFlags.get(batch.getId()))) {
                        log.info("Batch {} paused by user. Remaining candidates stay PENDING.", batch.getId());
                        batch.setStatus(Batch.BatchStatus.PAUSED);
                        batchRepository.save(batch);
                        return;
                    }

                    if (dailyLimit > 0) {
                        long sentLast24h = messageLogRepository
                                .countSentSince(LocalDateTime.now().minusHours(24));
                        if (sentLast24h >= dailyLimit) {
                            log.warn("Daily safety cap reached ({} sent / limit={}). Stopping batch {}.",
                                    sentLast24h, dailyLimit, batch.getId());
                            c.setStatus(Candidate.SendStatus.FAILED);
                            c.setLastError("Daily safety cap reached (" + dailyLimit + "/24h). Try again tomorrow.");
                            candidateRepository.save(c);
                            continue;
                        }
                    }

                    String body = templateService.render(template.getBody(), c);
                    WhatsAppSendResult result;
                    try {
                        result = whatsAppService.send(c.getPhoneNumber(), body);
                    } catch (Exception ex) {
                        result = WhatsAppSendResult.fail(ex.getMessage());
                    }
                    persistResult(batch.getId(), c, body, result);

                    if (baseDelayMs > 0) {
                        long actualDelay = baseDelayMs;
                        if (jitter > 0) {
                            double factor = 1d + (rnd.nextDouble() * 2d - 1d) * jitter;
                            actualDelay = (long) (baseDelayMs * factor);
                        }
                        TimeUnit.MILLISECONDS.sleep(actualDelay);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error sending to {}: {}", c.getPhoneNumber(), e.getMessage());
                }
            }

            batch.setStatus(Batch.BatchStatus.COMPLETED);
            batchRepository.save(batch);
            log.info("Bulk send complete for batch {}", batch.getId());
        } finally {
            pauseFlags.remove(batch.getId());
        }
    }

    /**
     * Persists the per-message audit log + updates the candidate's final status.
     * Wrapped in an explicit programmatic transaction (TransactionTemplate) so
     * the two saves are atomic even when invoked from within this same bean.
     */
    private void persistResult(UUID batchId, Candidate candidate, String renderedBody, WhatsAppSendResult result) {
        tx.executeWithoutResult(s -> {
            MessageLog mlog = MessageLog.builder()
                    .batchId(batchId)
                    .candidateId(candidate.getId())
                    .whatsappNumber(candidate.getPhoneNumber())
                    .renderedMessage(renderedBody)
                    .status(result.isSuccess() ? MessageLog.Status.SENT : MessageLog.Status.FAILED)
                    .providerMessageId(result.getProviderMessageId())
                    .errorMessage(result.getErrorMessage())
                    .build();
            messageLogRepository.save(mlog);

            candidate.setStatus(result.isSuccess() ? Candidate.SendStatus.SENT : Candidate.SendStatus.FAILED);
            candidate.setLastError(result.getErrorMessage());
            candidateRepository.save(candidate);
        });
    }

    private long resolveDelayMs() {
        double delaySec = properties.getRateLimit().getDelaySeconds();
        if (delaySec > 0) return (long) (delaySec * 1000);
        int mps = properties.getRateLimit().getMessagesPerSecond();
        return mps > 0 ? Math.max(1, 1000L / mps) : 1000L;
    }
}

