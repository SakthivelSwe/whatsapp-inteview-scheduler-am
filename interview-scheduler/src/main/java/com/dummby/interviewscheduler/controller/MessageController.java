package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.model.dto.BatchStatusResponse;
import com.dummby.interviewscheduler.model.dto.SendRequest;
import com.dummby.interviewscheduler.model.entity.MessageLog;
import com.dummby.interviewscheduler.service.BulkSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Bulk send WhatsApp messages and view status")
public class MessageController {

    private final BulkSendService bulkSendService;

    @Operation(summary = "Trigger bulk WhatsApp send for a batch")
    @PostMapping("/send-all")
    public ResponseEntity<BatchStatusResponse> sendAll(@Valid @RequestBody SendRequest req) {
        return ResponseEntity.ok(bulkSendService.triggerBulkSend(req.getBatchId(), req.getTemplateName()));
    }

    @Operation(summary = "Get delivery status of a batch")
    @GetMapping("/status/{batchId}")
    public ResponseEntity<BatchStatusResponse> status(@PathVariable UUID batchId) {
        return ResponseEntity.ok(bulkSendService.getStatus(batchId));
    }

    @Operation(summary = "Retry only the FAILED candidates of a batch (RD §5)")
    @PostMapping("/retry-failed")
    public ResponseEntity<BatchStatusResponse> retryFailed(@Valid @RequestBody SendRequest req) {
        return ResponseEntity.ok(bulkSendService.retryFailed(req.getBatchId(), req.getTemplateName()));
    }

    @Operation(summary = "Pause a SENDING batch — stops after the current message")
    @PostMapping("/pause/{batchId}")
    public ResponseEntity<BatchStatusResponse> pause(@PathVariable UUID batchId) {
        return ResponseEntity.ok(bulkSendService.pauseBatch(batchId));
    }

    @Operation(summary = "Resume a PAUSED batch — continues with remaining PENDING candidates")
    @PostMapping("/resume/{batchId}")
    public ResponseEntity<BatchStatusResponse> resume(@PathVariable UUID batchId,
                                                      @RequestParam(required = false) String templateName) {
        return ResponseEntity.ok(bulkSendService.resumeBatch(batchId, templateName));
    }

    @Operation(summary = "Get detailed message logs for a batch")
    @GetMapping("/logs/{batchId}")
    public ResponseEntity<List<MessageLog>> logs(@PathVariable UUID batchId) {
        return ResponseEntity.ok(bulkSendService.getLogs(batchId));
    }
}
