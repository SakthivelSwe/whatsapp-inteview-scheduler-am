package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.model.dto.TemplateRequest;
import com.dummby.interviewscheduler.model.entity.MessageTemplate;
import com.dummby.interviewscheduler.service.TemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Manage WhatsApp message templates")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<List<MessageTemplate>> list() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @PostMapping
    public ResponseEntity<MessageTemplate> create(@RequestBody TemplateRequest req) {
        return ResponseEntity.ok(templateService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageTemplate> update(@PathVariable UUID id, @RequestBody TemplateRequest req) {
        return ResponseEntity.ok(templateService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

