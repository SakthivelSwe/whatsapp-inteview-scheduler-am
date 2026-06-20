package com.dummby.interviewscheduler.repository;

import com.dummby.interviewscheduler.model.entity.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
    Optional<MessageTemplate> findByName(String name);
}

